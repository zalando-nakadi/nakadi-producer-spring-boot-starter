package org.zalando.nakadiproducer.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.fahrschein.NakadiClient;
import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;
import org.zalando.nakadiproducer.util.Fixture;

public class EventLogServiceImplIT extends BaseMockedExternalCommunicationIT {
    private static final String MY_EVENT_TYPE = "myEventType";
    private static final String FLOW_ID = "flowId1";

    @Autowired
    private EventLogWriter eventLogWriter;

    @Autowired
    private EventLogServiceImpl eventLogService;

    @Autowired
    private NakadiClient nakadiClient;

    @Test
    public void eventsShouldBeSubmittedToNakadi() {
        eventLogWriter.fireCreateEvent(Fixture.mockEventPayload(Fixture.mockPayload(1, "code"), MY_EVENT_TYPE), FLOW_ID);

        eventLogService.sendMessages();

    }
}
