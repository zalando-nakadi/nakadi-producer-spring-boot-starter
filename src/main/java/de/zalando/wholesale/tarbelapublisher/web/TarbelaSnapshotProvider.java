package de.zalando.wholesale.tarbelapublisher.web;

import java.util.Collection;

public interface TarbelaSnapshotProvider<T> {

    Collection<T> getSnapshot();

}
