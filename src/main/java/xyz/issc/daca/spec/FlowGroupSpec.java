package xyz.issc.daca.spec;

import lombok.Data;

@Data
public class FlowGroupSpec {
    int repeat;
    int priority = 0;
    FlowSpec[] flowSpecs;
}
