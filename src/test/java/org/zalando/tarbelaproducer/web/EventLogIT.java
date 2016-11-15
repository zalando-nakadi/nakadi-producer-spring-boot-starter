package org.zalando.tarbelaproducer.web;

import org.assertj.core.api.Assertions;
import org.zalando.tarbelaproducer.BaseMockedExternalCommunicationIT;
import org.zalando.tarbelaproducer.TestApplication;
import org.zalando.tarbelaproducer.api.event.model.BunchOfEventsDTO;
import org.zalando.tarbelaproducer.api.event.model.EventUpdateDTO;
import org.zalando.tarbelaproducer.persistance.entity.EventDataOperation;
import org.zalando.tarbelaproducer.persistance.repository.EventLogRepository;
import org.zalando.tarbelaproducer.service.model.EventPayload;
import org.zalando.tarbelaproducer.api.event.model.BunchOfEventUpdatesDTO;
import org.zalando.tarbelaproducer.persistance.entity.EventStatus;
import org.zalando.tarbelaproducer.service.EventLogWriter;
import org.zalando.tarbelaproducer.util.Fixture;
import org.zalando.tarbelaproducer.util.MockPayload;

import static com.google.common.collect.Lists.newArrayList;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static org.zalando.tarbelaproducer.util.Fixture.PUBLISHER_DATA_TYPE;
import static org.zalando.tarbelaproducer.util.Fixture.PUBLISHER_EVENT_OTHER_TYPE;
import static org.zalando.tarbelaproducer.util.Fixture.PUBLISHER_EVENT_TYPE;
import static org.zalando.tarbelaproducer.util.Fixture.SINK_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

public class EventLogIT extends BaseMockedExternalCommunicationIT {

    public static final String SOME_DETAIL = "some detail";

    @Autowired
    private EventLogRepository eventLogRepository;
    private List<MockPayload> mockPayloads = new ArrayList<>();
    private String mockedCode;

    @Autowired
    private EventLogWriter eventLogWriter;

    @Before
    public void setup() {
        eventLogRepository.deleteAll();

        mockedCode = UUID.randomUUID().toString();

        for (int i = 0; i < 5; i++) {
            MockPayload mockPayload = Fixture.mockPayload(i+1, mockedCode+i, true,
                    Fixture.mockSubClass("some info"+i), Fixture.mockSubList(2, SOME_DETAIL +i));
            mockPayloads.add(mockPayload);
            EventPayload eventPayload = Fixture.mockEventPayload(mockPayload);
            eventLogWriter.fireCreateEvent(eventPayload, "SOME_FLOW_ID");
        }

        // make listOfItems return predefined list
        TestApplication.setList(mockPayloads);

    }

    @Test
    public void anEventCanBeRetrieved() {

        MockPayload mockPayload = mockPayloads.get(0);
        given(aHttpsRequest())
                .header("Content-Type", EventController.CONTENT_TYPE_EVENT_LIST)
                .when().get("/events?status={id}", EventStatus.NEW.name())
                .then().assertThat()
                .statusCode(200)
                .body(
                        "_links.next.href", notNullValue(),
                        "events", hasSize(5),
                        "events[0].delivery_status", is(EventStatus.NEW.name()),
                        "events[0].event_payload.data_op", is("C"),
                        "events[0].event_payload.data_type", is(PUBLISHER_DATA_TYPE),
                        "events[0].event_payload.data.id", is(mockPayload.getId()),
                        "events[0].event_payload.data.code", is(mockedCode+"0"),
                        "events[0].event_payload.data.more.info", is(mockPayload.getMore().getInfo()),
                        "events[0].event_payload.data.active", is(mockPayload.isActive()),
                        "events[0].event_payload.data.items[0].detail", is(mockPayload.getItems().get(0).getDetail()),
                        "events[0].event_payload.data.items[1].detail", is(mockPayload.getItems().get(1).getDetail()),
                        "events[0].event_payload.metadata.eid", notNullValue(),
                        "events[0].event_payload.metadata.occurred_at", notNullValue(),
                        "events[0].channel.topic_name", is(PUBLISHER_EVENT_TYPE),
                        "events[0].channel.sink_identifier", is(SINK_ID)
                );
    }

    @Test
    public void occuredAtMetadataFieldMustAlwaysBeISO8601Zulu() {

        given(aHttpsRequest())
                .header("Content-Type", EventController.CONTENT_TYPE_EVENT_LIST)
                .when().get("/events?status={id}", EventStatus.NEW.name())
                .then().assertThat()
                .statusCode(200)
                .body(
                        "_links.next.href", notNullValue(),
                        "events", hasSize(5),
                        "events[0].event_payload.metadata.occurred_at", endsWith("Z")
                );
    }

    @Test
    public void anEventCanBeUpdatedAsSent() {

        BunchOfEventsDTO bunchOfEventsDTO = given(aHttpsRequest())
                .header("Content-Type", EventController.CONTENT_TYPE_EVENT_LIST)
                .when().get("/events?status={id}", EventStatus.NEW.name())
                .as(BunchOfEventsDTO.class);

        Assertions.assertThat(bunchOfEventsDTO.getEvents()).hasSize(5);
        assertThat(bunchOfEventsDTO.getEvents().get(0).getDeliveryStatus()).isEqualTo(EventStatus.NEW.name());

        final EventUpdateDTO updateEventDTO = new EventUpdateDTO();
        updateEventDTO.setEventId(bunchOfEventsDTO.getEvents().get(0).getEventId());
        updateEventDTO.setDeliveryStatus(EventStatus.SENT.name());

        final BunchOfEventUpdatesDTO updates = new BunchOfEventUpdatesDTO();
        updates.setEvents(newArrayList(updateEventDTO));

        given(aHttpsRequest())
                .header("Content-Type", EventController.CONTENT_TYPE_EVENT_LIST_UPDATE)
                .body(updates)
                .when().patch("/events")
                .then().assertThat()
                .statusCode(200);

        given(aHttpsRequest())
                .header("Content-Type", EventController.CONTENT_TYPE_EVENT_LIST)
                .when().get("/events")
                .then().assertThat()
                .statusCode(200)
                .log().body()
                .body(
                        "_links.next.href", notNullValue(),
                        "events", hasSize(5),
                        "events[0].delivery_status", is(EventStatus.SENT.name())
                );
    }

    @Test
    public void snapshotEventsCanBeCreatedAndRetrieved() {
        eventLogRepository.deleteAll();

        // Create snapshot
        given(aHttpsRequest())
                .when().post("/events/snapshots/" + PUBLISHER_EVENT_TYPE)
                .then().assertThat().statusCode(201);

        // Check created events. They should consist of events created from mockPayloads
        given(aHttpsRequest())
                .header("Content-Type", EventController.CONTENT_TYPE_EVENT_LIST)
                .when().get("/events?status={id}", EventStatus.NEW.name())
                .then().assertThat().statusCode(200)
                .body(
                        "_links.next.href", notNullValue(),
                        "events", hasSize(5),
                        "events.event_payload.data_op",
                                everyItem(equalTo(EventDataOperation.SNAPSHOT.toString())),
                        "events.channel.topic_name",
                                everyItem(equalTo(PUBLISHER_EVENT_TYPE)),
                        "events.channel.sink_identifier",
                                everyItem(equalTo(SINK_ID)),
                        "events.event_payload.data.id",
                                hasItems(mockPayloads.stream().map(MockPayload::getId).toArray()),
                        "events.event_payload.data.code",
                                hasItems(mockPayloads.stream().map(MockPayload::getCode).toArray()),
                        "events.event_payload.data.active",
                                hasItems(mockPayloads.stream().map(MockPayload::isActive).toArray()),
                        "events.event_payload.data.more.info",
                                hasItems(mockPayloads.stream().map(it -> it.getMore().getInfo()).toArray()),
                        "events.event_payload.data.items.detail",
                                everyItem(hasSize(2)),
                        "events.event_payload.data.items.detail",
                                everyItem(hasItems(startsWith(SOME_DETAIL)))
                );

    }

    @Test
    public void snapshotEventsOfDifferentTypesCanBeCreatedAndRetrieved() {
        eventLogRepository.deleteAll();

        // Create snapshot
        given(aHttpsRequest())
                .when().post("/events/snapshots/" + PUBLISHER_EVENT_OTHER_TYPE)
                .then().assertThat().statusCode(201);

        // Check created events. They should consist of events created from mockPayloads
        given(aHttpsRequest())
                .header("Content-Type", EventController.CONTENT_TYPE_EVENT_LIST)
                .when().get("/events?status={id}", EventStatus.NEW.name())
                .then().assertThat().statusCode(200)
                .body(
                        "_links.next.href", notNullValue(),
                        "events", hasSize(5),
                        "events.event_payload.data_op",
                                everyItem(equalTo(EventDataOperation.SNAPSHOT.toString())),
                        "events.channel.topic_name",
                                everyItem(equalTo(PUBLISHER_EVENT_OTHER_TYPE)),
                        "events.channel.sink_identifier",
                                everyItem(equalTo(SINK_ID)),
                        "events.event_payload.data.id",
                                hasItems(mockPayloads.stream().map(MockPayload::getId).toArray()),
                        "events.event_payload.data.code",
                                hasItems(mockPayloads.stream().map(MockPayload::getCode).toArray()),
                        "events.event_payload.data.active",
                                hasItems(mockPayloads.stream().map(MockPayload::isActive).toArray()),
                        "events.event_payload.data.more.info",
                                hasItems(mockPayloads.stream().map(it -> it.getMore().getInfo()).toArray()),
                        "events.event_payload.data.items.detail",
                                everyItem(hasSize(2)),
                        "events.event_payload.data.items.detail",
                                everyItem(hasItems(startsWith(SOME_DETAIL)))
                );

    }

    @Test
    public void snapshotEventsOfUnknownTypeThrowsAnError() {
        eventLogRepository.deleteAll();

        String unknownEventType = "unknown.event-type";

        // Create snapshot
        given(aHttpsRequest())
                .when().post("/events/snapshots/" + unknownEventType)
                .then().assertThat()
                .statusCode(422)
                .contentType(containsString(EventController.CONTENT_TYPE_PROBLEM))
                .body(
                    "type", is("http://httpstatus.es/422"),
                    "status", is(422),
                    "title", is("No event log found"),
                    "detail", is("No event log found for event type ("+unknownEventType+")."),
                    "instance", startsWith("X-Flow-ID")
                );

    }

}
