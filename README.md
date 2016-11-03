# tarbela-events-spring-boot-starter
Tarbela event producer API implementation as a Spring boot starter

Tarbela is a reliable generic event publisher [according to documentation](https://libraries.io/github/zalando-incubator/tarbela)

The goal of this Spring Boot starter is to simplify the integration between event producer and Tarbela publisher.

## Installation

Add the following dependency into the pom.xml of your Spring-Boot application

    <dependency>
        <groupId>de.zalando.wholesale.tarbelaevents</groupId>
        <artifactId>tarbela-events-spring-boot-starter</artifactId>
        <version>${tarbela-events.version}</version>
    </dependency>

### Prerequisites

* Spring Boot 1.4.1

TODO: to be extended

## Configuration

Use `@EnableTarbelaEvents` annotation to activate spring boot starter auto configuration

    @SpringBootApplication
    @EnableTarbelaEvents
    public class Application {
        public static void main(final String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }

This will configure: 

* the database table for events 
* EventLogService service for writing events into the table 
* controller listening `/events` endpoint that will publish the events for Tarbela

Configure event type, event data type and sinkId in application properties:

    tarbela:
      event_type: wholesale.some-publisher-change-event
      data_type: tarbela:some-publisher
      sinkId: zalando-nakadi

## Using 

The library implements an interface definition of which you can find in a file `src/main/resources/api/swagger_event-producer-api.yaml`

The API provides:
 
* `GET /events` 
Using this endpoint Tarbela retrieves some of the new events. The response will support pagination by a next link, using a cursor, assuming there are actually more events.
* `PATCH /events`
Using this endpoint Tarbela updates the publishing statuses of some events. This is used to inform the producer when a event was successfully delivered to the event sink or when it couldn't be delivered.
* `POST /events/snapshots`
Using this endpoint Tarbela makes producer to create a snapshot events at the producer's site so that Tarbela could request the whole state of the publisher from scratch

The typical use case for this library is to publish events like creating or updating of some objects.

In order to store events use `EventLogService` service methods `fireCreateEvent` and `fireUpdateEvent`.
Usually it makes sense to use these methods in one transaction with corresponding object creation or mutation.

**Important:** In order `POST /events/snapshots` to works your application should implement the `TarbelaSnapshotProvider` interface.
This interface defines only one method:


    public interface TarbelaSnapshotProvider<T> {
    
        Collection<T> getSnapshot();
    
    }

The method will be used by `EventLogService` to create the snapshot events of the whole Publisher's state.