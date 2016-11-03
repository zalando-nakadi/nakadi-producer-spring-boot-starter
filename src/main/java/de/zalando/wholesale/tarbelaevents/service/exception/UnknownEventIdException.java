package de.zalando.wholesale.tarbelaevents.service.exception;

public class UnknownEventIdException extends RuntimeException {

    private final Integer eventId;

    public UnknownEventIdException(final Integer eventId) {
        this.eventId = eventId;
    }

    @Override
    public String getMessage() {
        return "No event log found for event id (" + eventId + ").";
    }
}
