package org.zalando.nakadiproducer.service;

import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.service.model.EventPayload;
import org.zalando.nakadiproducer.web.FlowIdComponent;
import org.zalando.nakadiproducer.NakadiProperties;
import org.zalando.nakadiproducer.persistence.entity.EventDataOperation;
import org.zalando.nakadiproducer.persistence.entity.EventLog;
import org.zalando.nakadiproducer.persistence.repository.EventLogRepository;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_DATA_TYPE;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventLogServiceImplTest {
    private static final int SNAPSHOT_BATCH_SIZE = 5;

    @Mock
    private EventLogRepository eventLogRepository;

    @Mock
    private EventLogMapper eventLogMapper;

    @Mock
    private FlowIdComponent flowIdComponent;

    @Mock
    private NakadiProperties nakadiProperties;

    @Mock
    private SnapshotEventProvider snapshotEventProvider;

    @InjectMocks
    private EventLogServiceImpl eventLogService;

    private EventPayload eventPayload;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<List<EventLog>> listEventLogCaptor;

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

        final List<EventPayload> eventPayloads = Collections.singletonList(eventPayload);

        EventLog eventLog = EventLog.builder().id(123).eventBodyData(EVENT_BODY_DATA)
                .eventType(PUBLISHER_EVENT_TYPE)
                .dataType(PUBLISHER_DATA_TYPE)
                .dataOp(EventDataOperation.SNAPSHOT.toString())
                .flowId("FLOW_ID")
                .build();

        when(snapshotEventProvider.getSnapshot(PUBLISHER_EVENT_TYPE)).thenReturn(eventPayloads.stream());
        when(eventLogMapper.createEventLog(any(), any(), any())).thenReturn(eventLog);

        eventLogService.createSnapshotEvents(PUBLISHER_EVENT_TYPE, traceId);

        verify(eventLogRepository).save(listEventLogCaptor.capture());

        assertThat(listEventLogCaptor.getValue().size(), is(1));

        final EventLog snapshotEventLog = listEventLogCaptor.getValue().get(0);

        assertThat(snapshotEventLog.getDataOp(), is(EventDataOperation.SNAPSHOT.toString()));
        assertThat(snapshotEventLog.getEventBodyData(), is(EVENT_BODY_DATA));
    }

    @Test
    public void testSnapshotSavedInBatches() {

        final List<MockPayload> mockPayloadList = Fixture.mockPayloadList(5);

        final Stream<EventPayload> eventPayloadsStream = mockPayloadList.stream()
                .map(Fixture::mockEventPayload);

        // when snapshot returns 5 item stream
        when(snapshotEventProvider.getSnapshot(PUBLISHER_EVENT_TYPE)).thenReturn(eventPayloadsStream);
        // and the size of a batch is 3
        when(nakadiProperties.getSnapshotBatchSize()).thenReturn(3);

        // create a snapshot
        eventLogService.createSnapshotEvents(PUBLISHER_EVENT_TYPE, traceId);

        // verify that that save got called twice
        verify(eventLogRepository, times(2)).save(listEventLogCaptor.capture());

        // verify that stream was split in two batches
        assertThat(listEventLogCaptor.getAllValues().size(), is(2));
        // verify that size of the first batch is 3
        assertThat(listEventLogCaptor.getAllValues().get(0).size(), is(3));
        // verify that size of the last batch is 2
        assertThat(listEventLogCaptor.getAllValues().get(1).size(), is(2));

    }

}
