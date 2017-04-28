package org.zalando.nakadiproducer.snapshots;

public class SnapshotEventProviderNotImplementedException extends RuntimeException {

    @Override
    public String getMessage() {
        return "Snapshot not implemented by the service";
    }

}
