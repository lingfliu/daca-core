package xyz.issc;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class DemoServiceTest {

    @Test
    public void registerService() {
        for (int m = 0; m < 10000; m ++ ) {
            long num = new Random().nextInt();
            Assert.assertTrue(num > 0);
        }
    }
}