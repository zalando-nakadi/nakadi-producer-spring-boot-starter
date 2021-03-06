package org.zalando.nakadiproducer.eventlog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.transaction.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;

@Transactional
public class EventLogRepositoryIT extends BaseMockedExternalCommunicationIT {

    @Autowired
    private EventLogRepositoryImpl eventLogRepository;

    private static final String WAREHOUSE_EVENT_BODY_DATA =
            ("{'self':'http://WAREHOUSE_DOMAIN',"
            + "'code':'WH-DE-EF',"
            + "'name':'Erfurt',"
            + "'address':{'name':'Zalando Logistics SE & Co.KG',"
                       + "'street':'In der Hochstedter Ecke 1',"
                       + "'city':'Erfurt',"
                       + "'zip':'99098',"
                       + "'country':'DE',"
                       + "'additional':null"
            + "},"
            + "'is_allowed_for_shipping':true,"
            + "'is_allowed_for_purchase_order':true,"
            + "'legacy_warehouse_code':'3'"
            + "}").replace('\'', '"');

    private final String WAREHOUSE_EVENT_TYPE = "wholesale.warehouse-change-event";

    private Integer id;

    @Before
    public void setUp() throws Exception {
        eventLogRepository.deleteAll();

        final EventLog eventLog = EventLog.builder().eventBodyData(WAREHOUSE_EVENT_BODY_DATA)
                                                                     .eventType(WAREHOUSE_EVENT_TYPE)
                                                                     .flowId("FLOW_ID").build();
        eventLogRepository.persist(eventLog);
        id = eventLog.getId();
    }

    @Test
    public void findEventRepositoryId() {
        final EventLog eventLog = eventLogRepository.findOne(id);
        compareWithPersistedEvent(eventLog);
    }

    private void compareWithPersistedEvent(final EventLog eventLog) {
        assertThat(eventLog.getEventBodyData(), is(WAREHOUSE_EVENT_BODY_DATA));
        assertThat(eventLog.getEventType(), is(WAREHOUSE_EVENT_TYPE));
    }

}
