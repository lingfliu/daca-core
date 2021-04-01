package xyz.issc.daca.spec;

import lombok.Data;

import java.util.HashMap;

@Data
public class Routine {

    public static final int MODE_ROUTINE = 0; //when at routine mode, the procedure will not be retained
    public static final int MODE_FILTER = 1; //when at filter mode, the procedure will be retained until specific flows flush the procedure from the conn

    public static final int FILTER_NONE = 0; //not as filter
    public static final int FILTER_BLOCK = 1; //block all other procedures and filter procedures listed in the filtered
    public static final int FILTER_PASS = 2; //pass all other procedures and filter procedures listed in the filtered

    //not used
    public static final int RETAIN_MODE_NORMAL = 0; //creds removed when the procedure is finished
    public static final int RETAIN_MODE_ALWAYS = 1; //creds is retained throughout the connection
    public static final int RETAIN_MODE_REPLACE = 2; //creds is retained after the procedure lifetime until by other procedures declared or the same type

    String name;

    FlowGroupSpec[] flowGroupSpecs; //the first flow is the opening for the procedure

    int repeat; //-1 for infinite repeat
    boolean isAutostart = false;
    int retainMode = RETAIN_MODE_NORMAL;

    //filtering mode attributes
    int filterMode = FILTER_NONE; //filter is disabled by default
    String[] filtered; //routines to be filtered

    String[] recyclables; //routine that can recycle the retained creds after finishing

    //creds are initialized by the first flow containing the attrs, flows in the routine should meet the creds requirements
    HashMap<String, SvoSpec> creds;

    int qosWeight;
}
