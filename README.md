[![build status badge](https://img.shields.io/travis/zalando-nakadi/nakadi-producer-spring-boot-starter.svg?label=Travis%20Build)](https://travis-ci.org/zalando-nakadi/nakadi-producer-spring-boot-starter/branches)
[![last Github release badge](https://img.shields.io/github/release/zalando-nakadi/nakadi-producer-spring-boot-starter.svg?label=Last%20Release)](https://github.com/zalando-nakadi/nakadi-producer-spring-boot-starter/releases)
![last maven central release badge](https://img.shields.io/maven-central/v/org.zalando/nakadi-producer-spring-boot-starter.svg)
[![MIT license](https://img.shields.io/github/license/zalando-nakadi/nakadi-producer-spring-boot-starter.svg)](LICENSE)

# nakadi-producer-spring-boot-starter

[Nakadi](https://github.com/zalando/nakadi) is a distributed event bus that implements a RESTful API abstraction instead of Kafka-like queues.

The goal of this Spring Boot starter is to simplify the reliable integration between event producer and Nakadi. When we send events from a transactional application, a few recurring challenges appear:
- we have to make sure that events from a transaction get sent, when the transaction has been committed,
- we have to make sure that events from a transaction do not get sent, when the transaction has been rolled back,
- we have to make sure that events get sent, even if an error occurred while sending the event,
- we want to give the event consumer a way to infer the order in which the events occurred and
- it is very comfortable for initial data loads and error recovery to be able to generate snapshots of the current db state as events.

There are already [multiple clients for the Nakadi REST API](https://zalando.github.io/nakadi/manual.html#using_clients), but none of them solves the mentioned issues. 

We solved them by persisting new events in a log table as part of the producing JDBC transaction. They will then be sent asynchonously to Nakadi after the transaction completed. If the transaction is rolled back, the events will vanish too. As a result, events will always be sent if and only if the transaction succeeded.

The Transmitter generates a strictly monotonically increasing event id that can be used for ordering the events during retrieval. It is not guaranteed, that events will be sent to Nakadi in the order they have been produced. If an event could not be sent to Nakadi, the library will periodically retry the transmission.

This project is mature, used in production in some services at Zalando, and in active development.

Be aware that this library **does neither guarantee that events are sent exactly once, nor that they are sent in the order they have been persisted**. This is not a bug but a design decision that allows us to skip and retry sending events later in case of temporary failures. So make sure that your events are designed to be processed out of order.  To help you in this matter, the library generates a *strictly monotonically increasing event id* (field `metadata/eid` in Nakadi's event object) that can be used to reconstruct the message order.

## Versioning

This library follows the [semantic versioning](http://semver.org/) schema. A major version change means either an
incompatible change in the API, or some incompatible behavioral change (e.g. the database usage), minor versions
mean new features without breaking compatibility, and patch versions are backwards compatible bug fixes.

Please also be aware that, when udating between major releases of this lib, you must not jump over a major
release (1.0 → 3.0). Please always deploy the intermediate major releases at least once – otherwise you might
lose events. You will find migration instructions between major release in
[the release notes](https://github.com/zalando-incubator/nakadi-producer-spring-boot-starter/releases).

You may of course always setup a fresh system with the newest version.


## Prerequisites

This library was tested with Spring Boot 2.0.3.RELEASE and relies on an existing configured PostgreSQL DataSource. 
**If you are still using Spring Boot 1.x, please use versions < 20.0.0, they are still actively maintained ([Documentation](https://github.com/zalando-nakadi/nakadi-producer-spring-boot-starter/tree/spring-boot-1)).**

This library also uses:

* flyway-core
* Spring JDBC
* [fahrschein](https://github.com/zalando-nakadi/fahrschein) Nakadi client library
* jackson >= 2.7.0
* (optional) Zalando's [tracer-spring-boot-starter](https://github.com/zalando/tracer)
* (optional) Zalando's [tokens library](https://github.com/zalando/tokens) >= 0.10.0
    * Please note that [tokens-spring-boot-starter](https://github.com/zalando-stups/spring-boot-zalando-stups-tokens) 0.10.0 comes with tokens 0.9.9, which is not enough. You can manually add tokens 0.10.0 with that starter, though. To be used in zalandos k8s environment, you must at least use 0.11.0.


## Usage

### Setup

If you are using maven, include the library in your `pom.xml`:

```xml
<dependency>
    <groupId>org.zalando</groupId>
    <artifactId>nakadi-producer-spring-boot-starter</artifactId>
    <version>${nakadi-producer.version}</version>
</dependency>
```

The latest available version is visible in the Maven central badge at the top of this README. 

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

By default, Nakadi-producer-spring-boot starter uses the Fahrschein library to submit its events. It needs some configuration to know how to do this – we support two ways:

* Using existing Fahrschein setup
* Letting this library set things up

#### Using existing Fahrschein setup

If you are already using the [Fahrschein library](https://github.com/zalando-nakadi/fahrschein) directly (e.g. for event consumption) and have already a configured `org.zalando.fahrschein.NakadiClient` object, just make sure it is available as a Spring bean. Nakadi-Producer-Spring-Boot-Starter will pick it up and use it directly.

The configuration in the next section is then not needed at all.

#### Letting this library set things up 

You must tell the library, where it can reach your Nakadi instance:
```yaml
nakadi-producer:
  nakadi-base-uri: https://nakadi.example.org
```

Since the communication between your application and Nakadi is secured using OAuth2, you must also provide an OAuth2
token. The easiest way to do so is to include the [Zalando Tokens library](https://github.com/zalando/tokens) into your classpath:

```xml
<dependency>
    <groupId>org.zalando.stups</groupId>
    <artifactId>tokens</artifactId>
    <version>${tokens.version}</version>
</dependency>
```

This starter will detect and auto configure it. To do so, it needs to know the address of your oAuth2 server:
```yaml
nakadi-producer:
  access-token-uri: https://token.auth.example.org/oauth2/access_token
```

If your application is running in Zalando's Kubernetes environment, you also have to configure the credential rotation:
```yaml
apiVersion: "zalando.org/v1"
kind: PlatformCredentialsSet
metadata:
   name: {{{APPLICATION}}}-credentials
spec:
   application: {{{KIO_NAME}}}
   token_version: v2
   tokens:
     nakadi:
       privileges: []
``` 

Since [July 2017](https://github.com/zalando/nakadi/pull/692), Nakadi (at least in the version operated at Zalando) doesn't require any scopes other than the pseudo-scope `uid` for writing events, [the authorization is instead based on event-type configuration using the service's uid](https://nakadi.io/manual.html#using_authorization).

If your Nakadi installation needs real scopes for submitting events, you can provide them via configuration, too (as a comma-separated list):

```yaml
nakadi-producer:
  access-token-uri: https://token.auth.example.org/oauth2/access_token
  access-token-scopes: my.scope.name,other.scope.name
```

If you do not use the STUPS Tokens library, you can implement token retrieval yourself by defining a Spring bean of type `org.zalando.nakadiproducer.AccessTokenProvider`. The starter will detect it and call it once for each request to retrieve the token. 


### Creating events

The typical use case for this library is to publish events like creating or updating of some objects.

In order to store events you can autowire the [`EventLogWriter`](src/main/java/org/zalando/nakadiproducer/eventlog/EventLogWriter.java) service and use its methods: `fireCreateEvent`, `fireUpdateEvent`, `fireDeleteEvent`, `fireSnapshotEvent` or `fireBusinessEvent`.

You normally don't need to call `fireSnapshotEvent` directly, see below for [snapshot creation](#event-snapshots-optional).


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
    class which is optimized for JSON serialization.
      
    It will be serialized using the default Jackson ObjectMapper bean – so make sure all properties
    which should be in the event are accessible (usually via public getters), or annotated with the
    usual Jackson annotations. The JSON serialization should confirm to the JSON schema defined at
    the event type definition in Nakadi.


The choice of the method (*Create/Update/Delete/Snapshot*) event will determine the `data_op` field of the event.

It makes sense to use these methods in one transaction with corresponding object creation or mutation. This way we get rid of distributed-transaction problem as mentioned earlier.

For business events, you have just two parameters, the **eventType** and the event **payload** object.
You usually should fire those also in the same transaction as you are storing the results of the
process step the event is reporting.


### Event snapshots (optional)

A Snapshot event is a special type of data change event (data operation) defined by Nakadi.
It does not represent a change of the state of a resource, but a current snapshot of its state. It can be usefull to
bootstrap a new consumer or to recover from inconsistencies between sender and consumer after an incident.

You can create snapshot events programmatically (using EventLogWriter.fireSnapshotEvent), but usually snapshot event
creation is a irregular, manually triggered maintenance task.

This library provides a Spring Boot Actuator endpoint named `snapshot_event_creation` that can be used to trigger a Snapshot for a given event type. Assuming your management port is set to `7979`,

    GET localhost:7979/actuator/snapshot-event-creation

will return a list of all event types available for snapshot creation and 

    POST localhost:7979/actuator/snapshot-event-creation/my.event-type

will trigger a snapshot for the event type `my.event-type`. You can change the port, the authentication scheme and the
path prefix as part of your Spring Boot Actuator configuration.

You can provide an optional filter specifier that will be passed to your application to implement any application 
specific event/entity filtering logic.  It can be provided either as a request parameter called `filter`, or as a
request body

    {"filter":"myFilter"}

This endpoint will only work if your application includes spring-boot-actuator,
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```
your `application.properties` includes
``` 
management.endpoints.web.exposure.include=snapshot-event-creation,your-other-endpoints,...`
```
and if one or more Spring Beans implement the `org.zalando.nakadiproducer.snapshots.SnapshotEventGenerator` interface.
The optional filter specifier of the trigger request will be passed as a string parameter to the
SnapshotEventGenerator's `generateSnapshots` method and may be null, if none is given.

We provide a `SimpleSnapshotEventGenerator` to ease bean creation using a more functional style:
```java
@Bean
public SnapshotEventGenerator snapshotEventGenerator(MyService service) {
    return new SimpleSnapshotEventGenerator("event type", service::createSnapshotEvents);
}
```

### X-Flow-ID (optional)

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

### Customizing Database Setup (optional)

By default, the library will pick up your flyway data source (or the primary data source if no flyway data source is
configured), create its own schema and start setting up its tables in there. You can customize this process in two ways:

If you want to use a different data source for schema maintainence (for example to use a different username) and 
configuring the Spring Flyway datasource is not enough, your can define a spring bean of type `DataSource` and annotate 
it with `@NakadiProducerDataSource`.

You may also define a spring bean of type `NakadiProducerFlywayCallback`. The interface provides several hooks into the
schema management lifecycle that may, for example, be used to `SET ROLE migrator` before and `RESET ROLE` after each
migration. 

### Test support

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
The example above uses `com.jayway.jsonpath:json-path:jar:2.2.0` to parse and test the json results.

Note that you should disable the scheduled event transmission for the test (e.g. by setting `nakadi-producer.scheduled-transmission-enabled:false`), as that might interfere with the manual transmission and the clearing in the test setup, leading to events from one test showing up in the next test, depending on timing issues.

## Contributing

We welcome contributions. Please have a look at our [contribution guidelines](CONTRIBUTING.md).
 
If you have an idea of what the library should do, please have a look into our [Issues][issues] to see whether it was already proposed before, and otherwise open an issue. We also welcome pull requests (for your issues or even for issues from others).

If you want to support us, we collected a few [open issues that should be easy to contribute][help-wanted]. 

In the interest of fostering an open and welcoming environment, we follow and enforce our [Code of Conduct](CODE_OF_CONDUCT.md).

### Build

Build with unit tests and integration tests:

```shell
./mvnw clean install
```

This will sign the created artifact, which is needed for publication to Maven Central.
If the GPG integration causes headaches (and you do not plan to publish the created artifact to maven central anyway), 
you can skip gpg signing:

```shell
./mvnw -Dgpg.skip=true clean install
```

### Thanks

We (the [maintainers](MAINTAINERS)) want to thank our main contributors:

* Alexander Libin (@qlibin), who created a similar predecessor library (tarbela-producer-spring-boot-starter,
  now not public anymore), from which this one was forked.
* Lucas Medeiros de Azevedo (@wormangel), who added support for business events.

### Contact

For all questions, bug reports, proposals, etc., please
[create an issue](https://github.com/zalando-nakadi/nakadi-producer-spring-boot-starter/issues/new).
We try to react to new issues latest at the next working day.

If you need to contact the maintainers confidentially, please use the email addresses
in the [MAINTAINERS](MAINTAINERS) file. In case of a security issue, please also send
a copy to tech-security@zalando.de.


## License

The MIT License (MIT) Copyright © 2016 Zalando SE, https://tech.zalando.com

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

[issues]: https://github.com/zalando-nakadi/nakadi-producer-spring-boot-starter/issues?utf8=%E2%9C%93&q=is%3Aissue
[help-wanted]: https://github.com/zalando-nakadi/nakadi-producer-spring-boot-starter/labels/help%20wanted
