package de.zalando.wholesale.tarbelaproducer;

public class TarbelaSnapshotProviderNotImplementedException extends RuntimeException {

    @Override
    public String getMessage() {
        return "Tarbela Snapshot not implemented by the service";
    }

}
