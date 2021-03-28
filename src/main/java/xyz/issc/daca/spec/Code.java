package xyz.issc.daca.spec;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Code {

    String type;
    String name;

    //TODO major and minor type declaration
    String typeMinor;

    /**
     * all data are aligned piecewisely, the offsets are marked when gaps or reserved bits are required
     */
    AttrSegment[] attrSegments;

    public List<String> getAttrNames() {
        List<String> names = new ArrayList<>();
        for (AttrSegment attrSeg : attrSegments) {
            names.add(attrSeg.name);
        }
        return names;
    }
}
