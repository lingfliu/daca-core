package xyz.issc.daca;

import lombok.Data;

@Data
public class QosAdapter {

    int qos;

    public QosAdapter() {
        this.qos = 10;
    }
    public QosAdapter(int qos){
        this.qos = qos;
    }

    public int evaluate() {
        return qos; //when < 0, link will be broken
    }

    public void damage(Procedure proc) {
        qos--;
    }

    public void restore(Procedure proc) {
        qos ++;
    }
}
