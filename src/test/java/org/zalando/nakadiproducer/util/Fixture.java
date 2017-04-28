package org.zalando.nakadiproducer.util;

import org.zalando.nakadiproducer.service.model.EventPayload;
import org.zalando.nakadiproducer.service.model.EventPayloadImpl;

import java.util.ArrayList;
import java.util.List;

public class Fixture {

    public static final String PUBLISHER_EVENT_OTHER_TYPE = "wholesale.different-event-type";
    public static final String PUBLISHER_EVENT_TYPE = "wholesale.some-publisher-change-event";
    public static final String PUBLISHER_DATA_TYPE = "nakadi:some-publisher";
    public static final String SINK_ID = "zalando-nakadi";

    public static EventPayload mockEventPayload(MockPayload mockPayload, String eventType) {
        return EventPayloadImpl.builder()
                .data(mockPayload)
                .eventType(eventType)
                .dataType(PUBLISHER_DATA_TYPE)
                .build();
    }

    public static EventPayload mockEventPayload(MockPayload mockPayload) {
        return mockEventPayload(mockPayload, PUBLISHER_EVENT_TYPE);
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

    public static List<MockPayload> mockPayloadList(Integer size) {
        List<MockPayload> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(mockPayload(i + 1, "code" + i, true, mockSubClass("some info " + i), mockSubList(3, "some detail for code" + i)));
        }
        return list;
    }

    public static MockPayload.SubClass mockSubClass(String info) {
        return MockPayload.SubClass.builder().info(info).build();
    }

    public static MockPayload.SubClass mockSubClass() {
        return mockSubClass("Info something");
    }

    public static MockPayload.SubListItem mockSubListItem(String detail) {
        return MockPayload.SubListItem.builder().detail(detail).build();
    }

    public static MockPayload.SubListItem mockSubListItem() {
        return mockSubListItem("Detail something");
    }

    public static List<MockPayload.SubListItem> mockSubList(Integer size, String detail) {
        List<MockPayload.SubListItem> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(mockSubListItem(detail + i));
        }
        return items;
    }

    public static List<MockPayload.SubListItem> mockSubList(Integer size) {
        return mockSubList(size, "Detail something ");
    }

}
