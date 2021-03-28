package xyz.issc.daca.utils;

import java.util.UUID;

public class StringUtils {
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty() || str.isEmpty();
    }

    public static byte[] hexString2bytes(String hexStr) {
        int len = hexStr.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexStr.charAt(i), 16) << 4)
                    + Character.digit(hexStr.charAt(i+1), 16));
        }
        return data;

        //not recommended for short byte arrays
//        return new BigInteger(hexStr, 16).toByteArray();
    }

    public static String genSimpleId() {
        return UUID.randomUUID().toString();
    }
}
