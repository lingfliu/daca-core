package xyz.issc.daca;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.issc.daca.spec.FlowGroupSpec;
import xyz.issc.daca.spec.FlowSpec;
import xyz.issc.daca.spec.Routine;
import xyz.issc.daca.utils.RxFlowSubmitter;
import xyz.issc.daca.utils.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Procedure {
    public static final int STATE_IDLE = 0;
    public static final int STATE_FLOWING = 1;
    public static final int STATE_FINISHED = 2;
    public static final int STATE_TIMEOUT = 3;
    public static final int STATE_PENDING = 4; //waiting for resources from app services


    @Deprecated
    public static class ProcedureException extends Exception {
    }

    int type;
    Routine routine;

    int phase;
    int groupPhase;
    int repeat;
    int groupRepeat;
    int flowRepeat;

    Flow prevFlow; //the last flow feed into the procedure
    FlowSpec feedFlowSpec;
    int qosWeight;
    long lastFlowAt;
    int state;

    int filterMode;
    int retainMode;

    Map<String, Svo> creds;

    boolean isAutostart = false;

    String id;

    RxFlowSubmitter rxFlowSubmitter;
    Aconn parent;

    Logger log = LoggerFactory.getLogger("proc");

    public Procedure(Routine routine, Aconn conn) {
        this.routine = routine;
        phase = 0;
        groupPhase = 0;
        repeat = routine.getRepeat();
        prevFlow = null;
        feedFlowSpec = routine.getFlowGroupSpecs()[groupPhase].getFlowSpecs()[phase];
        lastFlowAt = 0;
        state = STATE_IDLE;
        id = StringUtils.genSimpleId();
        rxFlowSubmitter = new RxFlowSubmitter();
        this.parent = conn;
        this.filterMode = routine.getFilterMode();
        this.retainMode = routine.getFilterMode();
        this.creds = new ConcurrentHashMap<>();
    }

    public boolean isTimeout() {
        return System.currentTimeMillis() - lastFlowAt > feedFlowSpec.getTimeout();
    }

    public FlowSpec skip(Flow flow) {
        if (state != STATE_FLOWING) {
            return null;
        }
        int priority = routine.getFlowGroupSpecs()[groupPhase].getPriority();
        for (int m = groupPhase+1; m  < routine.getFlowGroupSpecs().length; m ++) {
            if (routine.getFlowGroupSpecs()[m].getFlowSpecs()[0].check(flow)) {
                if (routine.getFlowGroupSpecs()[m].getPriority() > priority) {
                    groupPhase = m;
                    phase = 0;
                    groupRepeat = routine.getFlowGroupSpecs()[groupPhase].getRepeat();
                    flowRepeat = routine.getFlowGroupSpecs()[groupPhase].getFlowSpecs()[phase].getRepeat();
                    return routine.getFlowGroupSpecs()[m].getFlowSpecs()[0];
                }
            }
            else {
                //update priority
                priority = routine.getFlowGroupSpecs()[m].getPriority();
            }
        }

        return null;
    }

    public FlowSpec forward(Flow flow) {
        if (state == STATE_TIMEOUT || state == STATE_FINISHED) {
            return null;
        }

        prevFlow = flow;

        if (!feedFlowSpec.check(flow)) {
            return null;
        }

        if (state == STATE_IDLE) {
            //openning
            //timeout check skipped for initial flow
            if (feedFlowSpec.check(flow)) {
                state = STATE_FLOWING;
                lastFlowAt = System.currentTimeMillis();
                groupPhase = 0;
                phase = 0;
                return forward(flow);
            }
            else {
                state = STATE_FINISHED;
                return null;
            }
        }


        if (isTimeout()) {
            //timeout
            state = STATE_TIMEOUT;
            return null;
        }

        lastFlowAt = System.currentTimeMillis();


        FlowGroupSpec currGroupSpec = routine.getFlowGroupSpecs()[groupPhase];

        if (flowRepeat < 0) {
            return feedFlowSpec;
        }
        else if (flowRepeat > 0) {
            flowRepeat --;
            return feedFlowSpec;
        }

        if (groupPhase == routine.getFlowGroupSpecs().length-1) {
            if (phase == currGroupSpec.getFlowSpecs().length - 1) {
                //check flowrepeat
                if (flowRepeat > 0) {
                    //flow repeat
                    flowRepeat --;
                    return feedFlowSpec;
                }
                else if (flowRepeat == 0) {
                    //flow finished
                    if (groupRepeat > 0) {
                        groupRepeat --;
                        groupPhase = 0;
                        phase = 0;
                        feedFlowSpec = routine.getFlowGroupSpecs()[groupPhase].getFlowSpecs()[phase];
                        return feedFlowSpec;
                    }
                    else if (groupRepeat == 0) {
                        state = STATE_FINISHED;
                        return null;
                    }
                    else {
                        phase = 0;
                        feedFlowSpec = routine.getFlowGroupSpecs()[groupPhase].getFlowSpecs()[phase];
                        return feedFlowSpec;
                    }
                }
                else {
                    //flow repeat infinitely
                    return feedFlowSpec;
                }
            }
            else {
                if (flowRepeat > 0) {
                    flowRepeat --;
                    return feedFlowSpec;
                }
                else if (flowRepeat == 0) {
                    phase ++;
                    flowRepeat = routine.getFlowGroupSpecs()[groupPhase].getFlowSpecs()[phase].getRepeat();
                    feedFlowSpec = routine.getFlowGroupSpecs()[groupPhase].getFlowSpecs()[phase];
                    return feedFlowSpec;
                }
                else {
                    return feedFlowSpec;
                }
            }
        }
        else {
            if (phase == currGroupSpec.getFlowSpecs().length - 1) {
                if (flowRepeat > 0) {
                    flowRepeat --;
                    return feedFlowSpec;
                }
                else if (flowRepeat == 0) {
                    groupPhase ++;
                    phase = 0;
                    groupRepeat = routine.getFlowGroupSpecs()[groupPhase].getRepeat();
                    feedFlowSpec = routine.getFlowGroupSpecs()[groupPhase].getFlowSpecs()[phase];
                    flowRepeat = feedFlowSpec.getRepeat();
                    return feedFlowSpec;
                }
                else {
                    return feedFlowSpec;
                }
            }
            else {
                phase ++;
                feedFlowSpec = routine.getFlowGroupSpecs()[groupPhase].getFlowSpecs()[phase];
                flowRepeat = feedFlowSpec.getRepeat();
                return feedFlowSpec;
            }
        }
    }
}
