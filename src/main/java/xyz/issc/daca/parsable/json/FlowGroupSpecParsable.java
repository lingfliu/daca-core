package xyz.issc.daca.parsable.json;



import xyz.issc.daca.spec.FlowGroupSpec;
import xyz.issc.daca.spec.FlowSpec;

import java.util.List;

public class FlowGroupSpecParsable {
    List<FlowSpecParsable> flows;
    int repeat;

    public FlowGroupSpec convert() {
        FlowGroupSpec groupSpec = new FlowGroupSpec();
        groupSpec.setRepeat(repeat);

        if (flows != null && flows.size() == 0) {
            groupSpec.setFlowSpecs(null);
        }
        else {
            FlowSpec[] flowSpecs = new FlowSpec[flows.size()];
            int cnt = 0;
            for (FlowSpecParsable spec : flows) {
                flowSpecs[cnt] = spec.convert();
                cnt ++;
            }
            groupSpec.setFlowSpecs(flowSpecs);
        }
        return groupSpec;
    }
}
