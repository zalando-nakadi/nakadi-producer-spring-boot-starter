package de.zalando.wholesale.tarbelaproducer;

import de.zalando.wholesale.tarbelaproducer.web.FlowIdComponent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.zalando.tracer.Tracer;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableJpaAuditing
@Slf4j
@EnableConfigurationProperties(TarbelaProperties.class)
@ComponentScan
public class TarbelaProducerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TarbelaSnapshotProvider.class)
    public TarbelaSnapshotProvider tarbelaSnapshotProvider() {
        log.error("TarbelaSnapshotProvider interface should be implemented by the service in order to /events/snapshots/{event_type} work");
        return eventType -> {
            throw new TarbelaSnapshotProviderNotImplementedException();
        };
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
