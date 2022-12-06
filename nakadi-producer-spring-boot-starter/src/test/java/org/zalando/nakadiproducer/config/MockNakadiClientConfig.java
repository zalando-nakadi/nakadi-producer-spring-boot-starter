package org.zalando.nakadiproducer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zalando.nakadiproducer.transmission.MockNakadiPublishingClient;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;

@Configuration
// we only want this in the tests which actually want a mock, i.e. ones which have a "test" profile.
@Profile("test")
public class MockNakadiClientConfig {
    @Bean
    public NakadiPublishingClient nakadiClient() {
        return new MockNakadiPublishingClient();
    }
}
