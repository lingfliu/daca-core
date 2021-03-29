package xyz.issc.daca.parsable.json;

import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.issc.daca.spec.RoutineBook;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RoutineBookParsableTest {

    @Test
    public void fromJson() throws URISyntaxException, IOException {
        URL url = getClass().getClassLoader().getResource("spec/routinebook_demo.json");
        Path path = Paths.get(url.toURI());
        String str = new String(Files.readAllBytes(path));
        Gson gson = new Gson();
        RoutineBookParsable parsable = gson.fromJson(str, RoutineBookParsable.class);
        long tic = System.currentTimeMillis();
        RoutineBook routineBook = parsable.convert();
        Assert.assertEquals(routineBook.getRoutines().get("register").getFlowGroupSpecs()[0].getFlowSpecs()[0].getCodeName(), "req_reg");
        long toc = System.currentTimeMillis();
        Logger log = LoggerFactory.getLogger("test");
        log.info("took "+(toc-tic) + " ms");
    }
}