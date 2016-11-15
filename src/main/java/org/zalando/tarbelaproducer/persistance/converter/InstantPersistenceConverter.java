package org.zalando.tarbelaproducer.persistance.converter;

import java.sql.Timestamp;
import java.time.Instant;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class InstantPersistenceConverter implements AttributeConverter<Instant, Timestamp> {
    @Override
    public Timestamp convertToDatabaseColumn(final Instant attribute) {
        return attribute == null ? null : new Timestamp(attribute.toEpochMilli());
    }

    @Override
    public Instant convertToEntityAttribute(final Timestamp dbData) {
        return dbData == null ? null : dbData.toInstant();
    }
}
