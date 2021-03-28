package xyz.issc.daca.spec;

import lombok.Data;
import xyz.issc.daca.FullMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RoutineBook {

    Map<String, Routine> routines;
    Map<String, String> routineInitials; //<initials, routine>
    List<String> shots;

    //global creds
    HashMap<String, SvoSpec> creds;
    int filterMode = Routine.FILTER_BLOCK;
    String[] unfiltered; //effective when the mode is block
    String[] filtered; //effective when the mode is pass

    long timeout; //connection timeout

    public Routine matchRoutineInitial(FullMessage msg, int direction) {
        String key = msg.getName()+'|' + direction;
       return routines.get(routineInitials.get(key));
    }

    public boolean isShot(FullMessage msg) {
        return shots.contains(msg.getName());
    }

    public void setRoutines(Map<String, Routine> routines) {
        this.routines = routines;
        routineInitials = new HashMap<>();
        for (String name : routines.keySet()) {
            Routine r = routines.get(name);
            if (r.getFlowGroupSpecs() != null || r.getFlowGroupSpecs().length > 0) {
                //skip 0 flow routines
                String initFlowName = r.getFlowGroupSpecs()[0].getFlowSpecs()[0].getCodeName();
                int direction = r.getFlowGroupSpecs()[0].getFlowSpecs()[0].getDirection();

                routineInitials.put(initFlowName+'|'+direction, name);
            }
        }
    }
}
