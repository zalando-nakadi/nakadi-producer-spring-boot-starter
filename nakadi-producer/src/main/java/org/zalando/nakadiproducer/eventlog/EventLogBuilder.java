package org.zalando.nakadiproducer.eventlog;

import org.zalando.nakadiproducer.eventlog.impl.EventLog;

public interface EventLogBuilder {

    EventLog buildEventLog(String eventType, Object eventPayload, String compactionKey);
}
