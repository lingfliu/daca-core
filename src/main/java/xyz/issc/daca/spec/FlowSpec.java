package xyz.issc.daca.spec;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.issc.daca.Flow;
import xyz.issc.daca.FullMessage;
import xyz.issc.daca.Svo;

import java.util.Map;

@Data
public class FlowSpec {
    Logger log = LoggerFactory.getLogger("flowspec");
    String codeName;
    int timeout; //timeout to feed flow into procedure
    int direction;
    int repeat;
    int priority;

    Map<String, SvoSpec> requires;

    public boolean check(Flow flow) {
        FullMessage msg  = flow.getMessage();
//        log.info(codeName);
//        log.info(String.valueOf(flow));
        if (!codeName.equals(flow.getMessage().getName())) {
            return false;
        }

        if (direction != flow.getDirection()) {
            return false;
        }

        if (requires==null || requires.size() == 0) {
            return true;
        }

        for (String name : requires.keySet()) {
            SvoSpec spec = requires.get(name);
            Svo attr = msg.getAttrByName(name);
            if (attr == null) {
                //require values not included
                return false;
            }

            if(!spec.check(attr)) {
                return false;
            }
        }
        return true;
    }
}
