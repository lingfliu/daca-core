package xyz.issc.daca;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class QosAdapter {

    @Getter
    @Setter
    int metric = 10;

    @Getter
    int metricMax;

    public QosAdapter() {
    }
    public QosAdapter(int max){
        this.metricMax = max;
    }

    public boolean evaluate() {
        return metric >= 0; //when < 0, link will be broken
    }

    public void damage(Procedure proc) {
        metric--;
    }

    public void restore(Procedure proc) {
        metric = metric >= metricMax ? metricMax : metric+1;
    }
}
