package org.zalando.nakadiproducer.service;

import javax.transaction.Transactional;

public interface EventLogService {
    /**
     * Creates snapshot event logs of given type.
     */
    @Transactional
    void createSnapshotEvents(String eventType, String flowId);

    @Transactional
    void sendMessages();
}
