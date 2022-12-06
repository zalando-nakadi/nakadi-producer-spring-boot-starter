package org.zalando.nakadiproducer;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.transmission.MockNakadiPublishingClient;
import org.zalando.nakadiproducer.transmission.impl.EventTransmitter;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

public class EndToEndTestIT extends BaseMockedExternalCommunicationIT {
    private static final String MY_DATA_CHANGE_EVENT_TYPE = "myDataChangeEventType";
    private static final String MY_BUSINESS_EVENT_TYPE = "myBusinessEventType";
    public static final String PUBLISHER_DATA_TYPE = "nakadi:some-publisher";
    private static final String CODE = "code123";

    @Autowired
    private EventLogWriter eventLogWriter;

    @Autowired
    private EventTransmitter eventTransmitter;

    @Autowired
    private MockNakadiPublishingClient nakadiClient;

    @BeforeEach
    @AfterEach
    public void clearNakadiEvents() {
        eventTransmitter.sendEvents();
        nakadiClient.clearSentEvents();
    }

    @Test
    public void dataEventsShouldBeSubmittedToNakadi() throws IOException {
        MockPayload payload = Fixture.mockPayload(1, CODE);
        eventLogWriter.fireCreateEvent(MY_DATA_CHANGE_EVENT_TYPE, PUBLISHER_DATA_TYPE, payload);

        eventTransmitter.sendEvents();
        List<String> value = nakadiClient.getSentEvents(MY_DATA_CHANGE_EVENT_TYPE);

        assertThat(value.size(), is(1));
        assertThat(read(value.get(0), "$.data_op"), is("C"));
        assertThat(read(value.get(0), "$.data_type"), is(PUBLISHER_DATA_TYPE));
        assertThat(read(value.get(0), "$.data.code"), is(CODE));
    }

    @Test
    public void businessEventsShouldBeSubmittedToNakadi() throws IOException {
        MockPayload payload = Fixture.mockPayload(1, CODE);
        eventLogWriter.fireBusinessEvent(MY_BUSINESS_EVENT_TYPE, payload);

        eventTransmitter.sendEvents();
        List<String> value = nakadiClient.getSentEvents(MY_BUSINESS_EVENT_TYPE);

        assertThat(value.size(), is(1));
        assertThat(read(value.get(0), "$.id"), is(payload.getId()));
        assertThat(read(value.get(0), "$.code"), is(payload.getCode()));
        assertThat(read(value.get(0), "$.items.length()"), is(3));
        assertThat(read(value.get(0), "$.items[0].detail"), is(payload.getItems().get(0).getDetail()));
        assertThat(read(value.get(0), "$.items[1].detail"), is(payload.getItems().get(1).getDetail()));
        assertThat(read(value.get(0), "$.items[2].detail"), is(payload.getItems().get(2).getDetail()));
        assertThat(read(value.get(0), "$.more.info"), is(payload.getMore().getInfo()));
        assertThat(read(value.get(0), "$[?]", where("data_op").exists(true)), is(empty()));
        assertThat(read(value.get(0), "$[?]", where("data_type").exists(true)), is(empty()));
        assertThat(read(value.get(0), "$[?]", where("data").exists(true)), is(empty()));
    }
}
