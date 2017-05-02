package org.zalando.nakadiproducer.snapshots.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;

import java.util.Collections;
import java.util.stream.Stream;

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
import org.zalando.nakadiproducer.FlowIdComponent;
import org.zalando.nakadiproducer.NakadiProperties;
import org.zalando.nakadiproducer.eventlog.EventPayload;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.eventlog.impl.EventLogWriterImpl;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotCreationServiceTest {
    private static final int SNAPSHOT_BATCH_SIZE = 5;

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private FlowIdComponent flowIdComponent;

    @Mock
    private NakadiProperties nakadiProperties;

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

    //J-
    private static final String EVENT_BODY_DATA =
            ("{'id':1,"
            + "'code':'mockedcode',"
            + "'more':{'info':'some info'},"
            + "'items':[{'detail':'some detail0'},{'detail':'some detail1'}],"
            + "'active':true"
            + "}").replace('\'', '"');
    //J+

    private String traceId;

    @Before
    public void setUp() throws Exception {

        when(nakadiProperties.getSnapshotBatchSize()).thenReturn(SNAPSHOT_BATCH_SIZE);

        MockPayload mockPayload = Fixture.mockPayload(1, "mockedcode", true,
            Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));

        eventPayload = Fixture.mockEventPayload(mockPayload);

        traceId = "TRACE_ID";
        when(flowIdComponent.getXFlowIdValue()).thenReturn(traceId);
    }

    @Test
    public void testCreateSnapshotEvents() {
        when(snapshotEventProvider.getSnapshot(PUBLISHER_EVENT_TYPE)).thenReturn(Collections.singletonList(eventPayload).stream());

        eventTransmissionService.createSnapshotEvents(PUBLISHER_EVENT_TYPE, traceId);

        verify(eventLogWriter).fireSnapshotEvent(listEventLogCaptor.capture(), eq(traceId));
        assertThat(listEventLogCaptor.getValue(), is(eventPayload));
    }

    @Test
    public void testSnapshotSavedInBatches() {

        final Stream<EventPayload> eventPayloadsStream = Fixture.mockPayloadList(5).stream()
                                                                .map(Fixture::mockEventPayload);

        // when snapshot returns 5 item stream
        when(snapshotEventProvider.getSnapshot(PUBLISHER_EVENT_TYPE)).thenReturn(eventPayloadsStream);
        // and the size of a batch is 3
        when(nakadiProperties.getSnapshotBatchSize()).thenReturn(3);

        // create a snapshot
        eventTransmissionService.createSnapshotEvents(PUBLISHER_EVENT_TYPE, traceId);

        // verify that three events got written
        verify(eventLogWriter, times(5)).fireSnapshotEvent(listEventLogCaptor.capture(), eq(traceId));
    }

}
