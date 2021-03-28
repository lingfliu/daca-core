# Declarative Application Connectivity Adapter (DACA) 

DACA is a light weighted design for M2M and IoT application connectivity management. 
This framework is designed with a reactive pattern by RxJava3 supports for application connectivity management with a well decoupled design from
the BI applications.

## Quick start

The core library implements a netty based socket server for an instant deploy of DACA:
```java
public class App {
    public static void main(String[] args) {
        Codebook codebook;
        RoutineBook routinebook;
        QosAdapter qosAdapter;
        AppConnManager connManager = new AppConnManager(maxconn, codebook, routinebook, qosAdapter);
        SimpleNettyServer socketServer = new SimpleNettyServer.Builder()
                .maxUser(maxuser)
                .port(port)
                .build();
        connManager.bind(socketServer);
        ...
        connManager.start();
        socketServer.start();
    }
}
```

A decoupled connection to the BI layer is realized by implmenting the AppConnDispatcher
```java
        connManager.setAppConnEventDispatcher(new AppConnManager.AppConnEventDispatcher() {
        @Override
        public FullMessage onUplink(String addr, FullMessage msg, FlowSpec flowSpec) {
            //bi service called and return the conn a message according to flowspec 
            return newMsg;
        }

        @Override
        public FullMessage onDownlink(String addr, FullMessage txMsg, FlowSpec flowSpec) {
            //bi service called and return the conn a message according to flowspec 
            return newMsg;
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
        });
```

You may implement a customized QoS metric adapter like
```java
QosAdapter qosAdapter = new QosAdapter(max){
    @Override
    public void damage(Procedure proc) {
        //implement your own qos metric here
        setQos(metric);
        }
    @Override
    public void restore(Procedure proc) {
        //implement your own qos metric here
        setQos(metric);
        }
});
```

## Licensing
DACA is currently released under the Apache 2.0 license.