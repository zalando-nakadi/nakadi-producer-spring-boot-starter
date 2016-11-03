package de.zalando.wholesale.tarbelapublisher;

public class TarbelaSnapshotProviderNotImplemented extends RuntimeException {

    @Override
    public String getMessage() {
        return "Tarbela Snapshot not implemented by the service";
    }

}
