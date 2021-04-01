package xyz.issc.daca.utils;

import java.nio.charset.StandardCharsets;

public class ByteParser {
    public static class ByteArrayOverflowException extends Exception {
    }

    public static class ParseLengthException extends Exception {
    }

    public enum ParseType {
        UINT,
        INT,
        STRING_INT,

        FLOAT,
        STRING_FLOAT, //not recommended
        DECIMAL,

        STRING,
        BOOL,
        BOOL_ARRAY
    }

    public static long[] parseIntArray(byte[] bytes, int offset, int parseLen, boolean signed, boolean msb, int len) throws ByteArrayOverflowException, ParseLengthException {
        if (bytes == null || bytes.length < offset + parseLen*len) throw new ByteArrayOverflowException();

        long[] vals = new long[len];
        for (int m = 0; m < len; m ++) {
            vals[m] = parseInt(bytes, offset+m*parseLen, parseLen, signed, msb);
        }
        return vals;
    }

    public static long parseInt(byte[] bytes, int offset, int len, boolean signed, boolean isMsb) throws ByteArrayOverflowException, ParseLengthException {
        //param check
        if (bytes == null || bytes.length < offset + len ) throw new ByteArrayOverflowException();
        if (len >8) throw new ParseLengthException();

        long val = 0;
        long mask = 0xff;
        long num;
        if (isMsb) { //11 22 33 for 0x112233
            for (int m = offset+len-1; m > offset-1; m--) {
                int shift = offset+len-1-m;
                if (signed) {
                    num = bytes[m];
                    num <<= 8*shift;
                    num &= mask;
                    mask <<= 8;
                    num <<= 8*(8-len);
                    num >>= 8*(8-len);
                    val += num;
                }
                else {
                    num = bytes[m];
                    num <<= 8*shift;
                    num &= mask;
                    mask <<= 8;
                    val += num;
                }
            }
        }
        else { //33 22 11 for 0x112233
            for (int m = offset; m < offset+len; m++) {
                if (signed) {
                    num = bytes[m];
                    num <<= 8*(m-offset);
                    num &= mask;
                    mask <<= 8;
                    num <<= 8*(8-len);
                    num >>= 8*(8-len);
                    val += num;
                }
                else {
                    num = bytes[m];
                    num <<= 8*(m-offset);
                    num &= mask;
                    mask <<= 8;
                    val += num;
                }
            }
        }
        return val;
    }


    public static double[] parseDeciArray(byte[] bytes, int offset, int parseLenInt, int parseLenDeci, boolean msb, int len) throws ByteArrayOverflowException, ParseLengthException {
        if (bytes == null || bytes.length < offset + (parseLenInt + parseLenDeci)*len) throw new ByteArrayOverflowException();
        double[] vals = new double[len];
        for (int m = 0; m < len; m ++) {
            vals[m] = parseDeci(bytes, offset+m*(parseLenInt+parseLenDeci), parseLenInt, parseLenDeci, msb);
        }
        return vals;
    }

    /**
     * Deimal parsing, by default the int part is signed
     * @param bytes
     * @param offset
     * @param lenInt
     * @param lenDeci
     * @param msb
     * @return
     */
    public static double parseDeci(byte[] bytes, int offset, int lenInt, int lenDeci, boolean msb) throws ByteArrayOverflowException, ParseLengthException {
        if (bytes == null || bytes.length < offset + lenInt + lenDeci) throw new ByteArrayOverflowException();
        if (offset + lenInt + lenDeci > bytes.length) {
            throw new ByteArrayOverflowException();
        }

        long a = parseInt(bytes, offset, lenInt, true, msb);
        long b = parseInt(bytes, offset+lenInt, lenDeci, false, msb);
        String c = a + "." + b;
        return Double.parseDouble(c);
    }

    /**
     * @param bytes
     * @param offset
     * @param lenInt
     * @param lenDeci
     * @param val
     * @param msb
     */
    public static void deci2bytes(byte[] bytes, int offset, int lenInt, int lenDeci, double val, boolean msb) throws ByteArrayOverflowException {
        if (bytes == null || bytes.length < offset + lenInt + lenDeci) throw new ByteArrayOverflowException();
        // TODO replace with bigdecimal
    }

    public static double[] parseFloatArray(byte[] bytes, int offset, int parseLen, boolean msb, int len) throws ByteArrayOverflowException, ParseLengthException {
        if (bytes == null || bytes.length < offset + parseLen*len) throw new ByteArrayOverflowException();

        double[] vals = new double[len];
        for (int m = 0; m < len; m ++) {
            vals[m] = parseFloat(bytes, offset+m*parseLen, parseLen, msb);
        }
        return vals;
    }

    public static double parseFloat(byte[] bytes, int offset, int len, boolean msb) throws ByteArrayOverflowException, ParseLengthException {
        if (bytes == null || offset + len> bytes.length) throw new ByteArrayOverflowException();
        if (len != 4 && len != 8) throw new ParseLengthException();

        if (len == 8) {
            return Double.longBitsToDouble(parseInt(bytes, offset, 8, true, msb));
        }
        else {
            return Float.intBitsToFloat((int) parseInt(bytes, offset, 4, true, msb));
        }
    }

    public static void float2bytes(byte[] bytes, int offset, int len, boolean msb, double x) throws ByteArrayOverflowException, ParseLengthException {
        if (bytes == null || bytes.length < offset + len) throw new ByteArrayOverflowException();
        if (len == 4) {
            int v = Float.floatToIntBits((float) x);
            int2bytes(bytes, offset, len, true, msb, v);
        }
        else if (len == 8) {
            long v = Double.doubleToLongBits(x);
            int2bytes(bytes, offset, len, true, msb, v);
        }
        else {
            throw new ParseLengthException();
        }
    }


    public static void int2bytes(byte[] bytes, int offset, int len, boolean signed, boolean msb, long x) throws ByteArrayOverflowException {
        //param check
        if (bytes.length < offset + len) throw new ByteArrayOverflowException();
        if (len > 8) throw new ByteArrayOverflowException();

        int mask = 0xff;
        if (msb) { //11 22 33 <-> 0x112233
            for (int m = offset+len-1; m > offset-1; m--) {
                int shift = offset+len-1-m;
                long v = (x >> 8*shift);
                v &= mask;
                bytes[m] = (byte) v;
            }
        }
        else { //33 22 11 <-> 0x112233
            for (int m = offset; m < offset+len; m++) {
                long v = (x>>(m-offset));
                v &= mask;
                bytes[m] = (byte) v;
            }
        }
    }



    public static String parseString(byte[] bytes, int offset, int len) throws ByteArrayOverflowException {
        if (offset + len  > bytes.length) throw new ByteArrayOverflowException();

        byte[] strBytes = new byte[len];
        System.arraycopy(bytes, offset, strBytes, 0, len);
        return new String(strBytes);
    }

    public static void string2bytes(byte[] bytes, int offset, String str) throws ByteArrayOverflowException {
        if (bytes.length < offset + str.length() - 1) throw new ByteArrayOverflowException();
        byte[] bs = str.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bs, 0, bytes, offset, bs.length);
    }

    public static long[] parseStringIntArray(byte[] bytes, int offset, int parseLen, int len) throws ByteArrayOverflowException {
        long[] vals = new long[len];
        for (int m = 0; m < len; m ++) {
            vals[m] = parseStringInt(bytes, offset+m*parseLen, parseLen);
        }
        return vals;
    }

    public static long parseStringInt(byte[] bytes, int offset, int len) throws ByteArrayOverflowException {
        if (offset + len  > bytes.length) throw new ByteArrayOverflowException();
        byte[] strBytes = new byte[len];
        System.arraycopy(bytes, offset, strBytes, 0, len);
        return Long.valueOf(new String(strBytes));
    }

    @Deprecated
    public static double[] parseStringFloatArray(byte[] bytes, int offset, int parseLen, int len) throws ByteArrayOverflowException {
        double[] vals = new double[len];
        for (int m = 0; m < len; m ++) {
            vals[m] = parseStringFloat(bytes, offset+m*parseLen, parseLen);
        }
        return vals;
    }

    public static double parseStringFloat(byte[] bytes, int offset, int len) throws ByteArrayOverflowException {
        if (offset + len  > bytes.length) throw new ByteArrayOverflowException();

        byte[] strBytes = new byte[len];
        System.arraycopy(bytes, offset, strBytes, 0, len);
        return Double.valueOf(new String(strBytes));
    }


    /**
     * @param bytes
     * @param offset
     * @param mask
     * @return
     */
    public static boolean parseBool(byte[] bytes, int offset, byte mask) throws ByteArrayOverflowException{
        if (bytes == null || bytes.length < offset) throw new ByteArrayOverflowException();
        return (bytes[offset]&mask) > 0;
    }

    public static boolean[] parseBoolArray(byte[] bytes, int offset, int len, byte[][] masks) throws ByteArrayOverflowException {
        if (bytes == null || masks.length < len || bytes.length < offset + masks[0].length) throw new ByteArrayOverflowException();

        boolean[] truths = new boolean[len];
        for (int m = 0; m < len; m ++) {
            truths[m] = ArrayHelper.mask(bytes, offset, masks[m]);
        }

        return truths;
    }

    public static void bool2bytes(byte[] bytes, int offset, boolean b, byte[] mask) throws ByteArrayOverflowException {
        //param check
        if (bytes.length < offset + mask.length-1) throw new ByteArrayOverflowException();

        for (int m = 0; m < mask.length; m ++) {
            if (b) {
                bytes[offset+m] |= mask[m];
            }
        }
    }


}
