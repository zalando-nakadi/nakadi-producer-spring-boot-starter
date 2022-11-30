package org.zalando.nakadiproducer.eventlog;

/**
 * This is a functional interface to be implemented by applications which want to produce compacted events.
 * In many cases it's possible to implement this with a method reference to a getter (like {@code Order::getNumber}),
 * in more complex cases it's usually still possible as a one-line Lambda.
 * @param <X> The (Java) type of objects for which the compaction key will be computed.
 * @see EventLogWriter#registerCompactionKeyExtractor(String, Class, CompactionKeyExtractor)
 */
@FunctionalInterface
public interface CompactionKeyExtractor<X> {
    /**
     * Extracts a compaction key of a data object.
     *
     * @param data The data/payload to be sent in the event.
     * @return the compaction key to be written in the metadata.
     */
    String getCompactionKeyFor(X data);
}
