# nakadi-producer-spring-boot-starter
Nakadi event producer library as a Spring boot starter.

[Nakadi](https://github.com/zalando/nakadi) is a distributed event bus that implements a RESTful API abstraction instead of Kafka-like queues.

The goal of this Spring Boot starter is to simplify the integration between event producer and Nakadi. New events are persisted in a log table as part of the producing JDBC transaction. They will then be sent asynchonously to Nakadi after the transaction completed. If the transaction is rolled back, the events will vanish too. As a result, events will always be sent if and only if the transaction succeeded.

The Transmitter generates a strictly monotonically increasing event id that can be used for ordering the events during retrieval. It is not guaranteed, that events will be sent to Nakadi in the order they have been produced. If an event could not be sent to Nakadi, the library will periodically retry the transmission.

Be aware that this library **does neither guarantee that events are sent exactly once, nor that they are sent in the order they have been persisted**. This is not a bug but a design decision that allows us to skip and retry sending events later in case of temporary failures. So make sure that your events are designed to be processed out of order.  To help you in this matter, the library generates a *strictly monotonically increasing event id* (field `metadata/eid` in Nakadi's event object) that can be used to reconstruct the message order.

Please also be aware that, when udating between major releases of this lib, you must not jump over a major release (1.0 -> 3.0). Please always deploy the intermediate major releases at least once. You may of course always setup a fresh system with the newest version.


## Prerequisites

This library was tested with Spring Boot 1.5.3.RELEASE and relies on existing configured PostgreSQL DataSource.

This library also uses:

* flyway-core
* Spring JDBC
* [fahrschein](https://github.com/zalando-incubator/fahrschein) Nakadi client library
* (Optional) Zalando's tracer-spring-boot-starter
* (Optional) Zalando's tokens library


## Usage
Include the library in your `pom.xml`:
```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>nakadi-producer-spring-boot-starter</artifactId>
    <version>${nakadi-producer.version}</version>
</dependency>
``` 

Use `@EnableNakadiProducer` annotation to activate spring boot starter auto configuration:
```java
@SpringBootApplication
@EnableNakadiProducer
public class Application {
    public static void main(final String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
```

The library uses flyway migrations to set up its own database schema `nakadi_events`.
### Nakadi communication configuration

You must tell the library, where it can reach your Nakadi instance:
```yaml
nakadi-producer:
  nakadi-base-uri: https://nakadi.example.org
```

Since the communication between your application and Nakadi is secured using OAuth2, you must also provide a OAuth2
token. The easiest way to do so is to include the [Zalando Tokens library](https://github.com/zalando/tokens) into your classpath:

```xml
<dependency>
    <groupId>org.zalando.stups</groupId>
    <artifactId>tokens</artifactId>
    <version>${tokens.version}</version>
</dependency>
```

This starter will detect and auto configure it. To do so, it needs to know the address of your oAuth2 server and a comma separated list of scopes it should request:
```yaml
nakadi-producer:
  access-token-uri: https://token.auth.example.org/oauth2/access_token
  access-token-scopes: uid, nakadi.event_stream.write
```

(Note that in the future you'll need a specific scope for writing to each event stream.)

If you do not use the STUPS Tokens library, you can implement token retrieval yourself by defining a Spring bean of type `org.zalando.nakadiproducer.AccessTokenProvider`. The starter will detect it and call it once for each request to retrieve the token. 

### X-Flow-ID (Optional)

This library supports [tracer-spring-boot-starter](https://github.com/zalando/tracer) (another library from Zalando) that provides a support of `X-Flow-ID` header.

In order to use it you should provide the library as a dependency in your project and configure it:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>tracer-spring-boot-starter</artifactId>
    <version>${tracer.version}</version>
</dependency>
```

```yaml
tracer:
  traces:
    X-Flow-ID: flow-id
```

### Creating events

The typical use case for this library is to publish events like creating or updating of some objects.

In order to store events you can autowire the [`EventLogWriter`](src/main/java/org/zalando/nakadiproducer/eventlog/EventLogWriter.java) service and use its methods: `fireCreateEvent`, `fireUpdateEvent`, `fireDeleteEvent`, `fireSnapshotEvent` or `fireBusinessEvent`.

You normally don't need to call `fireSnapshotEvent` directly, see below for [snapshot creation](#event-snapshots).


Example of using `fireCreateEvent`:

```java
@Service
public class SomeYourService {

    @Autowire
    private EventLogWriter eventLogWriter;

    @Autowired
    private WarehouseRepository repository;
    
    @Transactional
    public void createObject(Warehouse data) {
        
        // here we store an object in a database table
        repository.save(data);
       
        // and then in the same transaction we save the event about this object creation
        eventLogWriter.fireCreateEvent("wholesale.warehouse-change-event", "wholesale:warehouse", data);
    }
}
```

**Note:** The parameters to the `fire*Event` methods (except for business events) are the following:

* **eventType** - event type name string that determines to which channel/topic the event will get sent.
                  That event type name needs to exist at Nakadi.
* **dataType** - data type name string that will end up as the `data_type` property of the data change event.
                  ([It is not really clear what this property is used for](https://github.com/zalando/nakadi/issues/382),
                   but it is required.)
* **data** - event data payload itself, which will end up in the `data` property of the data change event.
              This should be an object representing the resource which was created/updated/deleted.
              This doesn't necessarily have to be the same object as you store in your DB, it can be a different
              class which is optimized for JSON serialization. Its JSON serialization should confirm to the
              JSON schema defined at the event type definition in Nakadi.

The choice of the method (*Create/Update/Delete/Snapshot*) event will determine the `data_op` field of the event.

It makes sense to use these methods in one transaction with corresponding object creation or mutation. This way we get rid of distributed-transaction problem as mentioned earlier.

For business events, you have just two parameters, the **eventType** and the event **payload** object.
You usually should fire those also in the same transaction as you are storing the results of the
process step the event is reporting.


### Event snapshots
A Snapshot event is a special type of data change event (data operation) defined by Nakadi.
It does not represent a change of the state of a resource, but a current snapshot of the state of the resource.

You can create snapshot events programmatically (using EventLogWriter.fireSnapshotEvent), but usually snapshot event
creation is a irregular, manually triggered maintenance task.

This library provides a Spring Boot Actuator endpoint named `snapshot_event_creation` that can be used to trigger a Snapshot for a given event type. Assuming your management port is set to `7979`,

    GET localhost:7979/snapshot_event_creation

will return a list of all event types available for snapshot creation and 

    POST localhost:7979/snapshot_event_creation/my.event-type

will trigger a snapshot for the event type `my.event-type`.

This will only  work if your application has configured spring-boot-actuator
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```
and if it implements the `org.zalando.nakadiproducer.snapshots.SnapshotEventProvider` interface as a Spring Bean. Otherwise, the library will respond with an error message when you request a snapshot creation. 

## Test support
This library provides a mock implementation of its Nakadi client that can be used in integration testing:
```java
public class MyIT {

    @Autowired
    private EventTransmitter eventTransmitter;

    @Autowired
    // Just define it in your tests spring config. It will automatically be picked up by the auto configuration.
    private MockNakadiPublishingClient nakadiClient;

    @Before
    @After
    public void clearNakadiEvents() {
        eventTransmitter.sendEvents();
        nakadiClient.clearSentEvents();
    }

    @Test
    public void businessEventsShouldBeSubmittedToNakadi() throws IOException {
        myTransactionalService.doSomethingAndFireEvent();

        eventTransmitter.sendEvents();
        List<String> jsonStrings = nakadiClient.getSentEvents("my_event_type");

        assertThat(jsonStrings.size(), is(1));
        assertThat(read(jsonStrings.get(0), "$.data_op"), is("C"));
        assertThat(read(jsonStrings.get(0), "$.data_type"), is(PUBLISHER_DATA_TYPE));
        assertThat(read(jsonStrings.get(0), "$.data.id"), is(123));
        assertThat(read(jsonStrings.get(0), "$.data.items.length()"), is(3));
        assertThat(read(jsonStrings.get(0), "$.items[0].detail"), is(payload.getItems().get(0).getDetail()));
    }
}
```
The example above uses `com.jayway.jsonpath:json-path:jar:2.2.0` to parse and test the json results
## Build

Build with unit tests and integration tests:

```shell
./mvnw clean install
```

If the GPG integration causes headaches (and you do not plan to publish the created artifact to maven central anyway), 
you can skip gpg signing:

```shell
./mvnw -Dgpg.skip=true clean install
```

## License

The MIT License (MIT) Copyright © 2016 Zalando SE, https://tech.zalando.com

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
