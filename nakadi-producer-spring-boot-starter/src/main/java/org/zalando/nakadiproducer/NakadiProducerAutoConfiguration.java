package org.zalando.nakadiproducer;

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.zalando.fahrschein.NakadiClient;
import org.zalando.nakadiproducer.eventlog.EventLogWriter;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepository;
import org.zalando.nakadiproducer.eventlog.impl.EventLogRepositoryImpl;
import org.zalando.nakadiproducer.eventlog.impl.EventLogWriterImpl;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;
import org.zalando.nakadiproducer.flowid.NoopFlowIdComponent;
import org.zalando.nakadiproducer.flowid.TracerFlowIdComponent;
import org.zalando.nakadiproducer.snapshots.SimpleSnapshotEventGenerator;
import org.zalando.nakadiproducer.snapshots.Snapshot;
import org.zalando.nakadiproducer.snapshots.SnapshotEventGenerator;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotCreationService;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotEventCreationEndpoint;
import org.zalando.nakadiproducer.snapshots.impl.SnapshotEventCreationMvcEndpoint;
import org.zalando.nakadiproducer.transmission.NakadiPublishingClient;
import org.zalando.nakadiproducer.transmission.impl.EventTransmissionService;
import org.zalando.nakadiproducer.transmission.impl.EventTransmitter;
import org.zalando.nakadiproducer.transmission.impl.FahrscheinNakadiPublishingClient;
import org.zalando.tracer.Tracer;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@AutoConfigureAfter(name="org.zalando.tracer.spring.TracerAutoConfiguration")
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
            @ConditionalOnProperty({ "nakadi-producer.access-token-uri" })
            @ConditionalOnMissingBean(AccessTokenProvider.class)
            public StupsTokenComponent accessTokenProvider(
                    @Value("${nakadi-producer.access-token-uri}") URI accessTokenUri,
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

    @ManagementContextConfiguration
    static class ManagementEndpointConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public SnapshotEventCreationEndpoint snapshotEventCreationEndpoint(
                SnapshotCreationService snapshotCreationService, FlowIdComponent flowIdComponent) {
            return new SnapshotEventCreationEndpoint(snapshotCreationService, flowIdComponent);
        }

        @Bean
        @ConditionalOnBean(SnapshotEventCreationEndpoint.class)
        @ConditionalOnEnabledEndpoint("snapshot_event_creation")
        public SnapshotEventCreationMvcEndpoint snapshotEventCreationMvcEndpoint(
                SnapshotEventCreationEndpoint snapshotEventCreationEndpoint) {
            return new SnapshotEventCreationMvcEndpoint(snapshotEventCreationEndpoint);
        }
    }

    @Bean
    public SnapshotCreationService snapshotCreationService(
            Optional<List<SnapshotEventGenerator>> snapshotEventGenerators,
            Optional<SnapshotEventProvider> snapshotEventProvider, EventLogWriter eventLogWriter) {
        final Stream<SnapshotEventGenerator> legacyGenerators =
                snapshotEventProvider.map(this::wrapInSnapshotEventGenerators)
                                     .orElseGet(Stream::empty);
        final Stream<SnapshotEventGenerator> nonLegacyGenerators =
                snapshotEventGenerators.map(List::stream)
                                       .orElseGet(Stream::empty);
        final List<SnapshotEventGenerator> allGenerators =
                Stream.concat(legacyGenerators, nonLegacyGenerators)
                      .collect(toList());
        return new SnapshotCreationService(allGenerators, eventLogWriter);
    }

    /**
     * This method (and the following three) support the legacy {@link SnapshotEventProvider} interface,
     * mapping it to the new logic (several {@link SnapshotEventGenerator}s).
     *
     * It will be removed when we don't support that interface anymore.
     */
    private Stream<SnapshotEventGenerator> wrapInSnapshotEventGenerators(SnapshotEventProvider p) {
        return p.getSupportedEventTypes()
                .stream()
                .map(t -> wrapInSnapshotEventGenerator(p, t));
    }

    private SnapshotEventGenerator wrapInSnapshotEventGenerator(SnapshotEventProvider provider, String eventType) {
        return new SimpleSnapshotEventGenerator(
            eventType,
            (cursor) -> createNonLegacySnapshots(provider, eventType, cursor)
        );
    }

    private List<Snapshot> createNonLegacySnapshots(SnapshotEventProvider provider, String eventType, Object cursor) {
        return provider.getSnapshot(eventType, cursor)
                       .stream()
                       .map(this::mapLegacyToNewSnapshot)
                       .collect(toList());
    }

    private Snapshot mapLegacyToNewSnapshot(SnapshotEventProvider.Snapshot snapshot) {
        return new Snapshot(snapshot.getId(), snapshot.getDataType(), snapshot.getData());
    }

    @Bean
    public EventLogWriter eventLogWriter(EventLogRepository eventLogRepository, ObjectMapper objectMapper,
            FlowIdComponent flowIdComponent) {
        return new EventLogWriterImpl(eventLogRepository, objectMapper, flowIdComponent);
    }

    @Bean
    public EventLogRepository eventLogRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
        @Value("${nakadi-producer.lock-size:0}") int lockSize) {
        return new EventLogRepositoryImpl(namedParameterJdbcTemplate, lockSize);
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
