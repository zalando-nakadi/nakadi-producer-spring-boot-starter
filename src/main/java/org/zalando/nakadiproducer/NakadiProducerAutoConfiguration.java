package org.zalando.nakadiproducer;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.ManagementContextConfiguration;
import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.zalando.fahrschein.NakadiClient;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.flowid.NoopFlowIdComponent;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotEventCreationEndpoint;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotEventCreationMvcEndpoint;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotEventProviderNotImplementedException;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotCreationService;
import org.zalando.nakadiproducer.flowid.TracerFlowIdComponent;
import org.zalando.stups.tokens.Tokens;
import org.zalando.tracer.Tracer;

@Configuration
@EnableJpaAuditing
@Slf4j
@ComponentScan
@EnableJpaRepositories
@ManagementContextConfiguration
public class NakadiProducerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SnapshotEventProvider.class)
    public SnapshotEventProvider snapshotEventProvider() {
        log.error("SnapshotEventProvider interface should be implemented by the service in order to /events/snapshots/{event_type} work");
        return new SnapshotEventProvider() {
            @Override
            public List<Snapshot> getSnapshot(String eventType, @Nullable Object withIdGreaterThan) {
                throw new SnapshotEventProviderNotImplementedException();
            }

            @Override
            public Set<String> getSupportedEventTypes() {
                return Collections.emptySet();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(NakadiClient.class)
    public NakadiClient nakadiClient(AccessTokenProvider accessTokenProvider, @Value("${nakadi-producer.nakadi-base-uri}") URI nakadiBaseUri) {
        return NakadiClient.builder(nakadiBaseUri)
                           .withAccessTokenProvider(accessTokenProvider::getAccessToken)
                           .build();
    }

    @Bean(destroyMethod = "stop")
    @ConditionalOnClass(Tokens.class)
    @ConditionalOnProperty({"nakadi-producer.access-token-uri", "nakadi-producer.access-token-scopes"})
    @ConditionalOnMissingBean(AccessTokenProvider.class)
    public StupsTokenComponent accessTokenProvider(@Value("${nakadi-producer.access-token-uri}") URI accessTokenUri, @Value("${nakadi-producer.access-token-scopes}") Collection<String> accessTokenScopes) {
        return new StupsTokenComponent(accessTokenUri, accessTokenScopes);
    }

    @Bean
    @ConditionalOnMissingClass("org.zalando.tracer.Tracer")
    public FlowIdComponent flowIdComponentFake() {
        return new NoopFlowIdComponent();
    }

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Bean
    @ConditionalOnClass(name = "org.zalando.tracer.Tracer")
    public FlowIdComponent flowIdComponent(Tracer tracer) {
        return new TracerFlowIdComponent(tracer);
    }

    @Bean
    @ConditionalOnMissingBean
    public SnapshotEventCreationEndpoint snapshotEventCreationEndpoint(SnapshotCreationService snapshotCreationService) {
        return new SnapshotEventCreationEndpoint(snapshotCreationService);
    }

    @Bean
    @ConditionalOnBean(SnapshotEventCreationEndpoint.class)
    @ConditionalOnEnabledEndpoint("snapshot_event_creation")
    public SnapshotEventCreationMvcEndpoint snapshotEventCreationMvcEndpoint(SnapshotEventCreationEndpoint snapshotEventCreationEndpoint) {
        return new SnapshotEventCreationMvcEndpoint(snapshotEventCreationEndpoint);
    }
}
