package xyz.issc.daca;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.issc.daca.spec.CodeBook;
import xyz.issc.daca.spec.FlowSpec;
import xyz.issc.daca.spec.RoutineBook;
import xyz.issc.daca.utils.CyclicThread;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppConnManager {

    private final static Logger logger = LoggerFactory.getLogger("appconnmgr");

    public interface AppConnEventDispatcher {
        FullMessage onUplink(String addr, FullMessage rxMsg, FlowSpec txFlowSpec);
        FullMessage onDownlink(String addr, FullMessage txMsg, FlowSpec flowSpec);
        void onFinish(String addr, Procedure procedure);
        void onTimeout(String addr, Procedure procedure);
        void onStateChanged(String addr, int state);
    }

    @Setter
    AppConnEventDispatcher appConnEventDispatcher;

    ConcurrentHashMap<String, AppConn> conns; //<IP, AppConn>
    AppConnFactory appConnFactory;

    NioServer server;

    int maxConn;

    //Rx attrs
    Scheduler rxScheduler = Schedulers.from(Executors.newCachedThreadPool());
    PublishSubject<Callable> rxPublisher = PublishSubject.create();

    public AppConnManager(int maxConn, CodeBook codeBook, RoutineBook routineBook, QosAdapter qosAdapter) {
        this.maxConn = maxConn;
        conns = new ConcurrentHashMap<>();
        appConnFactory = new AppConnFactory.Builder()
                .codeBook(codeBook)
                .routineBook(routineBook)
                .qosAdapter(qosAdapter)
                .build();

        rxPublisher = PublishSubject.create();
        rxPublisher.subscribeOn(rxScheduler)
                .observeOn(rxScheduler);
    }
    public void bind(NioServer server) {
        this.server = server;
        server.eventListener = new NioServer.EventListener() {
            @Override
            public void onChannelStateChanged(String addr, int state) {
//                if (state == AppConn.STATE_DISCONNECTED) {
                logger.info(addr + " changed to" + state);
//                }
            }

            @Override
            public void onChannelConnected(NioChannel channel) {
                String addr = channel.getAddr();
                if (!conns.contains(addr))  {
                    //TODO handling multiple connections (single connection per ip)
                    //create new connections
                    logger.info("channel connected at " + addr);
                    createAppConn(channel);
                }
                else {
                    //update exisiting connections
                }


            }

            @Override
            public void onChannelDisconnected(NioChannel channel) {
                //invoked when appconn.close()->niochannel close()->nioserver notification->gevent listener
                String addr = channel.getAddr();
                if (conns.contains(addr)) {
                    logger.info("channel disconnected at" + addr);
                    conns.remove(addr);
                }
            }

            @Override
            public void onReceived(String addr, byte[] bytes, int len) {
                AppConn conn = conns.get(addr);
                if (conn != null) {
                    conn.feedUplink(bytes);
                }
            }

            @Override
            public void onSent(String addr, byte[] bytes, int len) {
                logger.info("sent byte len = " + len);
            }
        };
    }

    public void refresh() {
        for (String addr : conns.keySet()) {
            AppConn conn = conns.get(addr);
            //remove finished, timeout procedures
            conn.refreshProcedures();

            //remove timeout channels
            if (System.currentTimeMillis() - conn.lastReceivedAt  >= conn.getRoutineBook().getTimeout()) {
                conn.close();
                logger.info("conn inactive, last received at " + conn.lastReceivedAt + ", closing");
                conns.remove(addr);
            }
        }
    }

    public void createAppConn(NioChannel channel) {
        final String addr = channel.getAddr();
        AppConn conn = appConnFactory.createAppConn();
        conn.setChannel(channel);
        conns.put(addr, conn);
        conn.loadAutostartFlows();
        conn.setAppConnListener(new AppConn.AppConnListener() {
            @Override
            public void onFeed(Procedure proc, Flow flow, FlowSpec flowSpec) {
                if (flowSpec.getDirection() == Flow.DOWNLINK) {
                    proc.rxFlowSubmitter.submit(() -> {
                        FullMessage msg = null;
                        if (appConnEventDispatcher != null) {
                            if (flow.getDirection() == Flow.UPLINK) {
                                msg = appConnEventDispatcher.onUplink(conn.getChannel().getAddr(), flow.getMessage(), flowSpec);
                            } else {
                                msg = appConnEventDispatcher.onDownlink(conn.getChannel().getAddr(), flow.getMessage(), flowSpec);
                            }
                        }
                        return msg;
                    }, flowSpec.getTimeout(), proc, flowSpec);
                }
                else {
                    if (appConnEventDispatcher != null) {
                        if (flow.getDirection() == Flow.UPLINK) {
                            appConnEventDispatcher.onUplink(conn.getChannel().getAddr(), flow.getMessage(), flowSpec);
                        } else {
                            appConnEventDispatcher.onDownlink(conn.getChannel().getAddr(), flow.getMessage(), flowSpec);
                        }
                    }
                }
            }


            @Override
            public void onFinish(Procedure procedure) {
                if (appConnEventDispatcher != null) {
                    appConnEventDispatcher.onFinish(conn.getChannel().getAddr(), procedure);
                }
            }

            @Override
            public void onTimeout(Procedure procedure) {
                if (appConnEventDispatcher != null) {
                    appConnEventDispatcher.onTimeout(conn.getChannel().getAddr(), procedure);
                }
            }

            @Override
            public void onStateChanged(int state) {
                if (state == AppConn.STATE_DISCONNECTED) {
                    conns.remove(addr);
                }

                if (appConnEventDispatcher != null) {
                    appConnEventDispatcher.onStateChanged(addr, state);
                }
            }
        });
    }

    ExecutorService executorService = Executors.newCachedThreadPool();

    CyclicThread mainTask = new CyclicThread(()-> {
        refresh();
        //clear inactive conns

        for (String key : conns.keySet()) {
            AppConn conn = conns.get(key);
            conn.forwardDownlink();
        }
    },
    100) ;

    public void start() {
        mainTask.start();
    }
}
