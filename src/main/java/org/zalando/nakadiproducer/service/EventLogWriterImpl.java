package org.zalando.nakadiproducer.service;

import org.zalando.nakadiproducer.persistence.entity.EventDataOperation;
import org.zalando.nakadiproducer.persistence.entity.EventLog;
import org.zalando.nakadiproducer.persistence.repository.EventLogRepository;
import org.zalando.nakadiproducer.service.model.EventPayload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.transaction.Transactional;

@Service
public class EventLogWriterImpl implements EventLogWriter {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private EventLogMapper eventLogMapper;

    @Override
    @Transactional
    public void fireCreateEvent(final EventPayload payload, @Nullable final String flowId) {
        final EventLog eventLog = eventLogMapper.createEventLog(EventDataOperation.CREATE, payload, flowId);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireUpdateEvent(final EventPayload payload, @Nullable final String flowId) {
        final EventLog eventLog = eventLogMapper.createEventLog(EventDataOperation.UPDATE, payload, flowId);
        eventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void fireDeleteEvent(final EventPayload payload, @Nullable final String flowId) {
        final EventLog eventLog = eventLogMapper.createEventLog(EventDataOperation.DELETE, payload, flowId);
        eventLogRepository.save(eventLog);
    }

}
