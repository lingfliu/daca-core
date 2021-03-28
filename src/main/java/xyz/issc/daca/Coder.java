package xyz.issc.daca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.issc.daca.spec.*;
import xyz.issc.daca.utils.ArrayHelper;
import xyz.issc.daca.utils.ByteCircleBuffer;
import xyz.issc.daca.utils.ByteParser;
import xyz.issc.daca.utils.StringUtils;

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
    int prefixLen;

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
        this.prefixLen = codeBook.calcPrefixLen();
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
        if (byteBuff.getAvailability() < prefixLen) {
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
                    attrLen = fetchPrefixAttrLen(seg.getName());
                }
                else {
                    attrLen = seg.getLen();
                }
                byteBuff.pop(buff, calcSegmentLength(seg.getOffset(), seg.getParseLen(), attrLen));

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


    public FullMessage decode() throws ByteParser.ByteArrayOverflowException, Svo.ValueUnpackException {

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
            if (byteBuff.getAvailability() < prefixLen + header.length) {
                //insufficient for prefix, wait
                invalidateDecoding();
                return null;
            }

            // parsing prefix
            meta = parseMeta();
            payloadLen = fetchPrefixAttrLen("payload");

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
                byteBuff.pop(buff, header.length+prefixLen);

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
                        status.set(STATUS_PENDING);
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
                        status.set(STATUS_PENDING);
                        return null;
                    }
                    attrLen = tailEnd / attrSeg.getParseLen();
                    availabilityExpected = tailEnd+tail.length;
                }

                if (byteBuff.getAvailability() < availabilityExpected)  {
                    status.set(STATUS_PENDING);
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
                if (byteBuff.getAvailability() < calcSegmentLength(offset, attrSeg.getParseLen(), attrLen)) {
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

    int fetchPrefixAttrLen(String attrName) {
        if (meta.getAttrLen().containsKey(attrName)) {
            return meta.getAttrLen().get(attrName);
        }
        else {
            return -1;
        }
    }

    public Message parseMeta() {

        Message msg = new Message();
        AttrSegment[] segments = metaCode.getAttrSegments();
        byte[] tmpBuff = new byte[codeBook.getHeader().length+prefixLen];
        byteBuff.read(tmpBuff, codeBook.getHeader().length+prefixLen);

        int idx = codeBook.getHeader().length;
        for (AttrSegment s : segments) {
            int segLen = calcSegmentLength(s.getOffset(), s.getParseLen(), s.getLen());
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
            else {
                msg.putAttr(s.getName(), attr);
            }
        }

        msg.setType(fetchType(msg));
        msg.setName("prefix");
        return msg;
    }

    private int calcSegmentLength(int offset, int parseLen, int len) {
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

    private String fetchType(Message meta) {
        // get type
        String type = null;
        try {
            Svo attrType = meta.getData().get("type");
            if (attrType.getType() == Svo.Type.INT) {
                type = Long.toString(meta.getData().get("type").unpackInteger());
            }
            else if (attrType.getType() == Svo.Type.STRING) {
                type = meta.getData().get("type").unpackString();
            }
        } catch (Svo.ValueUnpackException e) {
            e.printStackTrace();
            //invalid type, skip decoding
            return null;
        }
        return type;
    }


    /**
     * TODO encoding
     * @param msg
     * @return
     */
    public byte[] encode(FullMessage msg) {
        byte[] tmpBuff = new byte[65535];
        int cnt = 0;
        //1. encode header
        if (!codeBook.isHeadless()) {
            System.arraycopy(codeBook.getHeader(), 0, tmpBuff, 0, codeBook.getHeader().length);
            cnt += codeBook.getHeader().length;
        }
        //2. encode prefix
        Code meta = codeBook.getMetaCode();
        AttrSegment[] metaSegs = meta.getAttrSegments();
        Message metaMsg = msg.getMeta();
        for (AttrSegment seg : metaSegs)  {
            int len;
            if (metaMsg.getData().containsKey(seg.getName())) {
                len = toBytes(tmpBuff, cnt, seg, metaMsg.getData().get(seg.getName()));
            }
            else {
                len = seg.getLen()*seg.getParseLen();
            }
            cnt += len;
        }

        // TODO encode payload
        Message payload = msg.getPayload();
        Code code = codeBook.getCodeByName(msg.getName());
        for (AttrSegment seg : code.getAttrSegments()){
            int len;
            if (payload.getData().containsKey(seg.getName())) {
                len = toBytes(tmpBuff, cnt, seg, payload.getData().get(seg.getName()));
            }
            else {
                len = seg.getLen()*seg.getParseLen();
            }
            cnt += len;
        }
        //3. encode payload
        byte[] encoded = new byte[cnt];
        System.arraycopy(tmpBuff, 0, encoded, 0, cnt);
        return encoded;
    }

    //TODO: tobytes
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
                    ByteParser.string2bytes(buff, idx, s);
                    break;
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
