package org.zalando.nakadiproducer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
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

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@Transactional
@SpringBootTest(properties = {
        "nakadi-producer.scheduled-transmission-enabled:false",
        "nakadi-producer.lock-duration:300",
        "nakadi-producer.lock-duration-buffer:30"})
public class LockTimeoutIT extends BaseMockedExternalCommunicationIT {
    private static final String MY_EVENT_TYPE = "myEventType";

    @Autowired
    private EventLogWriter eventLogWriter;

    @Autowired
    private EventTransmitter eventTransmitter;

    @Autowired
    private EventTransmissionService eventTransmissionService;

    @Autowired
    private MockNakadiPublishingClient nakadiClient;

    @BeforeEach
    @AfterEach
    public void clearNakadiEvents() {
        mockServiceClock(Instant.now());
        eventTransmitter.sendEvents();
        nakadiClient.clearSentEvents();
    }

    @Test
    public void testLockedUntil() {
        eventLogWriter.fireBusinessEvent(MY_EVENT_TYPE, Fixture.mockPayload(1, "code123"));

        Instant timeOfInitialLock = Instant.now();
        mockServiceClock(timeOfInitialLock);

        assertThat(eventTransmissionService.lockSomeEvents().size(), is(1));
        assertThat(eventTransmissionService.lockSomeEvents(), empty());

        // lock is still valid
        mockServiceClock(timeOfInitialLock.plus(300 - 5, SECONDS));
        assertThat(eventTransmissionService.lockSomeEvents(), empty());

        // lock is expired
        mockServiceClock(timeOfInitialLock.plus(300 + 5, SECONDS));
        assertThat(eventTransmissionService.lockSomeEvents().size(), is(1));
    }

    @Test
    public void testLockNearlyExpired() {
        eventLogWriter.fireBusinessEvent(MY_EVENT_TYPE, Fixture.mockPayload(1, "code456"));
        Instant timeOfInitialLock = Instant.now();

        Collection<EventLog> lockedEvent = eventTransmissionService.lockSomeEvents();

        // event will not be sent, because the event-lock is "nearlyExpired"
        mockServiceClock(timeOfInitialLock.plus(300 - 30 + 5, SECONDS));
        eventTransmissionService.sendEvents(lockedEvent);
        assertThat(nakadiClient.getSentEvents(MY_EVENT_TYPE), empty());
    }

    private void mockServiceClock(Instant ins) {
        eventTransmissionService.overrideClock(Clock.fixed(ins, ZoneId.systemDefault()));
    }
}