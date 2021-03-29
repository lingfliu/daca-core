package xyz.issc.daca.parsable.json;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class AttrSegmentParsableTest {

    public enum TestEnum {
        @SerializedName("int")
        INT,
        @SerializedName("uint")
        UINT,
        @SerializedName("float")
        FLOAT,
        @SerializedName("string")
        STRING,
        DECIMAL
    }

    public static class TestObj {
        TestEnum type;
    }

    @Test
    public void convert() throws URISyntaxException, IOException {
        long toc = System.currentTimeMillis();
        URL url = getClass().getClassLoader().getResource("spec/attr_demo2.json");
        Path path = Paths.get(url.toURI());
        String str = new String(Files.readAllBytes(path));
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();

        TestObj obj = gson.fromJson(str, TestObj.class);
        long tic = System.currentTimeMillis();
        Logger log = LoggerFactory.getLogger("test");
        log.info("took "+(toc-tic) + " ms");
        TestObj obj2 = new TestObj();
        obj.type = TestEnum.UINT;
        String str2 = new Gson().toJson(obj2);
        assertEquals(str2, "int");
    }
}