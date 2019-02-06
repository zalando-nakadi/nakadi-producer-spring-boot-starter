package org.zalando.nakadiproducer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.eventlog.impl.EventLog;
import org.zalando.nakadiproducer.transmission.MockNakadiPublishingClient;
import org.zalando.nakadiproducer.transmission.impl.EventTransmissionService;
import org.zalando.nakadiproducer.transmission.impl.EventTransmitter;
import org.zalando.nakadiproducer.util.Fixture;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LockingIT extends BaseMockedExternalCommunicationIT {
    private static final String MY_EVENT_TYPE = "myEventType";

    @Autowired
    private EventLogWriter eventLogWriter;

    @Autowired
    private EventTransmitter eventTransmitter;

    @Autowired
    private EventTransmissionService eventTransmissionService;

    @Autowired
    private MockNakadiPublishingClient nakadiClient;

    @Before
    @After
    public void clearNakadiEvents() {
        eventTransmitter.sendEvents();
        nakadiClient.clearSentEvents();
    }

    @Test
    public void eventsShouldNotBeSentTwiceWhenLockExpiresDuringTransmission() {
        // Given that there is an event to be sent...
        eventLogWriter.fireBusinessEvent(MY_EVENT_TYPE, Fixture.mockPayload(1, "code123"));

        // ... and given one job instance locked it for sending...
        Instant timeOfInitialLock = Instant.now();
        mockServiceClock(timeOfInitialLock);
        Collection<EventLog> eventLogsLockedFirst = eventTransmissionService.lockSomeEvents();

        // ... and given that so much time passed in the meantime that the lock already expired...
        mockServiceClock(timeOfInitialLock.plus(11, MINUTES));

        // ... so that another job could have locked the same events ...
        Collection<EventLog> eventLogsLockedSecond = eventTransmissionService.lockSomeEvents();

        // when both job instances try to send their locked events
        eventTransmissionService.sendEvents(eventLogsLockedFirst);
        eventTransmissionService.sendEvents(eventLogsLockedSecond);

        // Then the event should have been sent only once.
        List<String> value = nakadiClient.getSentEvents(MY_EVENT_TYPE);

        assertThat(value.size(), is(1));
    }

    private void mockServiceClock(Instant ins) {
        eventTransmissionService.overrideClock(Clock.fixed(ins, ZoneId.systemDefault()));
    }
}
