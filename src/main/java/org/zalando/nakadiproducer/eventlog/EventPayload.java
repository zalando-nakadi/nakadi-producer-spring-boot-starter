package org.zalando.nakadiproducer.eventlog;

/**
 * The {@link EventPayload} Interface should be implemented by any object that represents
 * event payload.
 *
 * The object should provide an event type and data type corresponding to the object
 * that represents the data payload itself.
 *
 * {@link EventPayload#getData} is for returning the data payload itself.
 * This object should be serializable to Json by Jackson ObjectMapper
 */
public interface EventPayload {

    /**
     * Returns the event type name that will be used as the
     * channel name for submission to Nakadi.
     *
     * @return event type name
     */
    String getEventType();

    /**
     * Returns a predefined data type string name that will be used as the {@code data_type} field
     * for a data change event. ([It is not really clear what it means.](https://github.com/zalando/nakadi/issues/382))
     * For business events, this is not used (and should be {@code null}.
     *
     * @return data type name
     */
    String getDataType();

    /**
     * Returns the event data payload itself that will be included in
     * data change events in the {@code data} field.
     * This object should be serializable to Json by Jackson ObjectMapper.
     *
     * For business events, the contents of this object will be used directly as the
     * event content (just with some metadata added).
     *
     * For example if we want to sent an event about creation or update of some object,
     * this should return the object itself (or a version of it which can be serialized to JSON).
     *
     * @return event data
     */
    Object getData();
}
