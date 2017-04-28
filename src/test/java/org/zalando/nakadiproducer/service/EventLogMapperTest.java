package org.zalando.nakadiproducer.service;

import org.mockito.Spy;
import org.zalando.nakadiproducer.persistence.entity.EventDataOperation;
import org.zalando.nakadiproducer.persistence.entity.EventLog;
import org.zalando.nakadiproducer.service.model.EventPayload;
import org.zalando.nakadiproducer.persistence.entity.EventStatus;
import org.zalando.nakadiproducer.util.Fixture;
import org.zalando.nakadiproducer.util.MockPayload;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_DATA_TYPE;
import static org.zalando.nakadiproducer.util.Fixture.PUBLISHER_EVENT_TYPE;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class EventLogMapperTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EventLogMapper eventLogMapper;

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
    }

    @Test
    public void testCreateWarehouseEventLog() throws Exception {
        String eventBody =
                ("{'id':1,"
                + "'code':'mockedcode',"
                + "'more':{'info':'some info'},"
                + "'items':[{'detail':'some detail0'},{'detail':'some detail1'}],"
                + "'active':true"
                + "}").replace('\'', '"');
        MockPayload mockPayload = Fixture.mockPayload(1, "mockedcode", true,
                Fixture.mockSubClass("some info"), Fixture.mockSubList(2, "some detail"));
        EventPayload eventPayload = Fixture.mockEventPayload(mockPayload);
        String traceId = "TRACE_ID";

        final EventLog eventLog = eventLogMapper.createEventLog(
                EventDataOperation.UPDATE, eventPayload, traceId);
        MatcherAssert.assertThat(eventLog.getEventBodyData(), is(eventBody));
        MatcherAssert.assertThat(eventLog.getDataOp(), is(EventDataOperation.UPDATE.toString()));
        MatcherAssert.assertThat(eventLog.getEventType(), is(PUBLISHER_EVENT_TYPE));
        MatcherAssert.assertThat(eventLog.getDataType(), is(PUBLISHER_DATA_TYPE));
        MatcherAssert.assertThat(eventLog.getStatus(), is(EventStatus.NEW.toString()));
        MatcherAssert.assertThat(eventLog.getFlowId(), is(traceId));
    }

}
