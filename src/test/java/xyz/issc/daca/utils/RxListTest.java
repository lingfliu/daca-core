package xyz.issc.daca.utils;

import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.junit.Test;

public class RxListTest {

    @Test
    public void submit() throws InterruptedException {
        PublishSubject<Runnable> publish = PublishSubject.create();

        publish.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io());

        publish.subscribe(Runnable::run).dispose();

        publish.onNext(()->{
            int a = 0;
            System.out.println(a);
        });
        Thread.sleep(2000);
        publish.onNext(()->{
            int a = 1;
            System.out.println(a);
        });
        Thread.sleep(2000);
        publish.onNext(()->{
            int a = 1;
            System.out.println(a);
        });
    }
}