package de.zalando.wholesale.tarbelapublisher.service.event.exception;

public class InvalidCursorException extends RuntimeException {

    private final String invalidCursor;

    public InvalidCursorException(final String invalidCursor) {
        this.invalidCursor = invalidCursor;
    }

    @Override
    public String getMessage() {
        return "The provided cursor (" + invalidCursor + ") is not numeric.";
    }

}
