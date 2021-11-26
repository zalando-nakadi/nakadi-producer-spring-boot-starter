package org.zalando.nakadiproducer;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.zalando.fahrschein.NakadiClient;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepositoryImpl;
import org.zalando.nakadiproducer.eventlog.impl.EventLogWriterImpl;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.flowid.NoopFlowIdComponent;
import org.zalando.nakadiproducer.flowid.TracerFlowIdComponent;
import org.zalando.nakadiproducer.snapshots.SnapshotEventGenerator;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotCreationService;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotEventCreationEndpoint;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;
import org.zalando.nakadiproducer.transmission.impl.EventTransmissionService;
import org.zalando.nakadiproducer.transmission.impl.EventTransmitter;
import org.zalando.nakadiproducer.transmission.impl.FahrscheinNakadiPublishingClient;
import org.zalando.tracer.Tracer;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@AutoConfigureAfter(name="org.zalando.tracer.spring.TracerAutoConfiguration")
@EnableScheduling
@EnableConfigurationProperties({ DataSourceProperties.class, FlywayProperties.class })
@Slf4j
public class NakadiProducerAutoConfiguration {

    @ConditionalOnMissingBean({NakadiPublishingClient.class, NakadiClient.class})
    @Configuration
    @Import(FahrscheinWithTokensNakadiClientConfiguration.StupsTokenConfiguration.class)
    static class FahrscheinWithTokensNakadiClientConfiguration {

        @Bean
        public NakadiPublishingClient nakadiProducerPublishingClient(AccessTokenProvider accessTokenProvider,
                @Value("${nakadi-producer.nakadi-base-uri}") URI nakadiBaseUri) {
            return new FahrscheinNakadiPublishingClient(NakadiClient.builder(nakadiBaseUri)
                    .withAccessTokenProvider(accessTokenProvider::getAccessToken).build());
        }

        @ConditionalOnClass(name = "org.zalando.stups.tokens.Tokens")
        @Configuration
        static class StupsTokenConfiguration {
            @Bean(destroyMethod = "stop")
            @ConditionalOnMissingBean(AccessTokenProvider.class)
            public StupsTokenComponent accessTokenProvider(
                    @Value("${nakadi-producer.access-token-uri:http://nakadi-producer.access-token-uri.not-set}") URI accessTokenUri,
                    @Value("${nakadi-producer.access-token-scopes:uid}") String[] accessTokenScopes) {
                return new StupsTokenComponent(accessTokenUri, Arrays.asList(accessTokenScopes));
            }
        }
    }

    @ConditionalOnMissingBean(NakadiPublishingClient.class)
    @ConditionalOnBean(NakadiClient.class)
    @Configuration
    static class ExistingFahrscheinNakadiClientConfiguration {

        @Bean
        public NakadiPublishingClient nakadiProducerPublishingClient(NakadiClient fahrscheinNakadiClient) {
            return new FahrscheinNakadiPublishingClient(fahrscheinNakadiClient);
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
        private Tracer tracer;

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
    public SnapshotEventCreationEndpoint snapshotEventCreationEndpoint(
            SnapshotCreationService snapshotCreationService, FlowIdComponent flowIdComponent) {
        return new SnapshotEventCreationEndpoint(snapshotCreationService, flowIdComponent);
    }

    @Bean
    public SnapshotCreationService snapshotCreationService(
            Optional<List<SnapshotEventGenerator>> snapshotEventGenerators,
            EventLogWriter eventLogWriter) {
        return new SnapshotCreationService(
                snapshotEventGenerators.orElse(Collections.emptyList()),
                eventLogWriter
        );
    }

    @Bean
    public EventLogWriter eventLogWriter(EventLogRepository eventLogRepository, ObjectMapper objectMapper,
            FlowIdComponent flowIdComponent) {
        return new EventLogWriterImpl(eventLogRepository, objectMapper, flowIdComponent);
    }

    @Bean
    public EventLogRepository eventLogRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new EventLogRepositoryImpl(namedParameterJdbcTemplate);
    }

    @Bean
    public EventTransmitter eventTransmitter(EventTransmissionService eventTransmissionService) {
        return new EventTransmitter(eventTransmissionService);
    }

    @Bean
    public EventTransmissionScheduler eventTransmissionScheduler(EventTransmitter eventTransmitter,
            @Value("${nakadi-producer.scheduled-transmission-enabled:true}") boolean scheduledTransmissionEnabled) {
        return new EventTransmissionScheduler(eventTransmitter, scheduledTransmissionEnabled);
    }

  @Bean
  public EventTransmissionService eventTransmissionService(
      EventLogRepository eventLogRepository,
      NakadiPublishingClient nakadiPublishingClient,
      ObjectMapper objectMapper,
      @Value("${nakadi-producer.lock-duration:600}") int lockDuration,
      @Value("${nakadi-producer.lock-duration-buffer:60}") int lockDurationBuffer) {
    return new EventTransmissionService(
        eventLogRepository, nakadiPublishingClient, objectMapper, lockDuration, lockDurationBuffer);
  }

    @Bean
    public FlywayMigrator flywayMigrator() {
        return new FlywayMigrator();
    }

}
