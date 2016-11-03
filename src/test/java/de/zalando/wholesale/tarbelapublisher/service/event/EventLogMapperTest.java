package de.zalando.wholesale.tarbelapublisher.service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.zalando.wholesale.tarbelapublisher.api.event.model.BunchOfEventsDTO;
import de.zalando.wholesale.tarbelapublisher.api.event.model.EventDTO;
import de.zalando.wholesale.tarbelapublisher.persistance.entity.event.EventDataOperation;
import de.zalando.wholesale.tarbelapublisher.persistance.entity.event.EventLog;
import de.zalando.wholesale.tarbelapublisher.persistance.entity.event.EventStatus;
import de.zalando.wholesale.tarbelapublisher.service.event.model.NakadiEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import static com.google.common.collect.Lists.newArrayList;
import static de.zalando.wholesale.tarbelapublisher.util.Fixture.PUBLISHER_DATA_TYPE;
import static de.zalando.wholesale.tarbelapublisher.util.Fixture.PUBLISHER_EVENT_TYPE;
import static de.zalando.wholesale.tarbelapublisher.util.Fixture.SINK_ID;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventLogMapperTest {

    public static final int LIMIT = 5;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EventLogMapper eventLogMapper;

    private static final String EVENT_BODY_DATA =
        ("{'code':'WH-DE-EF','name':'Erfurt','legacy_warehouse_code':'3'}").replace('\'', '"');
    private EventLog eventLog1;
    private EventLog eventLog2;

    @Before
    public void setUp() {

        String localHost = "http://localhost";
        HttpServletRequest httpServletRequestMock = mock(HttpServletRequest.class);
        when(httpServletRequestMock.getRequestURL()).thenReturn(new StringBuffer(localHost));
        when(httpServletRequestMock.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(httpServletRequestMock.getRequestURI()).thenReturn(localHost);
        when(httpServletRequestMock.getContextPath()).thenReturn("");
        when(httpServletRequestMock.getServletPath()).thenReturn("");

        ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(httpServletRequestMock);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);

        eventLog1 = EventLog.builder().eventBodyData(EVENT_BODY_DATA).id(3)
                                     .eventType(PUBLISHER_EVENT_TYPE)
                                     .dataType(PUBLISHER_DATA_TYPE)
                                     .created(Instant.now()).dataOp(EventDataOperation.CREATE.toString())
                                     .status(EventStatus.NEW.toString()).flowId("FLOW_ID_1").errorCount(0).build();

        eventLog2 = EventLog.builder().eventBodyData(EVENT_BODY_DATA).id(5)
                                     .eventType(PUBLISHER_EVENT_TYPE)
                                     .created(Instant.now())
                                     .dataType(PUBLISHER_DATA_TYPE)
                                     .dataOp(EventDataOperation.CREATE.toString()).status(EventStatus.ERROR.toString())
                                     .flowId("FLOW_ID_2").errorCount(0).build();
    }

    @Test
    public void testMapToDTO() throws JsonProcessingException {
        final BunchOfEventsDTO result = eventLogMapper.mapToDTO(newArrayList(eventLog1, eventLog2),
                EventStatus.NEW.name(), LIMIT, PUBLISHER_EVENT_TYPE, SINK_ID);

        final String nextLink = result.getLinks().getNext().getHref();
        assertThat(nextLink).contains(String.valueOf(LIMIT));
        assertThat(nextLink).contains(EventStatus.NEW.name());
        assertThat(nextLink).contains(String.valueOf(eventLog2.getId()));

        assertThat(result.getEvents()).hasSize(2);
        compareEventDTOWithEvent(result.getEvents().get(0), eventLog1);
        compareEventDTOWithEvent(result.getEvents().get(1), eventLog2);
    }

    private void compareEventDTOWithEvent(final EventDTO eventDTO, final EventLog eventLog)
        throws JsonProcessingException {
        assertThat(eventDTO.getEventId()).isEqualTo(String.valueOf(eventLog.getId()));
        assertThat(eventDTO.getDeliveryStatus()).isEqualTo(eventLog.getStatus());

        assertThat(eventDTO.getChannel().getSinkIdentifier()).isEqualTo(SINK_ID);
        assertThat(eventDTO.getChannel().getTopicName()).isEqualTo(PUBLISHER_EVENT_TYPE);

        final NakadiEvent nakadiEvent = (NakadiEvent) eventDTO.getEventPayload();

        assertThat(nakadiEvent.getDataOperation()).isEqualTo(eventLog.getDataOp());
        assertThat(nakadiEvent.getDataType()).isEqualTo(eventLog.getDataType());
        assertThat(objectMapper.writeValueAsString(nakadiEvent.getData())).isEqualTo(eventLog.getEventBodyData());

        assertThat(nakadiEvent.getMetadata().getOccuredAt()).isEqualTo(eventLog.getCreated());
        assertThat(nakadiEvent.getMetadata().getEid()).endsWith(String.valueOf(eventLog.getId()));
    }

    @Test
    public void testMapToDTOWithEmptyList() {
        final BunchOfEventsDTO result = eventLogMapper.mapToDTO(emptyList(), EventStatus.NEW.toString(),
                LIMIT, PUBLISHER_EVENT_TYPE, SINK_ID);
        assertThat(result.getLinks()).isNull();
        assertThat(result.getEvents().isEmpty()).isTrue();
    }
}
