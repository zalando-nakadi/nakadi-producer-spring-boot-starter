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
    public void createFilteredSnapshotEvents(@Selector String arg0, @Nullable String filter) {
        snapshotCreationService.createSnapshotEvents(arg0, filter);
    }


    @AllArgsConstructor
    @Getter
    public static class SnapshotReport {
        private final Set<String> supportedEventTypes;
    }

}
