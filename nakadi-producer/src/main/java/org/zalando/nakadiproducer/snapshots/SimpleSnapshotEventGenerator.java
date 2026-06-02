package org.zalando.nakadiproducer.snapshots;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This is a simple implementation of the {@link SnapshotEventGenerator}. It is
 * meant to be used in a functional style.
 *
 * @see SnapshotEventGenerator
 * @deprecated Please use the {@link SnapshotEventGenerator#of} methods instead.
 */
@Deprecated
public final class SimpleSnapshotEventGenerator implements SnapshotEventGenerator {
    private final String supportedEventType;
    private final BiFunction<Object, String, List<Snapshot>> getSnapshotFunction;

    /**
     * Creates a SnapshotEventGenerator for an event type, which doesn't support
     * filtering.
     *
     * @param supportedEventType
     *            the eventType that this SnapShotEventProvider will support.
     * @param snapshotEventFactory
     *            a snapshot event factory function conforming to the
     *            specification of
     *            {@link SnapshotEventGenerator#generateSnapshots(Object, String)}
     *            (but without the filter parameter). Any filter provided by the
     *            caller will be thrown away.
     */
    public SimpleSnapshotEventGenerator(String supportedEventType,
            Function<Object, List<Snapshot>> snapshotEventFactory) {
        this.supportedEventType = supportedEventType;
        this.getSnapshotFunction = (id, filter) -> snapshotEventFactory.apply(id);
    }

    /**
     * Creates a SnapshotEventGenerator for an event type, with filtering
     * support.
     *
     * @param supportedEventType
     *            the eventType that this SnapShotEventProvider will support.
     * @param snapshotEventFactory
     *            a snapshot event factory function conforming to the
     *            specification of
     *            {@link SnapshotEventGenerator#generateSnapshots(Object, String)}.
     */
    public SimpleSnapshotEventGenerator(String supportedEventType,
            BiFunction<Object, String, List<Snapshot>> snapshotEventFactory) {
        this.supportedEventType = supportedEventType;
        this.getSnapshotFunction = snapshotEventFactory;
    }

    @Override
    public List<Snapshot> generateSnapshots(@Nullable Object withIdGreaterThan, String filter) {
        return getSnapshotFunction.apply(withIdGreaterThan, filter);
    }

    @Override
    public String getSupportedEventType() {
        return supportedEventType;
    }
}
