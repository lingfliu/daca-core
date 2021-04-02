# DACA 

DACA, short for declaractive application connectivity adapter, is a light weighted declaractive framework for standard and proprietary application protocol connecting. 
This framework is developed in a non-invasive and reactive pattern, providing a full life cycle, seperated management of the connections.

## Licensing
Apache License, ver. 2.0

## Compatibility
Java 1.8

## Quick start

A basic deploying of DACA include 4 components, the Codebook, Routinebook, AconnManager, and NioServer:
```java
Codebook codebook;
Routinebook routinebook;
NioServer server;
AconnManager manager = new AconnManager(maxconn, codebook, routinebook);
manager.bind(server);
manager.start();
nioserver.start();
```
where AconnManager controls the life cycles of all application connections (Aconns).

Interaction between the Aconn and the application layer can be done by implementing the message, state, event listeners:
```java
connManager.setMessageListener(new MessageListener() {
    @Override
    public FullMessage onUplink(String addr, FullMessage rxMsg, FlowSpec spec) {
        //app service called and return the aconn a message according to spec 
        return newMsg;
    }

    @Override
    public FullMessage onDownlink(String addr, FullMessage txMsg, FlowSpec spec) {
        //app service called and return the aconn a message according to spec 
        return newMsg;
    }
};
```

```java
connManager.setEventListener(new EventListener() {
    @Override
    public void onTimeout(String addr) {
    }
    @Override
    public void onFinish(String addr, Procedure proc) {
    }
    @Override
    public void onStateChanged(String addr, Procedure proc) {
    }
};
```

```java
connManager.setEventListener(new StateListener() {
    @Override
    public void onStateChanged(String addr, int state) {
    }
};
```

For the concepts of Procedure, Routine, and other concepts, please read  http://daca.issc.xyz/docs


## Samples and demos
Codbook and routinebook samples are provided in the resources/spec

## Documentation
Read the complete documentation to understand the design principles of stateful flow modeling, codebook and routinebook design on http://daca.issc.xyz (under development)
