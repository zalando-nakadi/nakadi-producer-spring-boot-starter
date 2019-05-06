# nakadi-producer-loadtest

The project contains functionality to create load for nakadi-producer (10k, 50k, 100k and 300k events) and measures 
the execution time. The events are fired against an internally started nakadi instance. Therefore it uses docker-compose
to start kafka, nakadi, postgres and zookeeper.

#### Prerequisites

[docker-compose](https://docs.docker.com/compose/) must be installed. The docker images will be pulled automatically by
executing the integration test or can be manually pulled by executing
 ```
docker-compose -f nakadi-producer-loadtest/src/test/resources/docker-compose.yaml pull
 ``` 

#### Usage
```
mvn test -Dtest=LoadTestIT -Dgpg.skip=true
```

#### Configuration

The time-tracking functionality can be configured by changing pointcuts in `ProfilerInterceptor.java`.
```
@Around("execution(* org.zalando.nakadiproducer.transmission.impl.EventTransmissionService.lockSomeEvents(..)) || " +
        "execution(* org.zalando.nakadiproducer.transmission.impl.EventTransmissionService.sendEvents(..))")
```