package org.zalando.nakadiproducer.snapshots.impl;

import java.util.Set;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@ConfigurationProperties("endpoints.snapshot-event-creation")
public class SnapshotEventCreationEndpoint extends AbstractEndpoint<SnapshotEventCreationEndpoint.SnapshotReport> {
    private final SnapshotCreationService snapshotCreationService;

    public SnapshotEventCreationEndpoint(SnapshotCreationService snapshotCreationService) {
        super("snapshot_event_creation", true, true);
        this.snapshotCreationService = snapshotCreationService;
    }

    @Override
    public SnapshotReport invoke() {
        return new SnapshotReport(snapshotCreationService.getSupportedEventTypes());
    }

    public void invoke(String eventType, String filter) {
        snapshotCreationService.createSnapshotEvents(eventType, filter);
    }


    @AllArgsConstructor
    @Getter
    public static class SnapshotReport {
        private final Set<String> supportedEventTypes;
    }

}
