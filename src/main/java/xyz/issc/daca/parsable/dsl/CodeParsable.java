package xyz.issc.daca.parsable.dsl;


import xyz.issc.daca.spec.AttrSegment;
import xyz.issc.daca.spec.Code;

import java.util.List;

public class CodeParsable {
    String type;
    String name;

    List<AttrSegmentParsable> attr_segments;

    String type_minor;

    public Code convert() {
        Code code = new Code();
        code.setType(type);
        code.setName(name);
        code.setTypeMinor(type_minor);
        AttrSegment[] segments = new AttrSegment[attr_segments.size()];
        int idx = 0;
        for (AttrSegmentParsable attr : attr_segments) {
            segments[idx] = attr.convert();
            idx ++;
        }
        code.setAttrSegments(segments);

        return code;
    }
}
