package org.zalando.nakadiproducer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.nakadiproducer.transmission.MockNakadiClient;
import org.zalando.nakadiproducer.transmission.NakadiClient;

@Configuration
public class MockNakadiClientConfig {
    @Bean
    public NakadiClient nakadiClient() {
        return new MockNakadiClient();
    }
}
