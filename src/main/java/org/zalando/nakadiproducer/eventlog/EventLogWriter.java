package org.zalando.nakadiproducer.eventlog;

import javax.transaction.Transactional;

public interface EventLogWriter {

    /**
     * Suppose you want to store some object in a database and also you want to fire an event
     * about this object creation. Then you can call this method in the same transaction.
     * This method will serialize a payload object and will store an event with this payload
     * in a database. After the transaction completed, it will later read the event and publish
     * it to nakadi
     *  @param eventType the nakadi event type of the event. This is roughly equivalent to an event channel or topic.
     *  @param dataType the content of the nakadi data_type field.
     *  @param data some POJO that can be serialized into JSON (required parameter). Will be used as content of the nakadi data field
     *
     */
    @Transactional
    void fireCreateEvent(String eventType, String dataType, Object data);

    /**
     * Suppose you want to update some object in a database and also you want to fire an event
     * about this object mutation. Then you can call this method in the same transaction.
     * This method will serialize a payload object and will store an event with this payload
     * in a database. After the transaction completed, it will later read the event and publish
     * it to nakadi
     *  @param eventType the nakadi event type of the event. This is roughly equivalent to an event channel or topic.
     *  @param dataType the content of the nakadi data type field.
     *  @param data some POJO that can be serialized into JSON (required parameter). Will be used as content of the nakadi data field
     *
     */
    @Transactional
    void fireUpdateEvent(String eventType, String dataType, Object data);

    /**
     * Suppose you want to remove an object from a database and also you want to fire an event
     * about this object removal. Then you can call this method in the same transaction.
     * This method will serialize a payload object and will store an event with this payload
     * in a database. After the transaction completed, it will later read the event and publish
     * it to nakadi
     *  @param eventType the nakadi event type of the event. This is roughly equivalent to an event channel or topic.
     *  @param dataType the content of the nakadi data type field.
     *  @param data some POJO that can be serialized into JSON (required parameter). Will be used as content of the nakadi data field
     *
     */
    @Transactional
    void fireDeleteEvent(String eventType, String dataType, Object data);

    /**
     * Suppose you want to notify your consumers about the current state of a resource, even if nothing changed.
     * Typical use cases include initial replication to new consumers or hotfixes of data inconsistencies between
     * producer and consumer. Then you can call this method.
     *
     * This method will serialize a payload object and will store an event with this payload
     * in a database. After the transaction completed, it will later read the event and publish
     * it to nakadi
     *  @param eventType the nakadi event type of the event. This is roughly equivalent to an event channel or topic.
     *  @param dataType the content of the nakadi data type field.
     *  @param data some POJO that can be serialized into JSON (required parameter). Will be used as content of the nakadi data field
     *
     */
    @Transactional
    void fireSnapshotEvent(String eventType, String dataType, Object data);

    /**
     * Suppose you want to perform some action on an object in a database and also you want to fire a
     * business event related to this object change. Then you can call this method in the same transaction.
     * This method will serialize a payload object and will store an event with this payload
     * in a database. After the transaction completed, it will later read the event and publish
     * it to nakadi
     *  @param eventType the nakadi event type of the event. This is roughly equivalent to an event channel or topic.
     *  @param payload some POJO that can be serialized into JSON (required parameter)
     */
    @Transactional
    void fireBusinessEvent(String eventType, Object payload);
}
