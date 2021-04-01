package xyz.issc.daca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.issc.daca.spec.*;
import xyz.issc.daca.utils.ArrayHelper;
import xyz.issc.daca.utils.ByteCircleBuffer;
import xyz.issc.daca.utils.ByteParser;
import xyz.issc.daca.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Coder {
    public final static int DEFAULT_BUFF_LEN = 65535;
    public final static int STATUS_READY = 0;
    public final static int STATUS_IDLE = 1;
    public final static int STATUS_PENDING = 2;

    public static class DecodeException extends Exception {
    }

    ByteCircleBuffer byteBuff;
    CodeBook codeBook;
    Code metaCode;

    byte[] buff;

    //decoding variables
    Message meta; //decoding meta
    Message payload; //decoding payload
    Code code; //decoding code

    boolean isDecoding; //flag of whether the received byte stream is aligned with header matched and prefix decoded
    int decodeAttrIdx;
    int decodeAttrLen;
    long decodeReceived; //time of receiving the starting of the waiting code
    long decodeTimeout;

    int payloadLen;
    int metaLen;

    AtomicInteger status = new AtomicInteger(STATUS_IDLE);
    public int getStatus() {
        return status.get();
    }
    public void setStatus(int s)  {
        status.set(s);
    }

    Logger log = LoggerFactory.getLogger("coder");

    public Coder(CodeBook codeBook, int buffLen) {
        byteBuff = new ByteCircleBuffer(buffLen);
        this.codeBook = codeBook;
        this.metaCode = codeBook.getMetaCode();
        this.metaLen = codeBook.calcMetaLen();
        this.buff = new byte[buffLen];
    }

    public void invalidateDecoding() {
        isDecoding = false;
        meta = null;
        payload = null;
        code = null;
        decodeReceived = -1;
        decodeAttrIdx = -1;
        decodeAttrLen = -1;
        status.set(STATUS_IDLE);
    }

    public void put(byte[] bytes) {
        byteBuff.push(bytes, bytes.length);
    }

    /**
     * headless decode, parsing the whole buff into one message
     * @return
     */
    public FullMessage oneshotDecode() {
        if (byteBuff.getAvailability() < metaLen) {
            setStatus(STATUS_IDLE);
            byteBuff.flush();
            return null;
        }
        meta = parseMeta();
        String type = meta.getType();

        if (StringUtils.isEmpty(type)) {
            //invalid type
            byteBuff.flush();
            invalidateDecoding();
            return null;
        }

        Code matchCode = codeBook.getCodeByType(type);
        if (matchCode != null) {
            //set decoding status
            payload = new Message();
            payload.setType(type);
            AttrSegment[] attrSegs = code.getAttrSegments();
            for (AttrSegment seg : attrSegs) {
                int attrLen = 0;
                if (seg.isVariant()) {
                    //TODO variant determining
                    attrLen = fetchMetaAttrLen(seg.getName());
                }
                else {
                    attrLen = seg.getLen();
                }
                byteBuff.pop(buff, calcSegmentLen(seg.getOffset(), seg.getParseLen(), attrLen));

                Svo attr = parseAttr(buff, seg.getOffset(), seg, attrLen);
                payload.putAttr(seg.getName(), attr);
            }
            return new FullMessage(meta, payload);
        }
        else {
            //invalid code type, skip decoding
            byteBuff.flush();
            invalidateDecoding();
            return null;
        }

   }


    public FullMessage decode() {

        //headless decoding
        if (codeBook.isHeadless()) {
            return oneshotDecode();
        }

        //decode timeout handling
        if (isDecoding && System.currentTimeMillis()-decodeReceived > decodeTimeout)  {
            invalidateDecoding();
            drop();
        }

        byte[] header = codeBook.getHeader();
        if (byteBuff.getAvailability() < header.length) {
            return null;
        }

        boolean foundHeader = false;

        if (!isDecoding) {
            //match header
            while (byteBuff.getAvailability() > codeBook.getHeader().length) {
                if (!matchHeader()) {
                    drop();
                }
                else {
                    foundHeader = true;
                    break;
                }
            }
            if (!foundHeader) {
                return null;
            }

            //when found header, determine buffer length for prefix decoding
            if (byteBuff.getAvailability() < metaLen + header.length) {
                //insufficient for meta, wait
                invalidateDecoding();
                return null;
            }

            // parsing meta without pop bytes from buff
            meta = parseMeta();
            payloadLen = fetchMetaAttrLen("payload");

            // get type
            String type = fetchType(meta);
            if (StringUtils.isEmpty(type)) {
                //invalid type, skip decoding
                invalidateDecoding();
                drop();
                return null;
            }

            Code matchCode = codeBook.getCodeByType(type);
            if (matchCode != null) {
                /*
                 * finally pop header and prefix here
                 */
                byteBuff.pop(buff, header.length+metaLen);

                //set decoding status
                payload = new Message();
                payload.setType(type);
                payload.setName(matchCode.getName());
                code = matchCode;
                isDecoding = true;
                decodeAttrIdx = 0;
           }
            else {
                //invalid code type, skip decoding
                log.info("code not found, invalidating");
                invalidateDecoding();
                drop();
                return null;
            }
        }

        /* ============================
         * Attr decode
        ==============================*/
        while (decodeAttrIdx < code.getAttrSegments().length) {
            AttrSegment attrSeg = code.getAttrSegments()[decodeAttrIdx];
            String attrName = attrSeg.getName();

            int attrLen = -1;
            int availabilityExpected = 0;
            int offset = attrSeg.getOffset();
            if (attrSeg.isVariant()) {
                //parse varAttrLen
                if (attrSeg.getVarMode() == 1) {
                    //declared at the beginning of attrSeg
                    if (byteBuff.getAvailability() < attrSeg.getVarParseLen()) {
                        return null;
                    }
                    else {
                        int varParseLen = attrSeg.getVarParseLen();
                        byteBuff.read(buff, offset + varParseLen);
                        try {
                            attrLen = (int) ByteParser.parseInt(buff, offset, varParseLen, false, attrSeg.isVarMsb());
                            availabilityExpected = attrLen*attrSeg.getParseLen() + offset + attrSeg.getVarParseLen();
                        } catch (ByteParser.ByteArrayOverflowException | ByteParser.ParseLengthException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else if (attrSeg.getVarMode() == 2) {
                    if (meta.getAttrLen().containsKey(attrName)) {
                        attrLen = meta.getAttrLen().get(attrName);
                    }
                    else if (payload.getAttrLen().containsKey(attrName)) {
                        attrLen = payload.getAttrLen().get(attrName);
                    }
                    availabilityExpected = attrLen*attrSeg.getParseLen() + offset;
                }
                else if (attrSeg.getVarMode() == 3) {
                    byte[] tail = codeBook.getTail();
                    byteBuff.read(buff, byteBuff.getAvailability());
                    boolean foundTail = false;
                    int tailEnd = 0;
                    for (int m = byteBuff.getAvailability() - tail.length; m >= 0; m--) {
                        if (matchTail(m)) {
                            foundTail = true;
                            tailEnd = m + tail.length - 1;
                            break;
                        }
                    }
                    if (!foundTail) {
                        return null;
                    }
                    attrLen = tailEnd / attrSeg.getParseLen();
                    availabilityExpected = tailEnd+tail.length;
                }

                if (byteBuff.getAvailability() < availabilityExpected)  {
                    return null;
                }
                else {
                    byteBuff.pop(buff, availabilityExpected);
                    Svo attr = parseAttr(buff, offset, attrSeg, attrLen);
                    payload.putAttr(attrName, attr);
                    decodeAttrIdx ++;
                }
            }
            else {
                attrLen = attrSeg.getLen();
                if (byteBuff.getAvailability() < calcSegmentLen(offset, attrSeg.getParseLen(), attrLen)) {
                    // waiting
                    status.set(STATUS_PENDING);
                    return null;
                }
                else {
                    byteBuff.pop(buff, offset + attrLen*attrSeg.getParseLen());
                    Svo attr = parseAttr(buff, 0, attrSeg, attrLen);
                    payload.putAttr(attrName, attr);
                    decodeAttrIdx ++;
                }
            }
        }
        log.info("msg decoded name = " + payload.name);
        return new FullMessage(meta, payload);
    }

    int fetchMetaAttrLen(String attrName) {
        return meta.getAttrLen().getOrDefault(attrName, -1);
    }

    public Message parseMeta() {

        Message msg = new Message();
        AttrSegment[] segments = metaCode.getAttrSegments();
        byte[] tmpBuff = new byte[codeBook.getHeader().length+metaLen];
        byteBuff.read(tmpBuff, codeBook.getHeader().length+metaLen);

        int idx = codeBook.getHeader().length;
        for (AttrSegment s : segments) {
            int segLen = calcSegmentLen(s.getOffset(), s.getParseLen(), s.getLen());
            System.arraycopy(tmpBuff, idx, buff, 0, segLen);
            idx += segLen;
            Svo attr = parseAttr(buff, s.getOffset(), s, s.getLen());
            if (s.isVarAttrLen()) {
                try {
                    msg.putAttrLen(s.getName(), (int) attr.unpackInteger());
                } catch (Svo.ValueUnpackException e) {
                    e.printStackTrace();
                }
            }
            msg.putAttr(s.getName(), attr);
        }

        msg.setType(fetchType(msg));
        msg.setName("meta");
        return msg;
    }

    private int calcSegmentLen(int offset, int parseLen, int len) {
        return offset + parseLen*len;
    }

    /**
     * @param buff
     * @param attrSeg
     * @param attrLen
     * @return
     */
    public Svo parseAttr(byte[] buff, int offset, AttrSegment attrSeg, int attrLen) {
        try {
            switch (attrSeg.getParseType()) {
                case UINT:
                    return Svo.pack(Svo.Type.INT_ARRAY, ByteParser.parseIntArray(buff, offset, attrSeg.getParseLen(), false, attrSeg.isMsb(), attrLen)).squeeze();
                case INT:
                    return Svo.pack(Svo.Type.INT_ARRAY, ByteParser.parseIntArray(buff, offset, attrSeg.getParseLen(), true, attrSeg.isMsb(), attrLen)).squeeze();
                case STRING_INT:
                    return Svo.pack(Svo.Type.INT_ARRAY, ByteParser.parseStringIntArray(buff, offset, attrSeg.getParseLen(), attrLen)).squeeze();
                case BOOL:
                    return Svo.pack(Svo.Type.BOOL, ByteParser.parseBool(buff, offset, attrSeg.getBoolMask()));
                case BOOL_ARRAY:
                    return Svo.pack(Svo.Type.BOOL_ARRAY, ByteParser.parseBoolArray(buff, offset, attrLen, attrSeg.getBoolMasks())).squeeze();
                case FLOAT:
                    return Svo.pack(Svo.Type.FLOAT_ARRAY, ByteParser.parseFloatArray(buff, offset, attrSeg.getParseLen(), attrSeg.isMsb(), attrLen)).squeeze();
                case DECIMAL:
                    return Svo.pack(Svo.Type.FLOAT_ARRAY, ByteParser.parseDeciArray(buff, offset, attrSeg.getDIntLen(), attrSeg.getDDeciLen(), attrSeg.isMsb(), attrLen)).squeeze();
                case STRING_FLOAT:
                    return Svo.pack(Svo.Type.FLOAT, ByteParser.parseStringFloat(buff, offset, attrSeg.getParseLen()));
                case STRING:
                    return Svo.pack(Svo.Type.STRING, ByteParser.parseString(buff, offset, attrSeg.getLen()));
                default:
                    return null;
            }
        }
        catch (Svo.ValuePackException | ByteParser.ByteArrayOverflowException | ByteParser.ParseLengthException e ) {
            //The exception will never be triggered
            e.printStackTrace();
            return null;
        }
    }

    private void drop() {
        byteBuff.pop(buff, 1);
    }

    boolean matchHeader() {
        byte[] header = codeBook.getHeader();
        byteBuff.read(buff, header.length);
        return ArrayHelper.cmp(buff, header, 0, header.length);
    }

    boolean matchTail(int offset) {
        byte[] tail = codeBook.getTail();
        return ArrayHelper.cmp(buff, tail, offset, tail.length);
    }

    private String fetchType(Message msg) {
        // get type
        String type = null;
        try {
            Svo attrType = msg.getData().get("type");
            if (attrType.getType() == Svo.Type.INT) {
                type = Long.toString(msg.getData().get("type").unpackInteger());
            }
            else if (attrType.getType() == Svo.Type.STRING) {
                type = msg.getData().get("type").unpackString();
            }
        } catch (Svo.ValueUnpackException e) {
            e.printStackTrace();
            //invalid type, skip decoding
            return null;
        }
        return type;
    }


    /**
     * @param msg
     * @return
     */
    public byte[] encode(FullMessage msg) throws ByteParser.ByteArrayOverflowException {
        byte[] tmpBuff = new byte[65535];
        int cnt = 0;
        //1. encode header
        if (!codeBook.isHeadless()) {
            byte[] header = codeBook.getHeader();
            System.arraycopy(header, 0, tmpBuff, 0, header.length);
            cnt += header.length;
        }
        //2. encode meta
        Code metaCode = codeBook.getMetaCode();
        AttrSegment[] segs = metaCode.getAttrSegments();
        Message metaMsg = msg.getMeta();
        for (AttrSegment s : segs)  {
            int len;
            if (metaMsg.getData().containsKey(s.getName())) {
                len = toBytes(tmpBuff, cnt, s, metaMsg.getData().get(s.getName()));
            }
            else {
                //setting type in meta by naming convention
                if (s.getName().equals("type")) {
                    String type = msg.getType();
                    if (s.getParseType() == ByteParser.ParseType.UINT) {
                        int typeInt = Integer.parseInt(type);
                        ByteParser.int2bytes(tmpBuff, cnt+s.getOffset(), s.getParseLen(), false, s.isMsb(), typeInt);
                        len = s.getOffset()  + s.getParseLen();
                    }
                    else if (s.getParseType() == ByteParser.ParseType.STRING){
                        System.arraycopy(tmpBuff, cnt+s.getOffset(), type.getBytes(), 0, s.getParseLen());
                        len = s.getOffset()  + s.getParseLen();
                    }
                    else {
                        //spec error
                        return null;
                    }

                }
                else {
                    len = s.getOffset() + s.getLen() * s.getParseLen();
                }
            }
            cnt += len;
        }

        Message payload = msg.getPayload();
        Code code = codeBook.getCodeByName(msg.getName());
        segs = code.getAttrSegments();
        Map<String, Integer> varLenRec = new HashMap<>();
        Map<String, Integer> varAttrLenPos = new HashMap<>();
        Map<String, AttrSegment> varAttrLenSegs = new HashMap<>();

        for (AttrSegment s : segs){
            int len;


            if (payload.getData().containsKey(s.getName())) {
                if (s.isVariant()) {
                    int mode = s.getVarMode();
                    if (mode == 1) {
                        len = toBytes(tmpBuff, cnt, s, payload.getData().get(s.getName()));
                        int varAttrParseLen = s.getVarParseLen();
                        byte[] varAttrLenBytes = new byte[varAttrParseLen];
                        ByteParser.int2bytes(varAttrLenBytes, 0, varAttrParseLen, false, s.isVarMsb(), len);
                        System.arraycopy(tmpBuff, cnt, tmpBuff, cnt+varAttrParseLen, len);
                        System.arraycopy(tmpBuff, cnt, varAttrLenBytes, 0, varAttrParseLen);
                    }
                    else if (mode == 2) {
                        len = toBytes(tmpBuff, cnt, s, payload.getData().get(s.getName()));
                        varLenRec.put(s.getName(), len);
                        varAttrLenSegs.put(s.getName(), s);
                    }
                    else if (mode == 3) {
                        len = toBytes(tmpBuff, cnt, s, payload.getData().get(s.getName()));
                        byte[] tail = codeBook.getTail();
                        System.arraycopy(tmpBuff, cnt+len, tail, 0, tail.length);
                        break;
                    }
                    else {
                        //code spec error
                        return null;
                    }
                }
                else {
                    if (s.isVarAttrLen()) {
                        len = s.getOffset();
                        varAttrLenPos.put(s.getName(), cnt);
                    }
                    else {
                        len = toBytes(tmpBuff, cnt+s.getOffset(), s, payload.getData().get(s.getName()));
                    }
                }
            }
            else {
                //empty attribute
                if (s.isVarAttrLen()) {
                    len = s.getOffset();
                }
                else {
                    len = s.getOffset() + s.getLen()*s.getParseLen();
                }
            }
            cnt += len;

        }

        //fill missed varAttrLen
        for (String name : varAttrLenPos.keySet()) {
            int pos = varAttrLenPos.get(name);
            int len = varLenRec.get(name);
            AttrSegment seg = varAttrLenSegs.get(name);
            int parseLen = seg.getVarParseLen();
            boolean msb = seg.isVarMsb();
            byte[] bs = new byte[parseLen];
            ByteParser.int2bytes(bs, 0, parseLen, false, msb, len);
            System.arraycopy(tmpBuff, pos, bs, 0, parseLen);
        }

        byte[] encoded = new byte[cnt];
        System.arraycopy(tmpBuff, 0, encoded, 0, cnt);
        return encoded;
    }

    public int toBytes(byte[] buff, int offset, AttrSegment segment, Svo val) {
        int idx = offset;
        try {
            switch (segment.getParseType()) {
                case UINT:
                    if (val.getType() == Svo.Type.INT_ARRAY) {
                        long[] values = val.unpackIntegerArray();
                        for (long v : values) {
                            ByteParser.int2bytes(buff, idx, segment.getParseLen(), false, segment.isMsb(), v);
                            idx += segment.getParseLen();
                        }
                    }
                    else {
                        ByteParser.int2bytes(buff, idx, segment.getParseLen(), false, segment.isMsb(), val.unpackInteger());
                    }
                    break;
                case INT:
                    if (val.getType() == Svo.Type.INT_ARRAY) {
                        long[] values = val.unpackIntegerArray();
                        for (long v : values) {
                            ByteParser.int2bytes(buff, idx, segment.getParseLen(), true, segment.isMsb(), v);
                            idx += segment.getParseLen();
                        }
                    }
                    else {
                        ByteParser.int2bytes(buff, idx, segment.getParseLen(), true, segment.isMsb(), val.unpackInteger());
                    }
                    break;
                case FLOAT:
                    if (val.getType() == Svo.Type.FLOAT_ARRAY) {
                        double[] values = val.unpackFloatArray();
                        for (double v : values) {
                            ByteParser.float2bytes(buff, idx, segment.getParseLen(), segment.isMsb(), v);
                            idx += segment.getParseLen();
                        }
                    }
                    else {
                        ByteParser.float2bytes(buff, idx, segment.getParseLen(), segment.isMsb(), val.unpackFloat());
                    }
                    break;
                case BOOL:
                    if (val.getType() == Svo.Type.BOOL_ARRAY) {
                        boolean[] values = val.unpackBoolArray();
                        for (int m = 0; m < values.length; m ++) {
                            boolean b = values[m];
                            ByteParser.bool2bytes(buff, idx, b, segment.getBoolMasks()[m]);
                        }
                        idx += segment.getBoolMasks()[0].length;
                    }
                    else {
                        boolean b = val.unpackBool();
                        ByteParser.bool2bytes(buff, idx, b, segment.getBoolMasks()[0]);
                        idx += segment.getBoolMasks()[0].length;
                    }
                    break;
                case STRING:
                    String s = val.unpackString();
                    ByteParser.string2bytes(buff, offset, s);
                    idx = s.getBytes().length;
                case STRING_FLOAT:
                    break;
                case STRING_INT:
                    {
                        long value = val.unpackInteger();
                        ByteParser.deci2bytes(buff, idx, segment.getDIntLen(), segment.getDDeciLen(), value, segment.isMsb());
                    }
                    break;
                case DECIMAL:
                    {
                        double value = val.unpackFloat();
                        ByteParser.deci2bytes(buff, idx, segment.getDIntLen(), segment.getDDeciLen(), value, segment.isMsb());
                    }
                    break;
                default:
                    break;
            }
        }
        catch (Svo.ValueUnpackException | ByteParser.ByteArrayOverflowException | ByteParser.ParseLengthException e) {
            e.printStackTrace();
        }
        return idx;
    }

}
