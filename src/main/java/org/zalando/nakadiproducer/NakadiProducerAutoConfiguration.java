package org.zalando.nakadiproducer;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.ManagementContextConfiguration;
import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.flowid.NoopFlowIdComponent;
import org.zalando.nakadiproducer.flowid.TracerFlowIdComponent;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotCreationService;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotEventCreationEndpoint;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotEventCreationMvcEndpoint;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotEventProviderNotImplementedException;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;
import org.zalando.nakadiproducer.transmission.impl.FahrscheinNakadiPublishingClient;
import org.zalando.tracer.Tracer;

@Configuration
@Slf4j
@ComponentScan
@ManagementContextConfiguration
@AutoConfigureAfter(name="org.zalando.tracer.spring.TracerAutoConfiguration")
public class NakadiProducerAutoConfiguration {

    @Autowired
    private DataSource dataSource;

    @Bean
    @ConditionalOnMissingBean(SnapshotEventProvider.class)
    public SnapshotEventProvider snapshotEventProvider() {
        log.error("SnapshotEventProvider interface should be implemented by the service in order to /events/snapshots/{event_type} work");
        return new SnapshotEventProvider() {
            @Override
            public List<Snapshot> getSnapshot(@Nonnull String eventType, @Nullable Object withIdGreaterThan) {
                throw new SnapshotEventProviderNotImplementedException();
            }

            @Override
            public Set<String> getSupportedEventTypes() {
                return Collections.emptySet();
            }
        };
    }

    @ConditionalOnMissingBean(NakadiPublishingClient.class)
    @Configuration
    @Import(FahrscheinNakadiClientConfiguration.StupsTokenConfiguration.class)
    static class FahrscheinNakadiClientConfiguration {

        @Bean
        public NakadiPublishingClient nakadiClient(AccessTokenProvider accessTokenProvider, @Value("${nakadi-producer.nakadi-base-uri}") URI nakadiBaseUri) {
            return new FahrscheinNakadiPublishingClient(
                org.zalando.fahrschein.NakadiClient.builder(nakadiBaseUri)
                                                   .withAccessTokenProvider(accessTokenProvider::getAccessToken)
                                                   .build()
            );
        }


        @ConditionalOnClass(name = "org.zalando.stups.tokens.Tokens")
        @Configuration
        static class StupsTokenConfiguration {
            @Bean(destroyMethod = "stop")
            @ConditionalOnProperty({"nakadi-producer.access-token-uri", "nakadi-producer.access-token-scopes"})
            @ConditionalOnMissingBean(AccessTokenProvider.class)
            public StupsTokenComponent accessTokenProvider(@Value("${nakadi-producer.access-token-uri}") URI accessTokenUri, @Value("${nakadi-producer.access-token-scopes}") String[] accessTokenScopes) {
                return new StupsTokenComponent(accessTokenUri, Arrays.asList(accessTokenScopes));
            }
        }
    }

    @Bean
    @ConditionalOnMissingClass("org.zalando.tracer.Tracer")
    @ConditionalOnMissingBean(FlowIdComponent.class)
    public FlowIdComponent flowIdComponent() {
        return new NoopFlowIdComponent();
    }

    @ConditionalOnClass(name = "org.zalando.tracer.Tracer")
    @Configuration
    static class TracerConfiguration {
        @Autowired(required = false)
        Tracer tracer;

        @SuppressWarnings("SpringJavaAutowiringInspection")
        @ConditionalOnMissingBean(FlowIdComponent.class)
        @Bean
        public FlowIdComponent flowIdComponent() {
            if (tracer == null) {
                return new NoopFlowIdComponent();
            } else {
                return new TracerFlowIdComponent(tracer);
            }
        }
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

    @PostConstruct
    public void migrateFlyway() {
        Flyway flyway = new Flyway();
        flyway.setLocations("classpath:db_nakadiproducer/migrations");
        flyway.setSchemas("nakadi_events");
        flyway.setDataSource(dataSource);
        flyway.setBaselineOnMigrate(true);
        flyway.setBaselineVersionAsString("2133546886.1.0");
        flyway.migrate();
    }
}
