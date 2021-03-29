package xyz.issc.daca.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {

    @Test
    public void isEmpty() {
        String a = "";
        assertTrue(StringUtils.isEmpty(a));
    }

    @Test
    public void hexString2bytes() {
//        String header = "11112222333344445555666677778888aaaabbbbccccaabbcc";
        String header = "aaccaacc";
        byte[] bytes = StringUtils.hexString2bytes(header);
        assertEquals(bytes[0]&0xff, 0xaa);
        assertEquals(bytes[1]&0xff, 0xcc);
        assertEquals(bytes[2]&0xff, 0xaa);
        assertEquals(bytes[3]&0xff, 0xcc);
    }

    @Test
    public void genSimpleId() {
        String id = StringUtils.genSimpleId();
        assertNotNull(id);
    }
}