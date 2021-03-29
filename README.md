# Declarative Application Connectivity Adapter (DACA) 

DACA is a light weighted design for M2M and IoT application connectivity management. 
This framework is developed in reactive pattern by RxJava3 supports for application connectivity management with a well decoupled design from
the BI layers.

## Licensing
Apache License, ver. 2.0

## Compatibility
Java 1.8

## Quick start

The core library implements a netty based socket server for an instant deploy of DACA:
```java
public class App {
    public static void main(String[] args) {
        AppConnManager manager = new AppConnManager(maxconn, codebook, routinebook, qosAdapter);
        manager.bind(nioserver);
        ...
        manager.start();
        nioserver.start();
    }
}
```

A non-invasive, decoupled interaction with the BI layer can be realized by implmenting the AppConnDispatcher
```java
        connManager.setAppConnEventDispatcher(new AppConnManager.AppConnEventDispatcher() {
        @Override
        public FullMessage onUplink(String addr, FullMessage rxMsg, FlowSpec spec) {
            //bi service called and return the conn a message according to spec 
            return newMsg;
        }

        @Override
        public FullMessage onDownlink(String addr, FullMessage txMsg, FlowSpec spec) {
            //bi service called and return the conn a message according to spec 
            return newMsg;
        }
        @Override
        public void onFinish(String addr, Procedure proc) {
                }
        @Override
        public void onTimeout(String addr, Procedure proc) {
                }
        @Override
        public void onStateChanged(String addr, int state) {
            //register, unregister the conn from addr
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

## Samples and demos
Codbook and routinebook samples are provided in the resources/spec

## Documentation
Read the complete documentation to understand the design principles of stateful flow modeling, codebook and routinebook design on http://daca.issc.xyz (under development)
