# tarbela-producer-spring-boot-starter
Tarbela event producer API implementation as a Spring boot starter 

:rocket:

Tarbela is a reliable generic event publisher [according to documentation](https://libraries.io/github/zalando-incubator/tarbela)

The goal of this Spring Boot starter is to simplify the integration between event producer and Tarbela publisher.

The important thing is that the new events are stored in the same database (and using the same JDBC connections and transactions) as the actual data we want to store (thereby making the distributed-transaction problem go away).


## Installation

Build and install the library into your local Maven repository:

    ./mvnw clean install

Add the following dependency into the pom.xml of your Spring-Boot application

```xml
<dependency>
    <groupId>de.zalando.wholesale.tarbelaevents</groupId>
    <artifactId>tarbela-producer-spring-boot-starter</artifactId>
    <version>${tarbela-events.version}</version>
</dependency>
```

### Prerequisites

* Spring Boot 1.4.1

## Configuration

Use `@EnableTarbelaEvents` annotation to activate spring boot starter auto configuration

```
@SpringBootApplication
@EnableTarbelaEvents
public class Application {
    public static void main(final String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
```

This will configure: 

* the database table for events 
* EventLogService service for writing events into the table 
* controller listening `/events` endpoint that will publish the events for Tarbela

Configure event type, event data type and sinkId in application properties:

    tarbela:
      event-type: wholesale.some-publisher-change-event
      data-type: tarbela:some-publisher
      sink-id: zalando-nakadi

Web endpoints this library provides are secured with oauth2 (spring-security-oauth2 library).

In order the security to work you need to configure oauth2 scopes to access endpoints.

Example configuration:

    spring:
      oauth2:
        application:
          scope:
            read.tarbela_event_log: "#oauth2.hasScope('tarbela-producer.read')"
            write.tarbela_event_log: "#oauth2.hasScope('tarbela-producer.event_log_write')"

Another important thing to configure is a flyway migrations directory.

Make sure that `classpath:db_tarbela/migrations` present in a `flyway.locations` property:

    flyway.locations: classpath:db_tarbela/migrations

This library uses tracer-spring-boot-starter library that provides a support of `X-Flow-ID` header.

This part is also needs to be configured:

    tracer:
      traces:
        X-Flow-ID: flow-id


## Using 

The library implements an interface definition of which you can find in a file `src/main/resources/api/swagger_event-producer-api.yaml`

The API provides:
 
endpoint | description
-------- | -----------
`GET /events` | Using this endpoint Tarbela retrieves some of the new events. The response will support pagination by a next link, using a cursor, assuming there are actually more events.
`PATCH /events` | Using this endpoint Tarbela updates the publishing statuses of some events. This is used to inform the producer when a event was successfully delivered to the event sink or when it couldn't be delivered.
`POST /events/snapshots` | Using this endpoint Tarbela makes producer to create a snapshot events at the producer's site so that Tarbela could request the whole state of the publisher from scratch

The typical use case for this library is to publish events like creating or updating of some objects.

In order to store events you can autowire `EventLogWriter` service and use its methods: `fireCreateEvent` and `fireUpdateEvent`.

Example of using `fireCreateEvent`:

```java
@Service
public class SomeYourService {

    @Autowire
    private EventLogWriter eventLogWriter 
    
    @Transactional
    public void createObject(MyObject data, String flowId) {
        ... here we store an object in a database table
       
        // and then in the same transaction we save the event about this object creation
        eventLogWriter.fireCreateEvent(data, flowId);
    }
}
```

It makes sense to use these methods in one transaction with corresponding object creation or mutation. This way we get rid of distributed-transaction problem as mentioned earlier.

**Important:** In order `POST /events/snapshots` to works your application should implement the `TarbelaSnapshotProvider` interface.
This interface defines only one method:


```java
public interface TarbelaSnapshotProvider<T> {

    Collection<T> getSnapshot();

}
```

The method will be used by `EventLogService` to create the snapshot events of the whole Publisher's state.

## Build

Build with unit tests and integration tests:

    ./mvnw clean install