package org.zalando.nakadiproducer.eventlog;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EidGeneratorStrategyTest {

    public static final int UUID_VERSION_RANDOM = 4;

    @Test
    public void noopStrategyReturnsNull() {
        UUID eid = EidGeneratorStrategy.noop().generateEid();
        assertNull(eid);
    }

    @Test
    public void randomStrategyReturnsV4UUID() {
        UUID eid = EidGeneratorStrategy.random().generateEid();
        assertNotNull(eid);
        assertEquals(UUID_VERSION_RANDOM, eid.version());
    }

    @Test
    public void randomStrategyReturnsDifferentUUIDs() {
        EidGeneratorStrategy strategy = EidGeneratorStrategy.random();
        UUID eid1 = strategy.generateEid();
        UUID eid2 = strategy.generateEid();
        assertNotEquals(eid1, eid2);
    }
}
