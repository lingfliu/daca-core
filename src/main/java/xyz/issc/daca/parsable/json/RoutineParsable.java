package xyz.issc.daca.parsable.json;

import lombok.Data;
import xyz.issc.daca.spec.FlowGroupSpec;
import xyz.issc.daca.spec.Routine;
import xyz.issc.daca.spec.SvoSpec;

import java.util.HashMap;
import java.util.List;

@Data
public class RoutineParsable {

    String name;

    int mode; //0 for routine only, 1 for filter

    List<FlowGroupSpecParsable> flow_groups;

    int repeat = 0; //-1 for infinite repeat

    boolean autostart = false;

    int filter_mode = 0; //by default filtering is disabled
    int retain_mode = 1; //normal mode
    List<String> filtered; //routine to be handled by this filter
    HashMap<String, SvoSpec> creds;

    public Routine convert() {
        Routine routine = new Routine();
        routine.setName(name);

        if (flow_groups != null && flow_groups.size() > 0) {
            FlowGroupSpec[] groupSpecs = new FlowGroupSpec[flow_groups.size()];
            for (int m = 0; m < flow_groups.size(); m++) {
                groupSpecs[m] = flow_groups.get(m).convert();
            }
            routine.setFlowGroupSpecs(groupSpecs);
        }
        else {
            routine.setFlowGroupSpecs(null);
        }

        routine.setRepeat(repeat);

        routine.setFilterMode(filter_mode);

        if (filtered != null && filtered.size() > 0 ) {
            String[] filts = new String[filtered.size()];
            for (int m = 0; m < filtered.size(); m++) {
                filts[m] = filtered.get(m);
            }
            routine.setFiltered(filts);
        }
        else {
            routine.setFiltered(null);
        }


        routine.setRetainMode(retain_mode);

        if (creds != null && creds.size() > 0) {
            routine.setCreds(creds);
        }
        else {
            routine.setCreds(null);
        }

        return routine;
    }
}
