package org.zalando.tarbelaproducer.service;

import org.zalando.tarbelaproducer.persistance.entity.EventDataOperation;
import org.zalando.tarbelaproducer.persistance.entity.EventLog;
import org.zalando.tarbelaproducer.persistance.repository.EventLogRepository;
import org.zalando.tarbelaproducer.service.model.EventPayload;

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

    @Override
    @Transactional
    public void fireDeleteEvent(EventPayload payload, String flowId) {
        final EventLog eventLog = eventLogMapper.createEventLog(EventDataOperation.DELETE, payload, flowId);
        eventLogRepository.save(eventLog);
    }

}
