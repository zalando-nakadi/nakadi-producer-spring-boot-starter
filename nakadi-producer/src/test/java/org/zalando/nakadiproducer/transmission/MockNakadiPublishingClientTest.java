package org.zalando.nakadiproducer.transmission;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class MockNakadiPublishingClientTest {

    private static final String MY_EVENT_TYPE = "my event type";
    private static final String OTHER_EVENT_TYPE = "other event type";

    private MockNakadiPublishingClient mockNakadiPublishingClient = new MockNakadiPublishingClient();

    @Test
    public void returnsEmptyResultIfNoEventsHaveBeenSent() {
        assertThat(mockNakadiPublishingClient.getSentEvents("myEventType"), is(empty()));
    }

    @Test
    public void returnsOnlyThoseEventsOfTheGivenType() {
        mockNakadiPublishingClient.publish(MY_EVENT_TYPE, singletonList(new Event("anEvent")));
        mockNakadiPublishingClient.publish(OTHER_EVENT_TYPE, singletonList(new Event("anotherEvent")));

        assertThat(mockNakadiPublishingClient.getSentEvents(MY_EVENT_TYPE), contains("{\"attribute\":\"anEvent\"}"));
    }

    @Test
    public void concatenatesSubsequentlyPublishedEventLists() {
        mockNakadiPublishingClient.publish(MY_EVENT_TYPE,
            asList(new Event("event1"), new Event("event2"))
        );
        mockNakadiPublishingClient.publish(MY_EVENT_TYPE,
            asList(new Event("event3"), new Event("event4"))
        );

        assertThat(
            mockNakadiPublishingClient.getSentEvents(MY_EVENT_TYPE),
            contains(
                "{\"attribute\":\"event1\"}",
                "{\"attribute\":\"event2\"}",
                "{\"attribute\":\"event3\"}",
                "{\"attribute\":\"event4\"}"
            )
        );
    }

    @Test
    public void deletesAllEventsOnClear() {
        mockNakadiPublishingClient.publish(MY_EVENT_TYPE, singletonList(new Event("event1")));
        mockNakadiPublishingClient.clearSentEvents();

        assertThat(mockNakadiPublishingClient.getSentEvents(MY_EVENT_TYPE), is(empty()));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Event {
        private String attribute;
    }

}