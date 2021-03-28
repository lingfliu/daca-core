package xyz.issc.daca.parsable.json;


import xyz.issc.daca.spec.FlowSpec;
import xyz.issc.daca.spec.SvoSpec;

import java.util.HashMap;
import java.util.Map;

public class FlowSpecParsable {
    String code_name;
    int timeout;
    int direction;

    Map<String, SvoSpecParsable> requires;

    public FlowSpec convert() {
        FlowSpec spec = new FlowSpec();
        spec.setDirection(direction);
        spec.setCodeName(code_name);
        spec.setTimeout(timeout);
        Map<String, SvoSpec> requireVals = new HashMap<>();
        if (requires != null && requires.size() > 0) {
            for (String attr_name : requires.keySet()) {
                requireVals.put(attr_name, requires.get(attr_name).convert());
            }
            spec.setRequires(requireVals);
        }
        else {
            spec.setRequires(null);
        }

        return spec;
    }
}
