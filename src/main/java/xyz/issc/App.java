package xyz.issc;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.issc.daca.*;
import xyz.issc.daca.servers.netty.SimpleNettyServer;
import xyz.issc.daca.parsable.json.CodeBookParsable;
import xyz.issc.daca.parsable.json.RoutineBookParsable;
import xyz.issc.daca.spec.CodeBook;
import xyz.issc.daca.spec.FlowSpec;
import xyz.issc.daca.spec.RoutineBook;
import xyz.issc.daca.spec.SpecValidator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Data
public class App 
{
    public static class Loader {
        public RoutineBook loadDemoRoutineBook() {
            URL url = getClass().getClassLoader().getResource("spec/routinebook_demo.json");
            Path path = null;
            try {
                assert url != null;
                path = Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return null;
            }
            try {
                String str = new String(Files.readAllBytes(path));
                return RoutineBookParsable.fromJson(str);

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        public CodeBook loadDemoCodeBook() {
            URL url = getClass().getClassLoader().getResource("spec/codebook_demo.json");
            Path path = null;
            try {
                assert url != null;
                path = Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return null;
            }

            String str = null;
            try {
                str = new String(Files.readAllBytes(path));
                return CodeBookParsable.fromJson(str);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }


    public static void main( String[] args )

    {
        Loader loader = new Loader();
        CodeBook codeBook = loader.loadDemoCodeBook();
        RoutineBook routineBook = loader.loadDemoRoutineBook();
        Logger logger = LoggerFactory.getLogger("main");

        if (SpecValidator.validate(codeBook, routineBook)) {

            AconnManager connManager = new AconnManager(65535, codeBook, routineBook);

            SimpleNettyServer socketServer = new SimpleNettyServer.Builder().maxUser(65535).port(9001).build();
            connManager.bind(socketServer);

            /*
             * bind application services here
             */
            connManager.setAconnEventDispatcher(new AconnManager.AconnEventDispatcher() {
                @Override
                public FullMessage onUplink(String addr, FullMessage msg, FlowSpec flowSpec) {
                    logger.info("uplink requires" + flowSpec);
                    if (flowSpec.getCodeName().equals("ack_register")) {
                        try {
                            String id = msg.getAttrByName("id").unpackString();
                            String pass = msg.getAttrByName("pass").unpackString();
                            String auth = new DemoService().registerService(id, pass);
                            Svo svo = Svo.pack(Svo.Type.STRING, auth);
                            Map<String, Svo>  values = new HashMap<>();
                            values.put("auth", svo);
                            values.put("id", msg.getAttrByName("id"));
                            logger.info("client register " + id + " auth:" + auth);
                            return FullMessage.compose(codeBook.getMetaCode(), codeBook.getCodeByName(flowSpec.getCodeName()), values);
                        } catch (Svo.ValueUnpackException | Svo.ValuePackException e) {
                            e.printStackTrace();
                        }

                    }
                    return null;
                }

                @Override
                public FullMessage onDownlink(String addr, FullMessage txMsg, FlowSpec flowSpec) {
                    if (flowSpec.getCodeName() == "ack_register") {
                        try {
                            String id = txMsg.getAttrByName("id").unpackString();
                            String pass = txMsg.getAttrByName("pass").unpackString();
                            String auth = new DemoService().registerService(id, pass);
                            Svo svo = Svo.pack(Svo.Type.STRING, auth);
                            Map<String, Svo>  values = new HashMap<>();
                            values.put("auth", svo);
                            logger.info("client register " + id + " auth:" + auth);
                            return FullMessage.compose(codeBook.getMetaCode(), codeBook.getCodeByName(flowSpec.getCodeName()), values);
                        } catch (Svo.ValueUnpackException | Svo.ValuePackException e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }


                @Override
                public void onFinish(String addr, Procedure procedure) {

                }

                @Override
                public void onTimeout(String addr, Procedure procedure) {

                }

                @Override
                public void onStateChanged(String addr, int state) {

                }

                @Override
                public void onCreateAconn(Aconn aconn) {
                    aconn.setQosAdapter(new QosAdapter(10){
                        @Override
                        public void damage(Procedure proc) {
                            setMetric(getMetric()-proc.getQosWeight());
                        }

                        @Override
                        public void restore(Procedure proc) {
                            setMetric(getMetric()+1);
                        }
                    });
                }
            });

            logger.info("Starting server");
            connManager.start();
            socketServer.start();
        }
        else {
            logger.error("Codebook or routinebook check failure");
        }
    }
}

