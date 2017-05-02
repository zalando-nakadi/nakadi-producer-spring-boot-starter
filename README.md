# nakadi-producer-spring-boot-starter
Nakadi event producer as a Spring boot starter 

:rocket:

A distributed event bus that implements a RESTful API abstraction instead of Kafka-like queues [according to documentation](https://github.com/zalando/nakadi)

The goal of this Spring Boot starter is to simplify the integration between event producer and Nakdi reducing boiler plate code.

The important thing is that the new events are persisted in a log table as part of the producing JDBC transaction. They will then be sent asynchonously to Nakadi after the transaction completed. If the transaction is rolled back, the events will vanish to. As a result, events will allways be sent if and only be sent if the transaction succeeded.


## Installation

Build and install the library into your local Maven repository:

```shell
./mvnw clean install
```

Add the following dependency into the pom.xml of your Spring-Boot application

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>nakadi-producer-spring-boot-starter</artifactId>
    <version>${nakadi-producer.version}</version>
</dependency>
```

### Prerequisites

This library tested with Spring Boot 1.4.1 and relies on existing PostgreSQL DataSource configured

This library also uses:

* flyway-core 4.0.3
* querydsl-jpa 4.1.4
* (Optional) Zalando's tracer-spring-boot-starter 0.11.2 

## Configuration

Use `@EnableNakadiProducer` annotation to activate spring boot starter auto configuration

```java
@SpringBootApplication
@EnableNakadiProducer
public class Application {
    public static void main(final String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
```

This will configure: 

* the database table for events 
* EventLogService service for writing events into the table 
* A scheduled job that sents your events to nakadi

### Data access layer configuration

Library relies on Spring Data JPA. In order for Spring to pick up needed repository and entity you should explicitly configure it using this annotations:

```java
@EnableJpaRepositories("org.zalando.nakadiproducer.persistence")
@EntityScan("org.zalando.nakadiproducer.persistence")
```

If you also use Spring Data JPA and you have your own repositories and entities, you should set them all like this:

```java
@EnableJpaRepositories({"path.to.your.package.containing.repositories", "org.zalando.nakadiproducer.persistence"})
@EntityScan({"path.to.your.package.containing.jpa.entities", "org.zalando.nakadiproducer.persistence"})
```

You can apply those annotations to any @Configuration marked class of your Spring Boot application.

### Database

Another important thing to configure is a flyway migrations directory.

Make sure that `classpath:db_nakadiproducer/migrations` is present in a `flyway.locations` property:

```yaml
flyway.locations: classpath:db_nakadiproducer/migrations
```

If you have you own `flyway.locations` property configured then just extend it with `, classpath:db_nakadiproducer/migrations` (with a comma).

Example:

```yaml
flyway.locations: classpath:my_db/migrations, classpath:db_nakadiproducer/migrations
```

#### Schema permissions

Note that by default schema permissions look like this:

```sql
GRANT USAGE ON SCHEMA nakadi_events TO PUBLIC;
GRANT SELECT ON nakadi_events.event_log TO PUBLIC;
GRANT INSERT ON nakadi_events.event_log TO PUBLIC;
GRANT UPDATE ON nakadi_events.event_log TO PUBLIC;
GRANT USAGE ON SEQUENCE nakadi_events.event_log_id_seq TO PUBLIC; 
```

If you need to restrict permissions to the schema change it via your database migrations

### Nakadi communication configuration

You must tell the library, where it can reach your Nakadi instance:
```yaml
nakadi-producer:
  access-token-uri: https://nakadi.example.org
```

Since the communication between your application and Nakadi is secured using OAuth2, you must also provide a OAuth2
token. The easiest way to do so is to include the stups token library into your classpath:

```xml
<dependency>
    <groupId>org.zalando.stups</groupId>
    <artifactId>tokens</artifactId>
    <version>0.11.0-beta-2</version>
</dependency>
```

The library will detect and auto configure it. To do so, it needs two know the address of your oAuth2 server and a list of scopes it should request:
```yaml
nakadi-producer:
  access-token-uri: https://token.auth.example.org/oauth2/access_token
  access-token-scopes: 
    - uid
    - nakadi.write
```

If you do not use the zalando tokens library, you can implement token retrieval yourself by defining a Spring bean of type `org.zalando.nakadiproducer.AccessTokenProvider`. The library will detect it and call it once for each request to retrieve the token. 

### X-Flow-ID (Optional)

This library supports tracer-spring-boot-starter (another library from Zalando) that provides a support of `X-Flow-ID` header.

In order to use it you should provide the library as a dependency in your project and configure it:

```yaml
tracer:
  traces:
    X-Flow-ID: flow-id
```

### Security

The library does not provide any security. 
You should secure the `/events/snapshots/{event_type}` endpoint as you need for your application

## Using 

The library implements an interface definition of which you can find in a file `src/main/resources/api/swagger_event-producer-api.yaml`

The API provides:
 
endpoint | description
-------- | -----------
`POST /events/snapshots/{event_type}` | This endpoint (a post without any body) can be used by operators to trigger creation of snapshot events in the producer. Those events will then be collected and published to Nakadi, so event consumers can get a full snapshot of the database.

### Creating events

The typical use case for this library is to publish events like creating or updating of some objects.

In order to store events you can autowire `EventLogWriter` service and use its methods: `fireCreateEvent` and `fireUpdateEvent`.

Example of using `fireCreateEvent`:

```java
@Service
public class SomeYourService {

    @Autowire
    private EventLogWriter eventLogWriter; 
    
    @Transactional
    public void createObject(Warehouse data, String flowId) {
        
        // ...
        // ... here we store an object in a database table
        // ...
       
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

**Important:** In order `POST /events/snapshots/{event_type}` to work your application should implement the `SnapshotEventProvider` interface.

```java
public interface SnapshotEventProvider {

    /**
     * Returns a stream consisting of elements for creating a snapshot of events
     * of given type (event type is an event channel topic name).
     * @param eventType event type to make a snapshot of
     * @return stream of elements to create a snapshot from
     * @throws UnknownEventTypeException if {@code eventType} is unknown
     */
    Stream<EventPayload> getSnapshot(@NotNull String eventType);

}
```

If you do not implement and define the `SnapshotEventProvider` as a Spring Bean the library will configure a fake `SnapshotEventProvider` which will throw `SnapshotEventProviderNotImplementedException` upon the any `POST /events/snapshots/*` request

The method will be used by `EventLogService` to create snapshot events of the whole Publisher's state.

`EventLogService` will take batches of elements from the stream returned by `getSnapshot` method and save those batches sequentially in event_log table.
 
The default size of the batch is 25 and it can be adjusted via `nakadi-producer.snapshot-batch-size` property:

```yaml
nakadi-producer:
  snapshot-batch-size: 100
```

## Build

Build with unit tests and integration tests:

```shell
./mvnw clean install
```


## License

The MIT License (MIT) Copyright © 2016 Zalando SE, https://tech.zalando.com

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.