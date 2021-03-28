package xyz.issc.daca.utils;

import java.util.List;

public class ArrayHelper {
    public static boolean cmp(byte[] a, byte[] b, int offset, int len) {
        int endPos = offset + len;
        if (endPos > a.length || endPos > b.length) {
            return false;
        }

        for (int m = offset; m < offset + len; m ++) {
            if (a[m] != b[m]) {
                return false;
            }
        }
        return true;
    }

    public static boolean cmp(long[] a, long[] b, int offset, int len) {
        int endPos = offset + len;
        if (endPos > a.length || endPos > b.length) {
            return false;
        }

        for (int m = offset; m < offset + len; m ++) {
            if (a[m] != b[m]) {
                return false;
            }
        }
        return true;
    }


    public static boolean cmp(double[] a, double[] b, int offset, int len) {
        int endPos = offset + len;
        if (endPos > a.length || endPos > b.length) {
            return false;
        }

        for (int m = offset; m < offset + len; m ++) {
            if (a[m] != b[m]) {
                return false;
            }
        }
        return true;
    }


    public static boolean cmp(boolean[] a, boolean[] b, int offset, int len) {
        int endPos = offset + len;
        if (endPos > a.length || endPos > b.length) {
            return false;
        }

        for (int m = offset; m < offset + len; m ++) {
            if (a[m] != b[m]) {
                return false;
            }
        }
        return true;
    }

    /**
     * TODO inversable byte array masking to bools
     * byte array to bools by mask
     * @param bytes, length of byte arrays to be masked, for instance, to get the bool of the 10th bit in
     *               01000000 11001001 (2 bytes), then the mask is 0x0060
     * @param offset
     * @param mask
     * @return
     */
    public static boolean mask(byte[] bytes, int offset, byte[] mask) {
        if (bytes.length < offset + mask.length) {
            return false;
        }

        byte x = (byte) 0;
        for (int m = 0; m < mask.length; m ++) {
            x |= (bytes[offset+m]&mask[m]);
        }

        return x > 0;
    }

    public static boolean contain(String[] list, String val) {
        boolean hasVal = false;
        for (String str : list) {
            if (str.equals(val)) {
                return true;
            }
        }
        return hasVal;
    }

    public static boolean contain(List<String> list, String val) {
        boolean hasVal = false;
        for (String str : list) {
            if (str.equals(val)) {
                return true;
            }
        }
        return hasVal;
    }
}
