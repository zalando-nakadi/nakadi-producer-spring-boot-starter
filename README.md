# tarbela-producer-spring-boot-starter
Tarbela event producer API implementation as a Spring boot starter 

:rocket:

Tarbela is a reliable generic event publisher [according to documentation](https://libraries.io/github/zalando-incubator/tarbela)

The goal of this Spring Boot starter is to simplify the integration between event producer and Tarbela publisher reducing boiler plate code.

The important thing is that the new events are stored in the same database (and using the same JDBC connections and transactions) as the actual data we want to store (thereby making the distributed-transaction problem go away).


## Installation

Build and install the library into your local Maven repository:

```sh
./mvnw clean install
```

Add the following dependency into the pom.xml of your Spring-Boot application

```xml
<dependency>
    <groupId>de.zalando.wholesale.tarbelaevents</groupId>
    <artifactId>tarbela-producer-spring-boot-starter</artifactId>
    <version>${tarbela-events.version}</version>
</dependency>
```

### Prerequisites

This library tested with Spring Boot 1.4.1 and relies on existing PostgreSQL DataSource configured

This library also uses:

* flyway-core 4.0.3
* querydsl-jpa 4.1.4
* Zalando's stups-spring-oauth2-server 1.0.15-GH42-1
* (Optional) Zalando's tracer-spring-boot-starter 0.11.2 

## Configuration

Use `@EnableTarbelaEvents` annotation to activate spring boot starter auto configuration

```java
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

### Tarbela sinkId

Configure Tarbela Sink identifier in application properties:

```yaml
tarbela:
  sink-id: zalando-nakadi
```

### Security

Web endpoints this library provides are secured with oauth2 (spring-security-oauth2 library).

In order the security to work you need to configure oauth2 scopes to access endpoints.

Example configuration:

```yaml
spring:
  oauth2:
    application:
      scope:
        read.tarbela_event_log: "#oauth2.hasScope('tarbela-producer.read')"
        write.tarbela_event_log: "#oauth2.hasScope('tarbela-producer.event_log_write')"
```

### Database

Another important thing to configure is a flyway migrations directory.

Make sure that `classpath:db_tarbela/migrations` is present in a `flyway.locations` property:

```yaml
flyway.locations: classpath:db_tarbela/migrations
```

If you have you own `flyway.locations` property configured then just extend it with `, classpath:db_tarbela/migrations` (with a comma).

Example:

```yaml
flyway.locations: classpath:my_db/migrations, classpath:db_tarbela/migrations
```

### X-Flow-ID (Optional)

This library supports tracer-spring-boot-starter (another library from Zalando) that provides a support of `X-Flow-ID` header.

In order to use it you should provide the library as a dependency in your project and configure it:

```yaml
tracer:
  traces:
    X-Flow-ID: flow-id
```

## Using 

The library implements an interface definition of which you can find in a file `src/main/resources/api/swagger_event-producer-api.yaml`

The API provides:
 
endpoint | description
-------- | -----------
`GET /events` | Using this endpoint Tarbela retrieves some of the new events. The response will support pagination by a next link, using a cursor, assuming there are actually more events.
`PATCH /events` | Using this endpoint Tarbela updates the publishing statuses of some events. This is used to inform the producer when a event was successfully delivered to the event sink or when it couldn't be delivered.
`POST /events/snapshots/{event_type}` | Using this endpoint Tarbela makes producer to create a snapshot events at the producer's site so that Tarbela could request the whole state of the publisher from scratch

### Creating events

The typical use case for this library is to publish events like creating or updating of some objects.

In order to store events you can autowire `EventLogWriter` service and use its methods: `fireCreateEvent` and `fireUpdateEvent`.

Example of using `fireCreateEvent`:

```java
@Service
public class SomeYourService {

    @Autowire
    private EventLogWriter eventLogWriter 
    
    @Transactional
    public void createObject(Warehouse data, String flowId) {
        
        ...
        ... here we store an object in a database table
        ...
       
        // compose an event payload
        EventPayload eventPayload = EventPayloadImpl.builder()
                .data(data)
                .eventType("wholesale.warehouse-change-event")
                .dataType("wholesale:warehouse")
                .build();

        // and then in the same transaction we save the event about this object creation
        eventLogWriter.fireCreateEvent(eventPayload, flowId);
    }
}
```

**Note:** `flowId` is an optional parameter that will be saved with the event to make it traceable. It could be a value of an X-Flow-ID header.

**Note:** `EventPayload` is an event payload structure that should contain following information:

* **eventType** - predefined event type sting name that will be attached to each `EventDTO`'s `channel` object as a `topicName` property
* **dataType** - predefined data type string name that will be attached to each `EventDTO`'s `event_payload` object as a `data_type` property
* **data** - event data payload itself that will be attached to each `EventDTO`'s `event_payload` object as a `data` property

It makes sense to use these methods in one transaction with corresponding object creation or mutation. This way we get rid of distributed-transaction problem as mentioned earlier.

### Event snapshots

**Important:** In order `POST /events/snapshots/{event_type}` to work your application should implement the `TarbelaSnapshotProvider` interface.

```java
public interface TarbelaSnapshotProvider {

    /**
     * Returns a stream consisting of elements for creating a snapshot of events
     * of given type (event type is an event channel topic name).
     * @return stream of elements to create a snapshot from
     */
    Stream<EventPayload> getSnapshot(@NotNull String eventType);

}
```

If you will not implement and define the `TarbelaSnapshotProvider` as a Spring Bean the library will configure a fake `TarbelaSnapshotProvider` which will throw TarbelaSnapshotProviderNotImplementedException upon the any `POST /events/snapshots/*` request

The method will be used by `EventLogService` to create snapshot events of the whole Publisher's state.

`EventLogService` will take batches of elements from the stream returned by `getSnapshot` method and save those batches sequentially in tarbela_event_log table.
 
The default size of the batch is 25 and it can be adjusted via `tarbela.snapshot-batch-size` property:

```yaml
tarbela:
  snapshot-batch-size: 100
```

## Build

Build with unit tests and integration tests:

    ./mvnw clean install