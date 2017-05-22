package org.zalando.nakadiproducer.transmission.impl;

public class EventTransmitter {
    private final EventTransmissionService eventTransmissionService;

    public EventTransmitter(EventTransmissionService eventTransmissionService) {
        this.eventTransmissionService = eventTransmissionService;
    }

    public void sendEvents() {
        eventTransmissionService.lockSomeEvents().forEach(eventTransmissionService::sendEvent);
    }
}
