package xyz.issc.daca.utils;

import org.junit.Assert;
import org.junit.Test;

public class ByteParserTest {

    @Test
    public void parseInt() throws ByteParser.ByteArrayOverflowException, ByteParser.ParseLengthException {
        byte[] a = {(byte) 0xff, (byte) 0xff, (byte) 0xfe, (byte) 0xf0, (byte) 0xf1, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4};
        long b = 0xfffffffffffffffeL;
        long c = 0xfffffeL;

        long d = 0xfffffffffffeffffL;
        long e = 0xfeffffL;

        long f = 0xfffffef0f1f2f3f4L;
        long g = 0xf4f3f2f1f0feffffL;

        Assert.assertEquals(ByteParser.parseInt(a, 0, 3, true, true), b);
        Assert.assertEquals(ByteParser.parseInt(a, 0, 3, false, true), c);
        Assert.assertEquals(ByteParser.parseInt(a, 0, 3, true, false), d);
        Assert.assertEquals(ByteParser.parseInt(a, 0, 3, false, false), e);
        Assert.assertEquals(ByteParser.parseInt(a, 0, 8, true, true), f);
        Assert.assertEquals(ByteParser.parseInt(a, 0, 8, true, false), g);



    }

    @Test
    public void parseDeci() throws ByteParser.ByteArrayOverflowException, ByteParser.ParseLengthException {
        byte[] a = {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0xf1, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4};
        double g = 4660.22136;
        Assert.assertTrue(String.valueOf(ByteParser.parseDeci(a, 0, 2, 2, true)).equals(String.valueOf(g)));

    }

    @Test
    public void parseFloat() throws ByteParser.ByteArrayOverflowException, ByteParser.ParseLengthException {
        byte[] a = {(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0xf1, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4};
        double val = ByteParser.parseFloat(a,0,8,true);
        Assert.assertTrue(val > 0);
    }

    @Test
    public void parseString() {
    }

    @Test
    public void parseBoolArray() throws ByteParser.ByteArrayOverflowException {
        byte[] a = {(byte) 0x0a, (byte) 0xad};

        boolean[] truth = new boolean[6];
        byte[][] masks = {{(byte) 0x08, (byte) 0x00},
                {(byte) 0x02, (byte) 0x00},
                {(byte) 0x80, (byte) 0x00},
                {(byte) 0x00, (byte) 0x04},
                {(byte) 0x00, (byte) 0x09},
                {(byte) 0x00, (byte) 0x02},
        };
        for (int m = 0; m < truth.length; m ++) {
            truth[m] = ArrayHelper.mask(a, 0, masks[m]);
        }

        truth = ByteParser.parseBoolArray(a, 0, 6, masks);

        Assert.assertTrue(truth[3]);
        Assert.assertTrue(truth[4]);
        Assert.assertFalse(truth[5]);

    }
}