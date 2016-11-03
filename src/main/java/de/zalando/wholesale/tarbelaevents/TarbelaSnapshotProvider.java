package de.zalando.wholesale.tarbelaevents;

import java.util.Collection;

public interface TarbelaSnapshotProvider<T> {

    Collection<T> getSnapshot();

}
