package org.zalando.nakadiproducer.transmission;

import java.util.List;

public interface NakadiPublishingClient {
    void publish(String eventType, List<?> nakadiEvents) throws Exception;
}
