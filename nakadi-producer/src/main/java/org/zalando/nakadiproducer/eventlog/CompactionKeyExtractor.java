package org.zalando.nakadiproducer.eventlog;

import org.zalando.nakadiproducer.eventlog.CompactionKeyExtractors.SimpleCompactionKeyExtractor;
import org.zalando.nakadiproducer.eventlog.CompactionKeyExtractors.TypedCompactionKeyExtractor;

import java.util.Optional;
import java.util.function.Function;

/**
 * This interface defines a way of extracting a compaction key from an object which
 * is sent as a payload in a compacted event type.
 * In most cases, for each compacted event type exactly one such object will be made known to the producer, and
 * you can define it using {@link #of(String, Class, Function)}, passing a method reference or a lambda.
 * For special occasions (e.g. where objects of different classes are used as payloads for the same event type)
 * also multiple extractors for the same event type are supported – in this case any which returns a
 * non-empty optional will be used.
 */
public interface CompactionKeyExtractor {

    default String getKeyOrNull(Object payload) {
        return tryGetKeyFor(payload).orElse(null);
    }

    Optional<String> tryGetKeyFor(Object o);

    String getEventType();

    /**
     * A type-safe compaction key extractor. This will be the one to be used by most applications.
     *
     * @param eventType Indicates the event type. Only events sent to this event type will be considered.
     * @param type  A Java type for payload objects. Only payload objects where {@code type.isInstance(payload)}
     *             will be considered at all.
     * @param extractorFunction A function extracting a compaction key from a payload object.
     *                          This will commonly be given as a method reference or lambda.
     * @return  A compaction key extractor, to be defined as a spring bean (if using the spring-boot starter)
     *  or passed manually to the event log writer implementation (if using nakadi-producer directly).
     *  (This should not return null.)
     * @param <X> the type of {@code type} and input type of {@code extractorFunction}.
     */
    static <X> CompactionKeyExtractor of(String eventType, Class<X> type, Function<X, String> extractorFunction) {
        return new TypedCompactionKeyExtractor<>(eventType, type, extractorFunction);
    }

    /**
     * Non-type safe key extractor, returning an Optional.
     * @param eventType The event type for which this extractor is intended.
     * @param extractor The extractor function. It is supposed to return {@link Optional#empty()} if this extractor
     *                 can't handle the input object, otherwise the actual key.
     * @return a key extractor object.
     */
    static CompactionKeyExtractor ofOptional(String eventType, Function<Object, Optional<String>> extractor) {
        return new SimpleCompactionKeyExtractor(eventType, extractor);
    }

    /**
     * Non-type safe key extractor, returning null for unknown objects.
     * @param eventType The event type for which this extractor is intended.
     * @param extractor The extractor function. It is supposed to return {@code null} if this extractor
     *                 can't handle the input object, otherwise the actual key.
     * @return a key extractor object.
     */
    static CompactionKeyExtractor ofNullable(String eventType, Function<Object, String> extractor) {
        return new SimpleCompactionKeyExtractor(eventType, extractor.andThen(Optional::ofNullable));
    }

    /**
     * An universal key extractor, capable of handling all objects.
     * @param eventType The event type for which this extractor is intended.
     * @param extractor The extractor function. It is not allowed to return {@code null}.
     * @return a key extractor object.
     */
    static CompactionKeyExtractor of(String eventType, Function<Object, String> extractor) {
        return new SimpleCompactionKeyExtractor(eventType, extractor.andThen(Optional::of));
    }
}
