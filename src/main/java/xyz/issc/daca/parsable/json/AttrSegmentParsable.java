package xyz.issc.daca.parsable.json;


import xyz.issc.daca.spec.AttrSegment;
import xyz.issc.daca.utils.ByteParser;
import xyz.issc.daca.utils.StringUtils;

public class AttrSegmentParsable {

    String name;

    //parsing param.
    int offset = 0;

    ParseTypeParsable parse_type = ParseTypeParsable.UINT;

    int parse_len = 1;
    int len = 1;

    boolean msb = true; //default true

    boolean is_variant_len;

    //for variable length parsing
    boolean is_variant = false;
    int variant_mode = 1; //1: declared in attr meta, 2: declared in prefix, 3: determined when tail reached
    int variant_parse_len = 0;
    boolean variant_msb = false;

    byte bool_mask = (byte) 0xff;

    //for decimal transmission
    int deci_int_len = 4;
    int deci_deci_len = 4;

    //for fixed content
    String content = null;


    public AttrSegment convert() {
        AttrSegment seg = new AttrSegment();
        seg.setName(name);
        seg.setOffset(offset);
        seg.setParseType(parseTypeParsableConvert(parse_type));
        seg.setParseLen(parse_len);
        seg.setLen(len);
        seg.setMsb(msb);
        seg.setVariant(is_variant);
        seg.setVarMode(variant_mode);
        seg.setVarParseLen(variant_parse_len);
        seg.setVarMsb(variant_msb);
        seg.setBoolMask(bool_mask);
        seg.setDIntLen(deci_int_len);
        seg.setDDeciLen(deci_deci_len);
        if (StringUtils.isEmpty(content))  {
            seg.setContent(null);
        }
        else {
            seg.setContent(StringUtils.hexString2bytes(content));
        }
        return seg;
    }

    ByteParser.ParseType parseTypeParsableConvert(ParseTypeParsable parsable)  {
        switch (parsable) {
            case UINT:
                return ByteParser.ParseType.UINT;
            case INT:
                return ByteParser.ParseType.INT;
            case STRING_INT:
                return ByteParser.ParseType.STRING_INT;
            case FLOAT:
                return ByteParser.ParseType.FLOAT;
            case DECIMAL:
                return ByteParser.ParseType.DECIMAL;
            case STRING_FLOAT:
                return ByteParser.ParseType.STRING_FLOAT;
            case STRING:
                return ByteParser.ParseType.STRING;
            case BOOL:
                return ByteParser.ParseType.BOOL;
            case BOOL_ARRAY:
                return ByteParser.ParseType.BOOL_ARRAY;
            default:
                return ByteParser.ParseType.UINT;
        }
    }
}
