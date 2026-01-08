package org.zalando.nakadiproducer.transmission.impl;

import nakadi.NakadiClient;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;

import java.util.Collection;
import java.util.List;

public class NakadiJavaNakadiPublishingClient implements NakadiPublishingClient {
    private final NakadiClient delegate;

    public NakadiJavaNakadiPublishingClient(NakadiClient delegate) {
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(String eventType, List<?> nakadiEvents) throws Exception {
        delegate.resources().events().send(eventType, (Collection) nakadiEvents);
    }
}
