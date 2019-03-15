package org.zalando.nakadiproducer.event;

public class ExampleBusinessEvent {

    public static String EVENT_NAME = "example.business.event";

    private String content;

    public ExampleBusinessEvent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "ExampleBusinessEvent{" + "content='" + content + '\'' + '}';
    }
}
