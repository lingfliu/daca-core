package xyz.issc.daca.parsable.json;


import xyz.issc.daca.Svo;
import xyz.issc.daca.spec.SvoSpec;

public class SvoSpecParsable {
    Svo.Type type;

    //effective for int, float
    Svo max;
    Svo min;

    //spec if is fixed content
    Svo content;

    public SvoSpec convert() {
        SvoSpec spec = new SvoSpec();
        spec.setType(type);
        spec.setMax(max);
        spec.setMin(min);
        spec.setContent(content);
        if (content != null) {
            spec.setFixed(true);
        }
        else {
            spec.setFixed(false);
        }

        return spec;
    }

}
