package xyz.issc.daca;


import xyz.issc.daca.spec.CodeBook;
import xyz.issc.daca.spec.RoutineBook;

public class AconnFactory {
    public CodeBook codeBook;
    public RoutineBook routineBook;
    public QosAdapter qosAdapter;

    public static class Builder  {
        public CodeBook codeBook;
        public RoutineBook routineBook;
        public QosAdapter qosAdapter = new QosAdapter();

        public Builder codeBook(CodeBook codeBook) {
            this.codeBook = codeBook;
            return this;
        }
        public Builder routineBook(RoutineBook routineBook) {
            this.routineBook = routineBook;
            return this;
        }
        public Builder qosAdapter(QosAdapter qosAdapter) {
            this.qosAdapter = qosAdapter;
            return this;
        }

        public AconnFactory build() {
            if (this.codeBook != null && this.routineBook != null) {
                return new AconnFactory(codeBook, routineBook, qosAdapter);
            }
            return null;
        }

    }

    private AconnFactory(CodeBook codeBook, RoutineBook routineBook, QosAdapter qosAdapter) {
        this.codeBook = codeBook;
        this.routineBook = routineBook;
        this.qosAdapter = qosAdapter;
    }

    public Aconn createAppConn() {
        QosAdapter adapter = new QosAdapter(qosAdapter.getMetricMax());
        return new Aconn(codeBook, routineBook, adapter);
    }

}
