package de.zalando.wholesale.tarbelapublisher.web;

import de.zalando.wholesale.tarbelapublisher.BaseMockedExternalCommunicationIT;
import de.zalando.wholesale.tarbelapublisher.TestApplication;
import de.zalando.wholesale.tarbelapublisher.api.event.model.BunchOfEventUpdatesDTO;
import de.zalando.wholesale.tarbelapublisher.api.event.model.BunchOfEventsDTO;
import de.zalando.wholesale.tarbelapublisher.api.event.model.EventUpdateDTO;
import de.zalando.wholesale.tarbelapublisher.persistance.entity.event.EventDataOperation;
import de.zalando.wholesale.tarbelapublisher.persistance.entity.event.EventStatus;
import de.zalando.wholesale.tarbelapublisher.persistance.repository.EventLogRepository;
import de.zalando.wholesale.tarbelapublisher.service.event.EventLogService;
import de.zalando.wholesale.tarbelapublisher.util.Fixture;
import de.zalando.wholesale.tarbelapublisher.util.MockPayload;

import static com.google.common.collect.Lists.newArrayList;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.given;
import static de.zalando.wholesale.tarbelapublisher.util.Fixture.PUBLISHER_DATA_TYPE;
import static de.zalando.wholesale.tarbelapublisher.util.Fixture.PUBLISHER_EVENT_TYPE;
import static de.zalando.wholesale.tarbelapublisher.util.Fixture.SINK_ID;
import static de.zalando.wholesale.tarbelapublisher.web.EventController.CONTENT_TYPE_EVENT_LIST;
import static de.zalando.wholesale.tarbelapublisher.web.EventController.CONTENT_TYPE_EVENT_LIST_UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class EventLogIT extends BaseMockedExternalCommunicationIT {
    @Autowired
    private EventLogRepository eventLogRepository;
    private List<MockPayload> mockPayloads = new ArrayList<>();
    private String mockedCode;

    @Autowired
    private EventLogService eventLogService;

    private static final String[] ALL_SCOPES_EXCEPT_READ = {
            UID_SCOPE, EVENT_LOG_WRITE_SCOPE
    };

    private static final String[] ALL_SCOPES_EXCEPT_EVENT_LOG_WRITE = {
            UID_SCOPE, READ_SCOPE
    };
    @Value("${tarbela.sinkId}")
    private String sinkId;

    @Before
    public void setup() {
        eventLogRepository.deleteAll();

        mockedCode = UUID.randomUUID().toString();

        for (int i = 0; i < 5; i++) {
            MockPayload mockPayload = Fixture.mockPayload(i+1, mockedCode+i, true,
                    Fixture.mockSubClass("some info"+i), Fixture.mockSubList(2, "some detail"+i));
            mockPayloads.add(mockPayload);
            eventLogService.fireCreateEvent(mockPayload, PUBLISHER_EVENT_TYPE, PUBLISHER_DATA_TYPE, "SOME_FLOW_ID");
        }

        // make listOfItems return predefined list
        TestApplication.setList(mockPayloads);

    }

    @Test
    public void getEventsWithoutTheCorrectScopeGivesAnError() {
        given(anAuthenticatedRequestWithScopes(ALL_SCOPES_EXCEPT_READ))
                .when().get("/events")
                .then().statusCode(403);
    }

    @Test
    public void patchEventLogWithoutTheCorrectScopeGivesAnError() {
        given(anAuthenticatedRequestWithScopes(ALL_SCOPES_EXCEPT_EVENT_LOG_WRITE))
                .header("Content-Type", CONTENT_TYPE_EVENT_LIST_UPDATE)
                .body(new BunchOfEventUpdatesDTO())
                .when().patch("/events")
                .then().statusCode(403);
    }

    @Test
    public void anEventCanBeRetrieved() {

        MockPayload mockPayload = mockPayloads.get(0);
        given(anAuthorizedRequest())
                .header("Content-Type", CONTENT_TYPE_EVENT_LIST)
                .when().get("/events?status={id}", EventStatus.NEW.name())
                .then().assertThat()
                .statusCode(200)
                .body(
                        "_links.next.href", notNullValue(),
                        "events", hasSize(5),
                        "events[0].delivery_status", is(EventStatus.NEW.name()),
                        "events[0].event_payload.data_op", is("C"),
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

        given(anAuthorizedRequest())
                .header("Content-Type", CONTENT_TYPE_EVENT_LIST)
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

        BunchOfEventsDTO bunchOfEventsDTO = given(anAuthorizedRequest())
                .header("Content-Type", CONTENT_TYPE_EVENT_LIST)
                .when().get("/events?status={id}", EventStatus.NEW.name())
                .as(BunchOfEventsDTO.class);

        assertThat(bunchOfEventsDTO.getEvents()).hasSize(5);
        assertThat(bunchOfEventsDTO.getEvents().get(0).getDeliveryStatus()).isEqualTo(EventStatus.NEW.name());

        final EventUpdateDTO updateEventDTO = new EventUpdateDTO();
        updateEventDTO.setEventId(bunchOfEventsDTO.getEvents().get(0).getEventId());
        updateEventDTO.setDeliveryStatus(EventStatus.SENT.name());

        final BunchOfEventUpdatesDTO updates = new BunchOfEventUpdatesDTO();
        updates.setEvents(newArrayList(updateEventDTO));

        given(anAuthorizedRequest())
                .header("Content-Type", CONTENT_TYPE_EVENT_LIST_UPDATE)
                .body(updates)
                .when().patch("/events")
                .then().assertThat()
                .statusCode(200);

        given(anAuthorizedRequest())
                .header("Content-Type", CONTENT_TYPE_EVENT_LIST)
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

        //J-
        given(anAuthenticatedRequestWithScopes(EVENT_LOG_WRITE_SCOPE))
                .when().post("/events/snapshots")
                .then().assertThat().statusCode(201);

        final BunchOfEventsDTO bunchOfEventsDTO = given(anAuthorizedRequest())
                .header("Content-Type", CONTENT_TYPE_EVENT_LIST)
                .when().get("/events?status={id}", EventStatus.NEW.name())
                .then().assertThat().statusCode(200)
                .extract().as(BunchOfEventsDTO.class);
        //J+

        assertThat(bunchOfEventsDTO).isNotNull();
        assertThat(bunchOfEventsDTO.getLinks()).isNotNull();
        assertThat(bunchOfEventsDTO.getEvents()).isNotEmpty();

        bunchOfEventsDTO.getEvents().stream().forEach(event -> {
            assertThat(event.getEventPayload()).isNotNull();

            final HashMap<String, Object> eventPayload = (HashMap<String, Object>) event.getEventPayload();
            assertThat(eventPayload.get("data_op")).isEqualTo(EventDataOperation.SNAPSHOT.toString());
        });
    }
}
