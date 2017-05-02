package org.zalando.nakadiproducer.snapshots.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.zalando.nakadiproducer.eventlog.EventPayload;
import org.zalando.nakadiproducer.eventlog.impl.EventLogWriterImpl;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider.Snapshot;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotCreationServiceTest {

    @Mock
    private SnapshotEventProvider snapshotEventProvider;

    @Mock
    private EventLogWriterImpl eventLogWriter;

    @InjectMocks
    private SnapshotCreationService eventTransmissionService;

    private EventPayload eventPayload;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<EventPayload> listEventLogCaptor;

    @Before
    public void setUp() throws Exception {
        MockPayload mockPayload = Fixture.mockPayload(1, "mockedcode", true,
            Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        eventPayload = Fixture.mockEventPayload(mockPayload);
    }

    @Test
    public void testCreateSnapshotEvents() {
        when(snapshotEventProvider.getSnapshot(PUBLISHER_EVENT_TYPE, null)).thenReturn(Collections.singletonList(new Snapshot(1, eventPayload)));

        eventTransmissionService.createSnapshotEvents(PUBLISHER_EVENT_TYPE);

        verify(eventLogWriter).fireSnapshotEvent(listEventLogCaptor.capture());
        assertThat(listEventLogCaptor.getValue(), is(eventPayload));
    }

    @Test
    public void testSnapshotSavedInBatches() {

        final List<Snapshot> eventPayloads = Fixture.mockSnapshotList(5);

        // when snapshot returns 5 item stream
        when(snapshotEventProvider.getSnapshot(PUBLISHER_EVENT_TYPE, null)).thenReturn(eventPayloads.subList(0, 3));
        when(snapshotEventProvider.getSnapshot(PUBLISHER_EVENT_TYPE, 2)).thenReturn(eventPayloads.subList(3, 5));
        when(snapshotEventProvider.getSnapshot(PUBLISHER_EVENT_TYPE, 5)).thenReturn(Collections.emptyList());

        // create a snapshot
        eventTransmissionService.createSnapshotEvents(PUBLISHER_EVENT_TYPE);

        // verify that all returned events got written
        verify(eventLogWriter, times(5)).fireSnapshotEvent(isA(EventPayload.class));
    }

}
