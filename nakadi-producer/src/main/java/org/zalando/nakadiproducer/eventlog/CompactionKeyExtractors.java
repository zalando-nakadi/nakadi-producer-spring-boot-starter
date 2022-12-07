package org.zalando.nakadiproducer.eventlog;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;
import java.util.function.Function;

/**
 * This class contains implementations of {@link CompactionKeyExtractor} used by the factory methods in that interface.
 */
final class CompactionKeyExtractors {

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    static class SimpleCompactionKeyExtractor implements CompactionKeyExtractor {
        @Getter
        private final String eventType;
        private final Function<Object, Optional<String>> extractorFunction;

        @Override
        public Optional<String> tryGetKeyFor(Object o) {
            return extractorFunction.apply(o);
        }
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    static class TypedCompactionKeyExtractor<X> implements CompactionKeyExtractor {
        @Getter
        private final String eventType;
        private final Class<X> type;
        private final Function<X, String> extractorFunction;

        @Override
        public Optional<String> tryGetKeyFor(Object o) {
            if(type.isInstance(o)) {
                return Optional.of(extractorFunction.apply(type.cast(o)));
            } else {
                return Optional.empty();
            }
        }
    }
}
