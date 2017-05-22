package org.zalando.nakadiproducer.snapshots;

public class UnknownEventTypeException extends RuntimeException {

    private final String eventType;

    public UnknownEventTypeException(final String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String getMessage() {
        return "No event log found for event type (" + eventType + ").";
    }
}
