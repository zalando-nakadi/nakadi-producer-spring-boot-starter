package de.zalando.wholesale.tarbelaproducer.service;

import de.zalando.wholesale.tarbelaproducer.persistance.entity.EventDataOperation;
import de.zalando.wholesale.tarbelaproducer.persistance.entity.EventLog;
import de.zalando.wholesale.tarbelaproducer.persistance.repository.EventLogRepository;
import de.zalando.wholesale.tarbelaproducer.service.model.EventPayload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
public class EventLogWriterImpl implements EventLogWriter {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private EventLogMapper eventLogMapper;

    @Override
    @Transactional
    public void fireCreateEvent(final EventPayload payload, final String flowId) {
        final EventLog eventLog = eventLogMapper.createEventLog(EventDataOperation.CREATE, payload, flowId);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireUpdateEvent(final EventPayload payload, final String flowId) {
        final EventLog eventLog = eventLogMapper.createEventLog(EventDataOperation.UPDATE, payload, flowId);
        eventLogRepository.save(eventLog);
    }

}
