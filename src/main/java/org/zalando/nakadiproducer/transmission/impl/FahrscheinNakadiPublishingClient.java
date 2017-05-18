package org.zalando.nakadiproducer.transmission.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;

public class FahrscheinNakadiPublishingClient implements NakadiPublishingClient {
    private final org.zalando.fahrschein.NakadiClient delegate;

    @Autowired
    public FahrscheinNakadiPublishingClient(org.zalando.fahrschein.NakadiClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(String eventType, List<?> nakadiEvents) throws Exception {
        delegate.publish(eventType, nakadiEvents);
    }
}
