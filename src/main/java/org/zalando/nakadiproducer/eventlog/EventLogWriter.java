package org.zalando.nakadiproducer.eventlog;

import javax.transaction.Transactional;

public interface EventLogWriter {

    /**
     * Suppose you want to store some object in a database and also you want to fire an event
     * about this object creation. Then you can call this method in the same transaction.
     * This method will serialize a payload object and will store an event with this payload
     * in a database. After the transaction completed, it will later read the event and publish
     * it to nakadi
     *  @param payload some POJO that can be serialized into JSON (required parameter)
     *
     */
    @Transactional
    void fireCreateEvent(String eventType, EventPayload payload);

    /**
     * Suppose you want to update some object in a database and also you want to fire an event
     * about this object mutation. Then you can call this method in the same transaction.
     * This method will serialize a payload object and will store an event with this payload
     * in a database. After the transaction completed, it will later read the event and publish
     * it to nakadi
     *  @param payload some POJO that can be serialized into JSON (required parameter)
     *
     */
    @Transactional
    void fireUpdateEvent(String eventType, EventPayload payload);

    /**
     * Suppose you want to remove an object from a database and also you want to fire an event
     * about this object removal. Then you can call this method in the same transaction.
     * This method will serialize a payload object and will store an event with this payload
     * in a database. After the transaction completed, it will later read the event and publish
     * it to nakadi
     *  @param payload some POJO that can be serialized into JSON (required parameter)
     *
     */
    @Transactional
    void fireDeleteEvent(String eventType, EventPayload payload);

    @Transactional
    void fireSnapshotEvent(String eventType, EventPayload payload);

    /**
     * Suppose you want to perform some action on an object in a database and also you want to fire a
     * business event related to this object change. Then you can call this method in the same transaction.
     * This method will serialize a payload object and will store an event with this payload
     * in a database. After the transaction completed, it will later read the event and publish
     * it to nakadi
     * @param payload some POJO that can be serialized into JSON (required parameter)
     */
    @Transactional
    void fireBusinessEvent(String eventType, EventPayload payload);
}
