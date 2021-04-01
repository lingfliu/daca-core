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

public class AconnManager {

    private final static Logger logger = LoggerFactory.getLogger("appconnmgr");

    public interface AconnEventDispatcher {
        FullMessage onUplink(String addr, FullMessage rxMsg, FlowSpec txFlowSpec);
        FullMessage onDownlink(String addr, FullMessage txMsg, FlowSpec flowSpec);
        void onFinish(String addr, Procedure procedure);
        void onTimeout(String addr, Procedure procedure);
        void onStateChanged(String addr, int state);
        void onCreateAconn(Aconn aconn);
    }

    @Setter
    AconnEventDispatcher aconnEventDispatcher;

    ConcurrentHashMap<String, Aconn> conns; //<IP, AppConn>
    AconnFactory aconnFactory;

    NioServer server;

    int maxConn;

    //Rx attrs
    Scheduler rxScheduler = Schedulers.from(Executors.newCachedThreadPool());
    PublishSubject<Callable> rxPublisher;

    public AconnManager(int maxConn, CodeBook codeBook, RoutineBook routineBook) {
        this.maxConn = maxConn;
        conns = new ConcurrentHashMap<>();
        aconnFactory = new AconnFactory.Builder()
                .codeBook(codeBook)
                .routineBook(routineBook)
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
                logger.info(addr + " changed to" + state);
            }

            @Override
            public void onChannelConnected(NioChannel channel) {
                String addr = channel.getAddr();
                if (!conns.contains(addr))  {
                    //create new connections
                    logger.info("channel connected at " + addr);
                    createAppConn(channel);
                }
                else {
                    //update exisiting connections
                    logger.info("duplicate connection, aborting");
                    channel.close();
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
                Aconn conn = conns.get(addr);
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
            Aconn conn = conns.get(addr);
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
        Aconn conn = aconnFactory.createAppConn();
        conn.setChannel(channel);
        conns.put(addr, conn);
        conn.loadAutostartFlows();
        conn.setAppConnListener(new Aconn.AppConnListener() {
            @Override
            public void onFeed(Procedure proc, Flow flow, FlowSpec flowSpec) {
                if (flowSpec.getDirection() == Flow.DOWNLINK) {
                    proc.rxFlowSubmitter.submit(() -> {
                        FullMessage msg = null;
                        if (aconnEventDispatcher != null) {
                            if (flow.getDirection() == Flow.UPLINK) {
                                msg = aconnEventDispatcher.onUplink(conn.getChannel().getAddr(), flow.getMessage(), flowSpec);
                            } else {
                                msg = aconnEventDispatcher.onDownlink(conn.getChannel().getAddr(), flow.getMessage(), flowSpec);
                            }
                        }
                        return msg;
                    }, flowSpec.getTimeout(), proc, flowSpec);
                }
                else {
                    if (aconnEventDispatcher != null) {
                        if (flow.getDirection() == Flow.UPLINK) {
                            aconnEventDispatcher.onUplink(conn.getChannel().getAddr(), flow.getMessage(), flowSpec);
                        } else {
                            aconnEventDispatcher.onDownlink(conn.getChannel().getAddr(), flow.getMessage(), flowSpec);
                        }
                    }
                }
            }


            @Override
            public void onFinish(Procedure procedure) {
                if (aconnEventDispatcher != null) {
                    aconnEventDispatcher.onFinish(conn.getChannel().getAddr(), procedure);
                }
            }

            @Override
            public void onTimeout(Procedure procedure) {
                if (aconnEventDispatcher != null) {
                    aconnEventDispatcher.onTimeout(conn.getChannel().getAddr(), procedure);
                }
            }

            @Override
            public void onStateChanged(int state) {
                if (state == Aconn.STATE_DISCONNECTED) {
                    conns.remove(addr);
                }

                if (aconnEventDispatcher != null) {
                    aconnEventDispatcher.onStateChanged(addr, state);
                }
            }
        });

        if (aconnEventDispatcher != null) {
            aconnEventDispatcher.onCreateAconn(conn);
        }
    }

    CyclicThread mainTask = new CyclicThread(()-> {
        refresh();
        //clear inactive conns

        for (String key : conns.keySet()) {
            Aconn conn = conns.get(key);
            conn.forwardDownlink();
        }
    },
    100) ;

    public void start() {
        mainTask.start();
    }

    public void stop() {
        mainTask.quit();
    }
}
