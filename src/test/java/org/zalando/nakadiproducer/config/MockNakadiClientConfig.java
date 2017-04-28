package org.zalando.nakadiproducer.config;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.fahrschein.NakadiClient;

@Configuration
public class MockNakadiClientConfig {
    @Bean
    public NakadiClient nakadiClient() {
        return mock(NakadiClient.class);
    }
}
