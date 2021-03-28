package xyz.issc.daca.spec;

import lombok.Data;
import xyz.issc.daca.utils.ByteParser;

/**
 * Byte segment of attributes in payload and prefix
 * The attr segment should be streamed in order as name (optional) : varLen (optional) : value
 */
@Data
public class AttrSegment {
    String name;

    //parsing param.
    int offset = 0;
    ByteParser.ParseType parseType = ByteParser.ParseType.UINT;
    int parseLen;
    int len = 1;
    boolean isMsb = true; //default true

    //for variable length parsing
    boolean isVarAttrLen = false;

    boolean isVariant = false;
    int varMode; //1: declared in attr meta, 2: freely declared meta or payload attr, 3: determined when tail reached
    int varParseLen;
    boolean isVarMsb;



    byte boolMask;
    //for bool array
    byte[][] boolMasks;

    //for decimal transmission
    int dIntLen;
    int dDeciLen;

    //for fixed content
    byte[] content = null;

}
