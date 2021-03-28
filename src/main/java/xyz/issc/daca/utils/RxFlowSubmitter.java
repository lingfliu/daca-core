package xyz.issc.daca.utils;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import xyz.issc.daca.Flow;
import xyz.issc.daca.FullMessage;
import xyz.issc.daca.Procedure;
import xyz.issc.daca.spec.FlowSpec;

import java.util.concurrent.*;


public class RxFlowSubmitter {
    public class MessageCompound {
        FullMessage msg;
        int res;
        Procedure proc;
        FlowSpec flowSpec;
    }
    PublishSubject<RxFlowBundle> publish;
    private final static ExecutorService executorService = Executors.newCachedThreadPool();
    private final static Scheduler rxScheduler = Schedulers.from(executorService);


    public RxFlowSubmitter() {
        publish = PublishSubject.create();
        publish.subscribeOn(rxScheduler)
                .observeOn(rxScheduler)
                .map(bundle -> {
                    bundle.procedure.setState(Procedure.STATE_PENDING);
                    Future<FullMessage> future = executorService.submit(bundle.callable);
                    try {
                        FullMessage msg = future.get(bundle.timeout, TimeUnit.MILLISECONDS);
                        MessageCompound msgCompound = new MessageCompound();
                        msgCompound.msg = msg;
                        msgCompound.res = 0;
                        msgCompound.proc = bundle.procedure;
                        msgCompound.flowSpec = bundle.flowSpec;
                        return msgCompound;
                    }
                    catch (TimeoutException e) {
                        MessageCompound msgCompound = new MessageCompound();
                        msgCompound.msg = null;
                        msgCompound.res = -2;
                        return msgCompound;
                    }
                })
        .subscribe(messageCompound -> {
            if (messageCompound.res == 0) {
                if (messageCompound.msg != null) {
                    messageCompound.proc.setState(Procedure.STATE_FLOWING);
                    Flow flow = new Flow();
                    flow.setDirection(messageCompound.flowSpec.getDirection());
                    flow.setMessage(messageCompound.msg);
                    messageCompound.proc.getParent().feedDownlink(messageCompound.msg);
                    messageCompound.proc.forward(flow);
                }
            }
            else if (messageCompound.res < 0) {
                messageCompound.proc.setState(Procedure.STATE_TIMEOUT);
            }
        });
    }


    static public class RxFlowBundle {
        public Callable<FullMessage> callable;
        public long timeout;
        public Procedure procedure;
        public FlowSpec flowSpec;
    }
    public void submit(Callable<FullMessage> c, long timeout, Procedure proc, FlowSpec flowSpec) {
        RxFlowBundle bundle = new RxFlowBundle();
        bundle.callable = c;
        bundle.timeout = timeout;
        bundle.procedure = proc;
        bundle.flowSpec = flowSpec;
        publish.onNext(bundle);
    }
}
