package org.zalando.nakadiproducer.snapshots;

import java.util.List;
import java.util.function.Function;

/**
 * This is a simple implementation of the {@link SnapshotEventProvider}.
 * It is meant to be used in a functional style
 *
 * @see SnapshotEventProvider
 */
public final class SimpleSnapshotEventProvider implements SnapshotEventProvider {
    private final String supportedEventType;
    private final Function<Object, List<Snapshot>> getSnapshotFunction;

    /**
     * @param supportedEventType the eventType that this SnapShotEventProvider will support
     * @param snapshotEventFactory a snapshot event factory function conforming to the specification of @link {@link SnapshotEventProvider#getSnapshot(Object)}
     */
    public SimpleSnapshotEventProvider(String supportedEventType, Function<Object, List<Snapshot>> snapshotEventFactory) {
        this.supportedEventType = supportedEventType;
        this.getSnapshotFunction = snapshotEventFactory;
    }


    @Override
    public List<Snapshot> getSnapshot(Object withIdGreaterThan) {
        return getSnapshotFunction.apply(withIdGreaterThan);
    }

    @Override
    public String getSupportedEventType() {
        return supportedEventType;
    }
}
