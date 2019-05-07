package org.zalando.nakadiproducer.snapshots.impl;

import java.util.Set;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.zalando.nakadiproducer.flowid.FlowIdComponent;

@ConfigurationProperties("endpoints.snapshot-event-creation")
public class SnapshotEventCreationEndpoint extends AbstractEndpoint<SnapshotEventCreationEndpoint.SnapshotReport> {
    private final SnapshotCreationService snapshotCreationService;
    private final FlowIdComponent flowIdComponent;

    public SnapshotEventCreationEndpoint(SnapshotCreationService snapshotCreationService, FlowIdComponent flowIdComponent) {
        super("snapshot_event_creation", true, true);
        this.snapshotCreationService = snapshotCreationService;
        this.flowIdComponent = flowIdComponent;
    }

    @Override
    public SnapshotReport invoke() {
        return new SnapshotReport(snapshotCreationService.getSupportedEventTypes());
    }

    public void invoke(String eventType, String filter) {
        flowIdComponent.startTraceIfNoneExists();
        snapshotCreationService.createSnapshotEvents(eventType, filter);
    }


    @AllArgsConstructor
    @Getter
    public static class SnapshotReport {
        private final Set<String> supportedEventTypes;
    }

}
