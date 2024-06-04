package org.zalando.nakadiproducer.eventlog.impl.eidgenerator;

import java.util.UUID;
import org.zalando.nakadiproducer.eventlog.EidGeneratorStrategy;

public class NoOpEidGeneratorStrategy implements EidGeneratorStrategy {

  @Override
  public UUID generateEid() {
    return null;
  }
}
