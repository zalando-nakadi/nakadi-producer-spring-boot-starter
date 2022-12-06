package org.zalando.nakadiproducer.snapshots.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_DATA_TYPE;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.snapshots.Snapshot;
import org.zalando.nakadiproducer.snapshots.SnapshotEventGenerator;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

@ExtendWith(MockitoExtension.class)
public class SnapshotCreationServiceTest {

    @Mock
    private SnapshotEventGenerator snapshotEventGenerator;

    @Mock
    private EventLogWriter eventLogWriter;

    private SnapshotCreationService snapshotCreationService;

    @Captor
    private ArgumentCaptor<Collection<?>> eventLogDataCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        when(snapshotEventGenerator.getSupportedEventType()).thenReturn(PUBLISHER_EVENT_TYPE);
        snapshotCreationService = new SnapshotCreationService(asList(snapshotEventGenerator), eventLogWriter);
    }

    @Test
    public void testCreateSnapshotEvents() {
        final String filter = "exampleFilter";

        MockPayload eventPayload = Fixture.mockPayload(1, "mockedcode", true,
                Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        when(snapshotEventGenerator.generateSnapshots(null, filter)).thenReturn(
                singletonList(new Snapshot(1, PUBLISHER_DATA_TYPE, eventPayload)));

        snapshotCreationService.createSnapshotEvents(PUBLISHER_EVENT_TYPE, filter);

        verify(eventLogWriter).fireSnapshotEvents(eq(PUBLISHER_EVENT_TYPE), eq(PUBLISHER_DATA_TYPE),
                eventLogDataCaptor.capture());
        assertThat(eventLogDataCaptor.getValue(), contains(eventPayload));
    }

    @Test
    public void testSnapshotSavedInBatches() {
        final String filter = "exampleFilter2";

        final List<Snapshot> eventSnapshots = Fixture.mockSnapshotList(5);

        // when snapshot returns 5 item stream
        when(snapshotEventGenerator.generateSnapshots(null, filter)).thenReturn(eventSnapshots.subList(0, 3));
        when(snapshotEventGenerator.generateSnapshots(2, filter)).thenReturn(eventSnapshots.subList(3, 5));
        when(snapshotEventGenerator.generateSnapshots(4, filter)).thenReturn(emptyList());

        // create a snapshot
        snapshotCreationService.createSnapshotEvents(PUBLISHER_EVENT_TYPE, filter);

        // verify that all returned events got written
        verify(eventLogWriter, times(2)).fireSnapshotEvents(eq(PUBLISHER_EVENT_TYPE), eq(PUBLISHER_DATA_TYPE),
                eventLogDataCaptor.capture());

        List<?> payloads = eventSnapshots.stream().map(Snapshot::getData).collect(toList());
        List<?> writtenEvents = eventLogDataCaptor
                .getAllValues()
                .stream()
                .flatMap(Collection::stream)
                .collect(toList());
        assertThat(writtenEvents, is(equalTo(payloads)));
    }
}
