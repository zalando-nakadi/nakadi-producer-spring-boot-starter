# nakadi-producer-loadtest

The project contains functionality to create load for nakadi-producer (10k, 50k, 100k and 300k events) and measures 
the execution time. The events are fired against an internally started nakadi instance. Therefore it uses docker-compose
to start kafka, nakadi, postgres and zookeeper.

usage:
```
mvn test -Dtest=LoadTestIT -Dgpg.skip=true
```

The time-tracking functionality can be configured by changing pointcuts in `ProfilerInterceptor.java`.
```
@Around("execution(* org.zalando.nakadiproducer.transmission.impl.EventTransmissionService.lockSomeEvents(..)) || " +
        "execution(* org.zalando.nakadiproducer.transmission.impl.EventTransmissionService.sendEvents(..))")
```