package de.zalando.wholesale.tarbelaevents;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableJpaAuditing
@Slf4j
public class TarbelaEventsAutoConfiguration {

    // TODO: create default configuration

    @Bean
    @ConditionalOnMissingBean(TarbelaSnapshotProvider.class)
    public TarbelaSnapshotProvider<?> tarbelaSnapshotProvider() {
        log.error("TarbelaSnapshotProvider interface should be implemented by the service in order to /events/snapshots work");
        return () -> {
            throw new TarbelaSnapshotProviderNotImplemented();
        };
    }

}
