package org.zalando.nakadiproducer.transmission.impl;

import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;

import java.util.List;

public class NakadiJavaNakadiPublishingClient implements NakadiPublishingClient {
    private final nakadi.NakadiClient delegate;

    public NakadiJavaNakadiPublishingClient(nakadi.NakadiClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(String eventType, List<?> nakadiEvents) throws Exception {
        delegate.resources().events().sendBatch(eventType, (List<NakadiEvent>) nakadiEvents);
    }
}
