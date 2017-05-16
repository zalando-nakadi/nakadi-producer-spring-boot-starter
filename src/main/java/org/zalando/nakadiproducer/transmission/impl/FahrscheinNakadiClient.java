package org.zalando.nakadiproducer.transmission.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.zalando.nakadiproducer.transmission.NakadiClient;

public class FahrscheinNakadiClient implements NakadiClient {
    private final org.zalando.fahrschein.NakadiClient delegatee;

    @Autowired
    public FahrscheinNakadiClient(org.zalando.fahrschein.NakadiClient delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public void publish(String eventType, List<?> nakadiEvents) throws Exception {
        delegatee.publish(eventType, nakadiEvents);
    }
}
