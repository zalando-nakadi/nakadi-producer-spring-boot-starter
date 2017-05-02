package org.zalando.nakadiproducer.snapshots.impl;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_DATA_TYPE;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;
import org.zalando.nakadiproducer.TestApplication;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.eventlog.EventPayload;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CreateSnapshotControllerIT extends BaseMockedExternalCommunicationIT {

    private static final String SOME_DETAIL = "some detail";

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
            MockPayload mockPayload = Fixture.mockPayload(i, mockedCode+i, true,
                    Fixture.mockSubClass("some info"+i), Fixture.mockSubList(2, SOME_DETAIL +i));
            mockPayloads.add(mockPayload);
            EventPayload eventPayload = Fixture.mockEventPayload(mockPayload);
            eventLogWriter.fireCreateEvent(eventPayload, "SOME_FLOW_ID");
        }

        // make listOfItems return predefined list
        TestApplication.setList(mockPayloads);

    }

    @Test
    public void snapshotEventsCanBeCreatedAndRetrieved() {
        eventLogRepository.deleteAll();

        // Create snapshot
        given(aHttpsRequest())
                .when().post("/events/snapshots/" + PUBLISHER_EVENT_TYPE)
                .then().assertThat().statusCode(201);

        List<EventLog> all = eventLogRepository.findAll();
        assertThat(all.size(), is(5));
        all.forEach(event -> {
            assertThat(event.getDataOp(), is("S"));
            assertThat(event.getDataType(), is(PUBLISHER_DATA_TYPE));
            assertThat(event.getEventType(), is(PUBLISHER_EVENT_TYPE));
            String eventBodyData = event.getEventBodyData();
            MockPayload mockPayload;
            try {
                mockPayload = new ObjectMapper().readValue(eventBodyData, MockPayload.class);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            int id = mockPayload.getId();
            assertThat(mockPayload.getCode(), is(mockedCode + id));
            assertThat(mockPayload.getItems().size(), is(2));
            assertThat(mockPayload.getMore().getInfo(), is("some info" + id));

        });
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
                .contentType(containsString("application/problem+json"))
                .body(
                    "type", is("http://httpstatus.es/422"),
                    "status", is(422),
                    "title", is("No event log found"),
                    "detail", is("No event log found for event type ("+unknownEventType+")."),
                    "instance", startsWith("X-Flow-ID")
                );

    }

}
