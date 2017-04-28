package org.zalando.nakadiproducer.persistence.repository;

import com.google.common.collect.Lists;

import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;
import org.zalando.nakadiproducer.persistence.entity.EventDataOperation;
import org.zalando.nakadiproducer.persistence.entity.EventLog;
import org.zalando.nakadiproducer.persistence.entity.EventStatus;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import javax.transaction.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Transactional
public class EventLogRepositoryIT extends BaseMockedExternalCommunicationIT {

    @Autowired
    private EventLogRepository eventLogRepository;

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
    private final String WAREHOUSE_DATA_TYPE = "wholesale:warehouse";

    private Integer id;

    @Before
    public void setUp() throws Exception {
        eventLogRepository.deleteAll();

        final EventLog eventLog = EventLog.builder().eventBodyData(WAREHOUSE_EVENT_BODY_DATA)
                                                                     .eventType(WAREHOUSE_EVENT_TYPE)
                                                                     .dataType(WAREHOUSE_DATA_TYPE)
                                                                     .dataOp(EventDataOperation.CREATE.toString())
                                                                     .status(EventStatus.NEW.toString())
                                                                     .flowId("FLOW_ID").errorCount(0).build();
        eventLogRepository.saveAndFlush(eventLog);
        id = eventLog.getId();
    }

    @Test
    public void findEventRepositoryId() {
        final EventLog eventLog = eventLogRepository.findOne(id);
        compareWithPersistedEvent(eventLog);
    }

    @Test
    public void findByEventRepositoryIdIn() {
        List<EventLog> result = eventLogRepository.findByIdIn(Lists.newArrayList(id));
        final EventLog eventLog = eventLogRepository.findOne(id);

        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(eventLog));
    }

    private void compareWithPersistedEvent(final EventLog eventLog) {
        assertThat(eventLog.getEventBodyData(), is(WAREHOUSE_EVENT_BODY_DATA));
        assertThat(eventLog.getErrorCount(), is(0));
        assertThat(eventLog.getEventType(), is(WAREHOUSE_EVENT_TYPE));
        assertThat(eventLog.getDataType(), is(WAREHOUSE_DATA_TYPE));
        assertThat(eventLog.getDataOp(), is(EventDataOperation.CREATE.toString()));
        assertThat(eventLog.getStatus(), is(EventStatus.NEW.toString()));
    }

}
