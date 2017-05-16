package org.zalando.nakadiproducer.transmission;

import java.util.List;

public interface NakadiClient {
    void publish(String eventType, List<?> nakadiEvents) throws Exception;
}
