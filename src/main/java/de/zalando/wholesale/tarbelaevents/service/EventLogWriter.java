package de.zalando.wholesale.tarbelaevents.service;

import javax.transaction.Transactional;

public interface EventLogWriter {

    /**
     * Suppose you want to store some object in a database and also you want to fire an event
     * about this object creation. Then you can call this method in the same transaction.
     * This method will serialize a payload object and will store an event with this payload
     * in a database and Tarbela will later read the event via the Publisher API (/events endpoint)
     * to publish this event in an event sink
     * @param payload some POJO that can be serialized into JSON (required parameter)
     * @param flowId Optional parameter to provide an X-Flow-ID value
     */
    @Transactional
    void fireCreateEvent(Object payload, String flowId);

    /**
     * Suppose you want to update some object in a database and also you want to fire an event
     * about this object mutation. Then you can call this method in the same transaction.
     * This method will serialize a payload object and will store an event with this payload
     * in a database and Tarbela will later read the event via the Publisher API (/events endpoint)
     * to publish this event in an event sink
     * @param payload some POJO that can be serialized into JSON (required parameter)
     * @param flowId Optional parameter to provide an X-Flow-ID value
     */
    @Transactional
    void fireUpdateEvent(Object payload, String flowId);

}
