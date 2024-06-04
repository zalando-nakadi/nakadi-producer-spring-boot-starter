package org.zalando.nakadiproducer.eventlog;

import java.util.UUID;

public interface EidGeneratorStrategy {

  UUID generateEid();

}
