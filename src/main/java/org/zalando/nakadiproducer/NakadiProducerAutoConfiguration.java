package org.zalando.nakadiproducer;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.zalando.fahrschein.NakadiClient;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProviderNotImplementedException;
import org.zalando.tracer.Tracer;

@Configuration
@EnableJpaAuditing
@Slf4j
@EnableConfigurationProperties(NakadiProperties.class)
@ComponentScan
@EnableJpaRepositories
public class NakadiProducerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SnapshotEventProvider.class)
    public SnapshotEventProvider snapshotEventProvider() {
        log.error("SnapshotEventProvider interface should be implemented by the service in order to /events/snapshots/{event_type} work");
        return (eventType, withIdGreaterThan) -> {
            throw new SnapshotEventProviderNotImplementedException();
        };
    }

    @Bean
    @ConditionalOnMissingBean(NakadiClient.class)
    public NakadiClient nakadiClient(AccessTokenProvider accessTokenProvider, @Value("${nakadi-producer.nakadi-base-uri}") URI nakadiBaseUri) {
        return NakadiClient.builder(nakadiBaseUri)
                           .withAccessTokenProvider(accessTokenProvider::getAccessToken)
                           .build();
    }

    @Bean
    @ConditionalOnMissingClass("org.zalando.tracer.Tracer")
    public FlowIdComponent flowIdComponentFake() {
        FlowIdComponent flowIdComponent = new FlowIdComponent(null);
        return flowIdComponent;
    }

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Bean
    @ConditionalOnClass(name = "org.zalando.tracer.Tracer")
    public FlowIdComponent flowIdComponent(Tracer tracer) {
        FlowIdComponent flowIdComponent = new FlowIdComponent(tracer);
        return flowIdComponent;
    }

}
