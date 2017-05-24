package org.zalando.nakadiproducer.eventlog.impl;

public enum EventDataOperation {

    CREATE("C"),

    UPDATE("U"),

    DELETE("D"),

    SNAPSHOT("S");

    private final String value;

    EventDataOperation(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
