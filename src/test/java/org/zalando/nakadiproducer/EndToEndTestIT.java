package org.zalando.nakadiproducer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.fahrschein.NakadiClient;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.eventlog.DataChangeEventPayload;
import org.zalando.nakadiproducer.transmission.impl.EventTransmitter;
import org.zalando.nakadiproducer.transmission.impl.NakadiBusinessEvent;
import org.zalando.nakadiproducer.transmission.impl.NakadiDataChangeEvent;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

public class EndToEndTestIT extends BaseMockedExternalCommunicationIT {
    private static final String MY_DATA_CHANGE_EVENT_TYPE = "myDataChangeEventType";
    private static final String MY_BUSINESS_EVENT_TYPE = "myBusinessEventType";
    private static final String CODE = "code123";

    @Autowired
    private EventLogWriter eventLogWriter;

    @Autowired
    private EventTransmitter eventTransmitter;

    @Autowired
    private NakadiClient nakadiClient;

    @Captor
    private ArgumentCaptor<List<NakadiDataChangeEvent>> captor;

    @Captor
    private ArgumentCaptor<List<NakadiBusinessEvent>> businessEventCaptor;

    @Test
    public void dataEventsShouldBeSubmittedToNakadi() throws IOException {
        MockPayload code = Fixture.mockPayload(1, CODE);
        DataChangeEventPayload payload = Fixture.mockEventPayload(code);
        eventLogWriter.fireCreateEvent(MY_DATA_CHANGE_EVENT_TYPE, payload);

        eventTransmitter.sendEvents();

        verify(nakadiClient).publish(eq(MY_DATA_CHANGE_EVENT_TYPE), captor.capture());
        List<NakadiDataChangeEvent> value = captor.getValue();

        assertThat(value.size(), is(1));
        assertThat(value.get(0).getDataOperation(), is("C"));
        assertThat(value.get(0).getDataType(), is(payload.getDataType()));
        Map<String, Object> data = (Map<String, Object>) value.get(0).getData();
        assertThat(data.get("code"), is(CODE));
    }

    @Test
    public void businessEventsShouldBeSubmittedToNakadi() throws IOException {
        MockPayload payload = Fixture.mockPayload(1, CODE);
        eventLogWriter.fireBusinessEvent(MY_BUSINESS_EVENT_TYPE, payload);

        eventTransmitter.sendEvents();

        verify(nakadiClient).publish(eq(MY_BUSINESS_EVENT_TYPE), businessEventCaptor.capture());
        List<NakadiBusinessEvent> value = businessEventCaptor.getValue();

        assertThat(value.size(), is(1));
        assertThat(value.get(0).any().get("data_op"), is(nullValue()));
        assertThat(value.get(0).any().get("data_type"), is(nullValue()));
        assertThat(value.get(0).any().get("data"), is(nullValue()));
        assertThat(value.get(0).any().get("id"), is(payload.getId()));
        assertThat(value.get(0).any().get("code"), is(payload.getCode()));
        List<Map<String, Object>> items = (List<Map<String, Object>>) value.get(0).any().get("items");
        assertThat(items, is(not(nullValue())));
        assertThat(items.get(0).get("detail"), is(payload.getItems().get(0).getDetail()));
        Map<String, Object> subclass = (Map<String, Object>) value.get(0).any().get("more");
        assertThat(subclass.get("info"), is(payload.getMore().getInfo()));
    }
}
