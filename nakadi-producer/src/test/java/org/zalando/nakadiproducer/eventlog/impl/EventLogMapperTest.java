package org.zalando.nakadiproducer.eventlog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.nakadiproducer.eventlog.EidGeneratorStrategy;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.util.Fixture;

@ExtendWith(MockitoExtension.class)
public class EventLogMapperTest {

  private static final String TRACE_ID = "TRACE_ID";
  private static final UUID EID = UUID.fromString("558a8fe5-330e-4d89-ae6c-d58432b2dde0");
  private static final String EVENT_TYPE = "eventType";
  private static final String COMPACTION_KEY = "compactionKey";

  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();
  @Mock
  private FlowIdComponent flowIdComponent;
  @Mock
  private EidGeneratorStrategy eidGeneratorStrategy;

  @InjectMocks
  private EventLogMapper eventLogMapper;

  @Test
  public void testCreateEventLog() throws Exception {
    // given
    when(flowIdComponent.getXFlowIdValue()).thenReturn(TRACE_ID);
    when(eidGeneratorStrategy.generateEid()).thenReturn(EID);

    Object eventPayload = Fixture.mockPayload(42, "bla");

    // when
    EventLog actual = eventLogMapper.createEventLog(EVENT_TYPE, eventPayload, COMPACTION_KEY);
    EventLog expected = getEventLog(eventPayload);
    // then
    assertThat(actual, Matchers.equalTo(expected));
    verify(flowIdComponent).getXFlowIdValue();
    verify(eidGeneratorStrategy).generateEid();
  }

  private EventLog getEventLog(Object eventPayload) throws JsonProcessingException {
    return EventLog.builder()
        .eventType(EVENT_TYPE)
        .eventBodyData(objectMapper.writeValueAsString(eventPayload))
        .flowId(TRACE_ID)
        .compactionKey(COMPACTION_KEY)
        .eid(EID)
        .build();
  }
}
