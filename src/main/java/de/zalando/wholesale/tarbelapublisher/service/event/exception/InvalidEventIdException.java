package de.zalando.wholesale.tarbelapublisher.service.event.exception;

public class InvalidEventIdException extends RuntimeException {
    private final String eventId;

    public InvalidEventIdException(final String eventId) {
        this.eventId = eventId;
    }

    @Override
    public String getMessage() {
        return "The provided event id (" + eventId + ") is not numeric.";
    }
}
