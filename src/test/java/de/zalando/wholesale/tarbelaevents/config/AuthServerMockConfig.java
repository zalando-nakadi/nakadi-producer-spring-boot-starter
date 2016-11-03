package de.zalando.wholesale.tarbelaevents.config;

import de.zalando.wholesale.tarbelaevents.util.MockResourceServerTokenServices;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

@Configuration
public class AuthServerMockConfig {

    @Bean
    @Profile("test")
    public ResourceServerTokenServices customResourceTokenServices() {
        return new MockResourceServerTokenServices();
    }
}
