package org.zalando.nakadiproducer.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.zalando.nakadiproducer.AccessTokenProvider;

@Configuration
public class TokenConfiguration {

    @Primary
    @Bean
    public AccessTokenProvider accessTokenProvider() {
        return () -> "MY-FAKE-TOKEN";
    }

}
