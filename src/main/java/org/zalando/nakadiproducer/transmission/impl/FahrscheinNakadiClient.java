package org.zalando.nakadiproducer.transmission.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.nakadiproducer.transmission.NakadiClient;

public class FahrscheinNakadiClient implements NakadiClient {
    private final org.zalando.fahrschein.NakadiClient delegate;

    @Autowired
    public FahrscheinNakadiClient(org.zalando.fahrschein.NakadiClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public void publish(String eventType, List<?> nakadiEvents) throws Exception {
        delegate.publish(eventType, nakadiEvents);
    }
}
