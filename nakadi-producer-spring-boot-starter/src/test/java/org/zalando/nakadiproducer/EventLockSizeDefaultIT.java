package org.zalando.nakadiproducer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.transmission.impl.EventTransmissionService;
import org.zalando.nakadiproducer.util.Fixture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class EventLockSizeDefaultIT extends BaseMockedExternalCommunicationIT {

    @Autowired
    private EventLogWriter eventLogWriter;

    @Autowired
    private EventTransmissionService eventTransmissionService;

    @Test
    public void smallNumberOfEventsAreAllLockedTogether() {

        for (int i = 1; i <= 8; i++) {
            eventLogWriter.fireBusinessEvent("myEventType", Fixture.mockPayload(i, "code123"));
        }

        assertThat(eventTransmissionService.lockSomeEvents(), hasSize(8));
    }

    @Test
    public void moreThan1000EventsAreNotLockedTogether() {

        for (int i = 1; i <= 1010; i++) {
            eventLogWriter.fireBusinessEvent("myEventType", Fixture.mockPayload(i, "code123"));
        }

        assertThat(eventTransmissionService.lockSomeEvents(), hasSize(1000));
        assertThat(eventTransmissionService.lockSomeEvents(), hasSize(10));
    }
}
