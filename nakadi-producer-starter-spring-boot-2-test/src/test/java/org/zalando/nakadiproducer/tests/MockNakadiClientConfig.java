package org.zalando.nakadiproducer.tests;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.nakadiproducer.transmission.MockNakadiPublishingClient;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;

@Configuration
public class MockNakadiClientConfig {
    @Bean
    public NakadiPublishingClient mockNakadiPublishingClient() {
        return new MockNakadiPublishingClient();
    }
}
