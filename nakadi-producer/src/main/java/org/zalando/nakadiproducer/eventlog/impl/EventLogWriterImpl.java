package org.zalando.nakadiproducer.eventlog.impl;

import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.CREATE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.DELETE;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.SNAPSHOT;
import static org.zalando.nakadiproducer.eventlog.impl.EventDataOperation.UPDATE;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;

import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventLogWriterImpl implements EventLogWriter {

    private final EventLogRepository eventLogRepository;

    private final ObjectMapper objectMapper;
    private final FlowIdComponent flowIdComponent;

    public EventLogWriterImpl(EventLogRepository eventLogRepository, ObjectMapper objectMapper, FlowIdComponent flowIdComponent) {
        this.eventLogRepository = eventLogRepository;
        this.objectMapper = objectMapper;
        this.flowIdComponent = flowIdComponent;
    }

    @Override
    @Transactional
    public void fireCreateEvent(final String eventType, final String dataType, final Object data) {
        final EventLog eventLog = createEventLog(eventType, new DataChangeEventEnvelope(CREATE.toString(), dataType, data));
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireCreateEvents(final String eventType, final Map<String, Collection<Object>> dataTypeToData) {
      eventLogRepository.persist(createEventLogs(eventType, CREATE, dataTypeToData));
    }

    @Override
    @Transactional
    public void fireUpdateEvent(final String eventType, final String dataType, final Object data) {
        final EventLog eventLog = createEventLog(eventType, new DataChangeEventEnvelope(UPDATE.toString(), dataType, data));
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireUpdateEvents(final String eventType, final Map<String, Collection<Object>> dataTypeToData) {
      eventLogRepository.persist(createEventLogs(eventType, UPDATE, dataTypeToData));
    }

    @Override
    @Transactional
    public void fireDeleteEvent(final String eventType, final String dataType, final Object data) {
        final EventLog eventLog = createEventLog(eventType, new DataChangeEventEnvelope(DELETE.toString(), dataType, data));
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireDeleteEvents(final String eventType, final Map<String, Collection<Object>> dataTypeToData) {
      eventLogRepository.persist(createEventLogs(eventType, DELETE, dataTypeToData));
    }

    @Override
    @Transactional
    public void fireSnapshotEvent(final String eventType, final String dataType, final Object data) {
        final EventLog eventLog = createEventLog(eventType, new DataChangeEventEnvelope(SNAPSHOT.toString(), dataType, data));
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireSnapshotEvents(final String eventType, final Map<String, Collection<Object>> dataTypeToData) {
      eventLogRepository.persist(createEventLogs(eventType, SNAPSHOT, dataTypeToData));
    }

    @Override
    @Transactional
    public void fireBusinessEvent(final String eventType, Object payload) {
        final EventLog eventLog = createEventLog(eventType, payload);
        eventLogRepository.persist(eventLog);
    }

    @Override
    @Transactional
    public void fireBusinessEvents(final String eventType, final Collection<Object> payload) {
        final Collection<EventLog> eventLogs = createEventLogs(eventType, payload);
        eventLogRepository.persist(eventLogs);
    }

    private Collection<EventLog> createEventLogs(final String eventType,
        final Collection<Object> eventPayloads) {
      return eventPayloads.stream()
          .map(payload -> createEventLog(eventType, payload))
          .collect(Collectors.toList());
    }

    private EventLog createEventLog(final String eventType, final Object eventPayload) {
        final EventLog eventLog = new EventLog();
        eventLog.setEventType(eventType);
        try {
            eventLog.setEventBodyData(objectMapper.writeValueAsString(eventPayload));
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("could not map object to json: " + eventPayload.toString(), e);
        }

        eventLog.setFlowId(flowIdComponent.getXFlowIdValue());
        return eventLog;
    }

  private Collection<EventLog> createEventLogs(
      final String eventType,
      final EventDataOperation eventDataOperation,
      final Map<String, Collection<Object>> dataTypeToData
  ) {
    return dataTypeToData.entrySet().stream()
        .flatMap(entry -> entry.getValue()
            .stream()
            .map(data -> createEventLog(
                eventType,
                new DataChangeEventEnvelope(eventDataOperation.toString(), entry.getKey(), data)
            )))
        .collect(Collectors.toList());
  }

}
