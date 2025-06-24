package org.zalando.nakadiproducer.eventlog.impl;

public interface EventLogBuilder {

    EventLog buildEventLog(String eventType, Object eventPayload, String compactionKey);
}
