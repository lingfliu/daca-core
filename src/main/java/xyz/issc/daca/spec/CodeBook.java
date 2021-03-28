package xyz.issc.daca.spec;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class CodeBook {

    byte[] header;
    boolean isHeadless;

    byte[] tail;

    //TODO: CRC implementation
    boolean hasCrc;
    boolean isCrcOptional;

    //global properties
    String gStrEncode = "ASCII"; //not used
    boolean gIsMsb = true; //TODO: inject global properties into each code

    Code metaCode;

    Map<String, Code> codes = new HashMap<>(); //<name, code>

    public Code getCodeByType(String type) {
        return codes.get(type);
    }

    public Code getCodeByType(int type) {
        return codes.get(String.valueOf(type));
    }

    public Code getCodeByName(String name) {
        for (String k : codes.keySet()) {
            if (codes.get(k).name.equals(name)) {
                return codes.get(k);
            }
        }
        return null;
    }

    public List<String> getTypeList() {
        return new ArrayList<>(codes.keySet());
    }

    public List<String> getNameList() {
        List<String> list = new ArrayList<>();
        for (String k : codes.keySet()) {
            list.add(codes.get(k).name);
        }

        return list;
    }

    public int calcPrefixLen() {
        int len = 0;
        AttrSegment[] attrSegments = metaCode.getAttrSegments();
        for (int m = 0; m < attrSegments.length; m ++) {
            len += attrSegments[m].getParseLen()*attrSegments[m].getLen();
        }
        return len;
    }

    /**
     * effective when is headless or invariant
     * @param code
     * @return
     */
    public int calcPayloadLen(Code code) {
        int len = 0;
        AttrSegment[] attrSegs = code.getAttrSegments();
        for (AttrSegment attrSeg : attrSegs) {
            len += attrSeg.getParseLen() * attrSeg.getLen();
        }
        return len;
    }

    public int suggestBuffLen() {
        return calcPrefixLen()*100;
    }

}
