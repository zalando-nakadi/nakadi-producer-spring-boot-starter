package org.zalando.nakadiproducer.util;

import java.util.ArrayList;
import java.util.List;

import org.zalando.nakadiproducer.eventlog.DataChangeEventPayload;
import org.zalando.nakadiproducer.eventlog.SimpleDataChangeEventPayload;
import org.zalando.nakadiproducer.snapshots.SnapshotEventProvider.Snapshot;

public class Fixture {

    public static final String PUBLISHER_EVENT_TYPE = "wholesale.some-publisher-change-event";
    public static final String PUBLISHER_DATA_TYPE = "nakadi:some-publisher";

    public static DataChangeEventPayload mockEventPayload(MockPayload mockPayload) {
        return SimpleDataChangeEventPayload.builder()
                                           .data(mockPayload)
                                           .dataType(PUBLISHER_DATA_TYPE)
                                           .build();
    }

    public static MockPayload mockPayload(Integer id, String code, Boolean isActive,
                                          MockPayload.SubClass more, List<MockPayload.SubListItem> items) {
        return MockPayload.builder()
                .id(id)
                .code(code)
                .isActive(isActive)
                .more(more)
                .items(items)
                .build();
    }

    public static MockPayload mockPayload(Integer id, String code) {
        return mockPayload(id, code, true, mockSubClass(), mockSubList(3));
    }

    public static List<Snapshot> mockSnapshotList(Integer size) {
        List<Snapshot> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(new Snapshot(i, PUBLISHER_EVENT_TYPE, mockEventPayload(mockPayload(i + 1, "code" + i, true, mockSubClass("some info " + i), mockSubList(3, "some detail for code" + i)))));
        }
        return list;
    }

    public static MockPayload.SubClass mockSubClass(String info) {
        return MockPayload.SubClass.builder().info(info).build();
    }

    private static MockPayload.SubClass mockSubClass() {
        return mockSubClass("Info something");
    }

    private static MockPayload.SubListItem mockSubListItem(String detail) {
        return MockPayload.SubListItem.builder().detail(detail).build();
    }

    public static List<MockPayload.SubListItem> mockSubList(Integer size, String detail) {
        List<MockPayload.SubListItem> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(mockSubListItem(detail + i));
        }
        return items;
    }

    private static List<MockPayload.SubListItem> mockSubList(Integer size) {
        return mockSubList(size, "Detail something ");
    }

}
