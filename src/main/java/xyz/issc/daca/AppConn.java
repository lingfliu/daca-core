package xyz.issc.daca;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.issc.daca.spec.CodeBook;
import xyz.issc.daca.spec.FlowSpec;
import xyz.issc.daca.spec.Routine;
import xyz.issc.daca.spec.RoutineBook;
import xyz.issc.daca.utils.ArrayHelper;
import xyz.issc.daca.utils.ByteParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * connectivity state entity to indicate the
 */
@Data
public class AppConn {
    Logger log = LoggerFactory.getLogger("appconn");

    QosAdapter qosAdapter;

    public interface AppConnListener {
        void onFeed(Procedure proc, Flow flow, FlowSpec flowSpec);
        void onFinish(Procedure proc);
        void onTimeout(Procedure proc);
        void onStateChanged(int state);
    }

    public static final int STATE_DISCONNECTED = 1;
    public static final int STATE_CONNECTED = 2;
    int state;

    CodeBook codeBook;
    RoutineBook routineBook;
    Coder coder;

    NioChannel channel;

    AppConnListener appConnListener;

    long lastReceivedAt;

    int filterMode;
    Map<String, Svo> creds;
    List<String> filtered;

    Lock lck = new ReentrantLock();

    ConcurrentLinkedQueue<FullMessage> sendMessageQueue;

    public void send(FullMessage txMsg) {
        channel.send(coder.encode(txMsg));
    }

    Map<String, Procedure> activeProcedures;//<id, pro>
    Map<String, Procedure> filters; //<id, pro>
    Map<String, Procedure> retained; //<id, pro>

    //load autostart flows inited by downlinks (from host to client)
    public void loadAutostartFlows() {
        for (String key : routineBook.getRoutines().keySet()) {
            Routine routine = routineBook.getRoutines().get(key);
            if (routine.isAutostart()) {
                Procedure procedure = new Procedure(routine, this);
                activeProcedures.put(procedure.getId(), procedure);
                if(procedure.filterMode == Routine.MODE_FILTER) {
                    filters.put(procedure.getId(), procedure);
                }
            }
        }
    }

    public void update(Flow flow) {

        FullMessage msg = flow.getMessage();
        if (routineBook.isShot(msg)) {
            //shot messages (no routine)
            lastReceivedAt = System.currentTimeMillis();
            qosAdapter.restore(null);
        }
        else {
            //1. active procedures
            Procedure proc = matchAndForward(flow);

            if (proc != null) {
                if (proc.state == Procedure.STATE_FINISHED)  {
                    if (appConnListener != null) {
                        appConnListener.onFinish(proc);
                    }
                    if (proc.retainMode == Routine.RETAIN_MODE_REPLACE) {
                        retained.put(proc.getId(), proc);
                    }
                    else if (proc.retainMode == Routine.RETAIN_MODE_ALWAYS) {
                        //lift creds from procedure to appconn
                        for (String name : proc.creds.keySet()) {
                            creds.put(name, proc.creds.get(name));
                        }
                    }
                }
                else if (proc.state == Procedure.STATE_TIMEOUT) {
                    if (appConnListener != null) {
                        appConnListener.onTimeout(proc);
                    }
                }
                else if (proc.state == Procedure.STATE_PENDING) {
                    //skip
                }
                else if (proc.state == Procedure.STATE_FLOWING) {
                    //check timeout
                    if (proc.isTimeout()) {
                        qosAdapter.damage(proc);
                    }

                    FlowSpec flowSpec = proc.feedFlowSpec;
                    if (appConnListener != null) {
                        appConnListener.onFeed(proc, flow, flowSpec);
                    }
                }
                else {
                    //skip
                }
            }
            else {

                Routine routine = routineBook.matchRoutineInitial(msg, flow.getDirection());
                Procedure proc2 = new Procedure(routine, this);

                //3. recycle procedures of branching
                proc = recycle(flow, proc2);
                FlowSpec spec2 = proc2.forward(flow);

                if (proc != null) {
                    //previous procedures are recycled
                    addActives(proc2);
                    lastReceivedAt = System.currentTimeMillis();
                    if (appConnListener != null) {
                        appConnListener.onFeed(proc, flow, spec2);
                    }
                }
                else {
                    //4. filtering
                    flow = filter(flow);
                    if (flow != null) {
                        //5. new procedures
                        Procedure proc3 = new Procedure(routine, this);
                        FlowSpec spec3 = proc3.forward(flow);
                        addActives(proc3);
                        lastReceivedAt = System.currentTimeMillis();
                        if (spec3 == null) {
                            if (proc3.state == Procedure.STATE_FINISHED) {
                                if (appConnListener != null) {
                                    appConnListener.onFinish(proc3);
                                }
                            }
                            else if (proc3.state == Procedure.STATE_TIMEOUT) {
                                if (appConnListener != null) {
                                    appConnListener.onTimeout(proc3);
                                }
                            }
                        }
                        else {
                            if (appConnListener != null) {
                                appConnListener.onFeed(proc3, flow, spec3);
                            }
                        }
                    }
                    else {
                        log.info("flow blocked: " + flow.getMessage().getName());
                    }
                }
            }
        }
    }


    Flow composeFlow(FullMessage msg, int direction) {
        Flow flow = new Flow();
        flow.setFormedAt(System.currentTimeMillis());
        flow.setDirection(direction);
        flow.setMessage(msg);
        return flow;
    }

    /**
     * uplink (from remote to host)
     * @param bytes
     */
    public void feedUplink(byte[] bytes) {
        coder.put(bytes);
        while (true) {
            try {
                FullMessage msg = coder.decode();
                if (msg == null) {
                    break;
                }
                else {
                    Flow flow = composeFlow(msg, Flow.UPLINK); //uplink
                    update(flow);
                }
            } catch (ByteParser.ByteArrayOverflowException | Svo.ValueUnpackException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * feed conn for downlink (host to remote)
     * @param msg
     */
    public void feedDownlink(FullMessage msg) {
        Flow flow = composeFlow(msg, Flow.DOWNLINK);
        send(msg);
        update(flow);
    }


    Procedure matchAndForward(Flow flow) {
        //1. match active procedures
        for (String id : activeProcedures.keySet()) {
            Procedure proc = activeProcedures.get(id);
            if (proc.feedFlowSpec.check(flow) ) {
                proc.forward(flow);
                return proc;
            }
            else {
                FlowSpec flowSpec = proc.skip(flow);
                if (flowSpec != null) {
                    return proc;
                }
            }
        }
        return null;
    }



    Procedure recycle(Flow flow, Procedure proc) {
        for (String id : retained.keySet()) {
            Procedure retainedProc = retained.get(id);
            if (ArrayHelper.contain(retainedProc.routine.getRecyclables(), proc.routine.getName())) {
                //transfer creds from retained to new proc
                for (String name : retainedProc.creds.keySet()) {
                    proc.creds.put(name, retainedProc.creds.get(name));
                }
                return retainedProc;
            }
        }

        return null;
    }

    Flow filter(Flow flow) {
        //global filtering
        if (routineBook.getFilterMode() == Routine.FILTER_BLOCK) {
            if (ArrayHelper.contain(filtered, flow.message.name)) {
                boolean hasCreds = true;
                for (String name : creds.keySet()) {
                    if (!flow.contain(name, creds.get(name))) {
                        hasCreds = false;
                        break;
                    }
                }
                if (!hasCreds) {
                    return null;
                } else {

                    List<Procedure> matchFilters = matchFilter(filters, flow);
                    if (matchFilters == null || matchFilters.size() == 0) {
                        return null;
                    } else {
                        boolean gHasCreds = true;
                        for (Procedure proc : matchFilters) {
                            boolean hasCreds2 = true;
                            for (String name : proc.creds.keySet()) {
                                Svo ref = proc.creds.get(name);
                                if (!flow.contain(name, ref)) {
                                    hasCreds2 = false;
                                    break;
                                }
                            }
                            if (!hasCreds2) {
                                gHasCreds = false;
                                break;
                            }
                        }

                        if (!gHasCreds) {
                            return null;
                        } else {
                            return flow;
                        }
                    }

                }
            } else {
                return null;
            }
        } else if (routineBook.getFilterMode() == Routine.FILTER_PASS) {
            if (ArrayHelper.contain(filtered, flow.message.name)) {
                boolean hasCreds = true;
                for (String name : creds.keySet()) {
                    if (!flow.contain(name, creds.get(name))) {
                        hasCreds = false;
                        break;
                    }
                }
                if (!hasCreds) {
                    return null;
                }
            }

            //pass the flow to procedure filters
            List<Procedure> matchFilters = matchFilter(filters, flow);
            if (matchFilters == null || matchFilters.size() == 0) {
                return flow;
            } else {
                boolean gHasCreds = true;
                for (Procedure proc : matchFilters) {
                    boolean hasCreds = true;
                    for (String name : proc.creds.keySet()) {
                        Svo ref = proc.creds.get(name);
                        if (!flow.contain(name, ref)) {
                            hasCreds = false;
                            break;
                        }
                    }
                    if (!hasCreds) {
                        gHasCreds = false;
                        break;
                    }
                }

                if (!gHasCreds) {
                    return null;
                } else {
                    return flow;
                }
            }

        }
        else {
            return flow;
        }
    }

    public List<Procedure> matchFilter(Map<String, Procedure> filters, Flow flow) {
        if (filters == null || filters.size() == 0) {
            return null;
        }

        List<Procedure> matched = new ArrayList<>();
        for (String id : filters.keySet()) {
            Procedure proc = filters.get(id);
            Routine routine = routineBook.getRoutines().get(id);
            if (routine.getFilterMode() == Routine.FILTER_NONE) {
                continue;
            }
            String flowName = flow.message.name;

            if (ArrayHelper.contain(routine.getFiltered(), flowName)) {
                matched.add(proc);
            }
        }

        return matched;
    }

    public void addActives(Procedure procedure) {
        activeProcedures.put(procedure.id, procedure);
    }

    public void refreshProcedures() {
        for (String id : activeProcedures.keySet()) {
            Procedure pro = activeProcedures.get(id);
            if (pro.getState() == Procedure.STATE_FINISHED) {
                activeProcedures.remove(pro.id);
            }
            else if (pro.getState() == Procedure.STATE_TIMEOUT) {
                activeProcedures.remove(pro.id);
            }
            else {
                //for pending & flowing states, update timeout determining here
                if (pro.isTimeout()) {
                    pro.setState(Procedure.STATE_TIMEOUT);
                    activeProcedures.remove(pro.id);
                }
            }
        }
        if (qosAdapter.qos < 0) {
            log.info("qos low, closing conn: " + channel.getAddr());
            close();
        }
    }

    public void forwardDownlink() {
        for (String key : activeProcedures.keySet()) {
            Procedure proc = activeProcedures.get(key);
            //using state flags to prevent re-forwarding
            if (proc.state == Procedure.STATE_PENDING) {
//                log.info("state pending");
                continue;
            }
            FlowSpec flowSpec = proc.getFeedFlowSpec();
            if (flowSpec.getDirection() == Flow.DOWNLINK) {
//                log.info("state flowing to downlink");
                if (appConnListener != null) {
                    appConnListener.onFeed(proc, proc.getPrevFlow(), flowSpec);
                }
            }
        }
    }

    /**
     * when closed, the conn will not be used again
     */
    public void close() {
        channel.close();
        if (state != STATE_DISCONNECTED) {
            state = STATE_DISCONNECTED;
            if (appConnListener != null) {
                appConnListener.onStateChanged(state);
            }
        }
    }

    public AppConn(CodeBook codeBook, RoutineBook routineBook, QosAdapter qosAdapter) {
        this.codeBook = codeBook;
        this.routineBook = routineBook;
        coder = new Coder(codeBook, codeBook.suggestBuffLen());
        this.qosAdapter = qosAdapter;
        sendMessageQueue = new ConcurrentLinkedQueue<>();
        activeProcedures = new ConcurrentHashMap<>();
        filters = new ConcurrentHashMap<>();
        retained = new ConcurrentHashMap<>();
    }
}
