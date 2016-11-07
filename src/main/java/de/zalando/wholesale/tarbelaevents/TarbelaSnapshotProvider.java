package de.zalando.wholesale.tarbelaevents;

import java.util.stream.Stream;

/**
 * The <code>TarbelaSnapshotProvider</code> interface should be implemented by any
 * Tarbela Event Producer that wants to support snapshot events feature. The
 * class must define a method of no arguments called <code>getSnapshot</code>.
 * <p>
 *
 * @param <T> the type of elements of the stream returned by <code>getSnapshot</code>
 */
public interface TarbelaSnapshotProvider<T> {

    /**
     * Returns a stream consisting of elements for creating a snapshot of events.
     * @return stream of elements to create a snapshot from
     */
    Stream<T> getSnapshot();

}
