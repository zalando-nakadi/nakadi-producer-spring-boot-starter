package org.zalando.nakadiproducer.eventlog;

import lombok.Data;

import java.util.Optional;
import java.util.function.Function;

@Data(staticConstructor = "of")
public class CompactionKeyExtractor<X> {
    private final String eventType;
    private final Class<X> type;
    private final Function<X, String> extractorFunction;


    public Optional<String> tryGetKeyFor(Object o) {
        if(type.isInstance(o)) {
            return Optional.of(extractorFunction.apply(type.cast(o)));
        } else {
            return Optional.empty();
        }
    }
}
