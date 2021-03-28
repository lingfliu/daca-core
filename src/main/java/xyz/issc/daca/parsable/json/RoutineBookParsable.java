package xyz.issc.daca.parsable.json;

import com.google.gson.Gson;
import xyz.issc.daca.spec.Routine;
import xyz.issc.daca.spec.RoutineBook;
import xyz.issc.daca.spec.SvoSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutineBookParsable {

    List<RoutineParsable> routines;
    List<String> shots;

    int filter_mode;

    long timeout; //global timeout

    List<String> unfiltered;
    List<String> filtered;
    HashMap<String, SvoSpecParsable> creds;

    public static RoutineBook fromJson(String json) {
        RoutineBookParsable parsable = new Gson().fromJson(json, RoutineBookParsable.class);
        return parsable.convert();
    }

    public RoutineBook convert() {
        RoutineBook routineBook = new RoutineBook();
        Map<String, Routine> routineMap = new HashMap<>();
        for (RoutineParsable r : routines) {
            routineMap.put(r.getName(), r.convert());
        }
        routineBook.setRoutines(routineMap);
        routineBook.setShots(shots);

        routineBook.setTimeout(timeout);

        routineBook.setFilterMode(filter_mode);

        if (creds != null & creds.size() > 0) {
            HashMap<String, SvoSpec> credsImpl = new HashMap<>();
            for (String key : creds.keySet()) {
                SvoSpec spec = creds.get(key).convert();
                credsImpl.put(key, spec);
                routineBook.setCreds(credsImpl);
            }
        }
        else {
            routineBook.setCreds(null);
        }

        if (filtered != null && filtered.size() > 0 ) {
            String[] filts = new String[filtered.size()];
            for (int m = 0; m < filtered.size(); m++) {
                filts[m] = filtered.get(m);
            }
            routineBook.setFiltered(filts);
        }
        else {
            routineBook.setFiltered(null);
        }

        if (unfiltered != null && unfiltered.size() > 0) {
            String[] unfilts = new String[unfiltered.size()];
            for (int m = 0; m < unfiltered.size(); m++) {
                unfilts[m] = unfiltered.get(m);
            }
            routineBook.setUnfiltered(unfilts);
        }
        else {
            routineBook.setUnfiltered(null);
        }

        return routineBook;
    }
}
