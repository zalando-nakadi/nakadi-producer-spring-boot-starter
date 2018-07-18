package org.zalando.nakadiproducer.snapshots.impl;

import java.util.Set;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Endpoint(id = "snapshot-event-creation")
public class SnapshotEventCreationEndpoint {
    private final SnapshotCreationService snapshotCreationService;

    public SnapshotEventCreationEndpoint(SnapshotCreationService snapshotCreationService) {
        this.snapshotCreationService = snapshotCreationService;
    }

    @ReadOperation
    public SnapshotReport getSupportedEventTypes() {
        return new SnapshotReport(snapshotCreationService.getSupportedEventTypes());
    }

    @WriteOperation
    public void createFilteredSnapshotEvents(
            // this is the event type. Could have a better name, but since Spring Boot relies on the -parameters
            // compiler flag being set to resolve path parameter names, it would then get trickier to reliably run this
            // Test in the IDE. So let's stick with arg0 for now.
            @Selector String arg0,
            @Nullable String filter) {
        snapshotCreationService.createSnapshotEvents(arg0, filter);
    }


    @AllArgsConstructor
    @Getter
    public static class SnapshotReport {
        private final Set<String> supportedEventTypes;
    }

}
