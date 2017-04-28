package org.zalando.nakadiproducer.util;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MockPayload {
    private Integer id;

    private String code;

    private boolean isActive = true;

    private SubClass more;

    private List<SubListItem> items;

    @Builder(toBuilder = true)
    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubClass {
        private String info;
    }

    @Builder(toBuilder = true)
    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubListItem {
        private String detail;
    }
}
