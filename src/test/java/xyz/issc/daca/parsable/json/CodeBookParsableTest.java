package xyz.issc.daca.parsable.json;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.issc.daca.spec.CodeBook;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CodeBookParsableTest {

    @Test
    public void fromJson() throws URISyntaxException, IOException {
        URL url = getClass().getClassLoader().getResource("spec/codebook_demo.json");
        Path path = Paths.get(url.toURI());
        String str = new String(Files.readAllBytes(path));
        Gson gson = new Gson();
        CodeBookParsable parsable = gson.fromJson(str, CodeBookParsable.class);
        long tic = System.currentTimeMillis();
        CodeBook codeBook = parsable.convert();
        Assert.assertEquals(codeBook.getCodeByName("pulse").getName(), "pulse");
        long toc = System.currentTimeMillis();
        Logger log = LoggerFactory.getLogger("test");
        log.info("took "+(toc-tic) + " ms");

    }
}