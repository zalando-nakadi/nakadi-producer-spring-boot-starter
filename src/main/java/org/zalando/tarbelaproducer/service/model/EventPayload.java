package org.zalando.tarbelaproducer.service.model;

/**
 * The {@link EventPayload} Interface should be implemented by any object that represents
 * event payload.
 *
 * The object should provide an event type and data type corresponding to the object
 * that represents the data payload itself
 *
 * {@link EventPayload::getData} is for returning the data payload itself.
 * This object should be serializable to Json by Jackson ObjectMapper
 */
public interface EventPayload {

    /**
     * Returns predefined event type sting name that will be attached
     * to each {@code EventDTO}'s {@code channel} as a {@code topicName} parameter
     * @return event type name
     */
    String getEventType();

    /**
     * Returns predefined data type string name that will be attached
     * to each {@code EventDTO}'s {@code event_payload} as a {@code data_type} parameter
     * @return data type name
     */
    String getDataType();

    /**
     * Returns event data payload itself that will be attached to each {@code EventDTO}'s {@code event_payload} as a {@code data} parameter.
     * This object should be serializable to Json by Jackson ObjectMapper.
     * For example if we want to sent an event about creation of some object
     * then {@code getData} should return the object itself
     * @return event data
     */
    Object getData();

}
