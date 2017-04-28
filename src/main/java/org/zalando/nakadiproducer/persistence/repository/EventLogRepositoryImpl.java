package org.zalando.nakadiproducer.persistence.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.core.types.Predicate;

import org.zalando.nakadiproducer.persistence.entity.EventLog;
import org.zalando.nakadiproducer.persistence.entity.QEventLog;

import org.springframework.data.jpa.repository.support.QueryDslRepositorySupport;

import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

public class EventLogRepositoryImpl extends QueryDslRepositorySupport
    implements EventLogCustomRepository {

    public EventLogRepositoryImpl() {
        super(EventLog.class);
    }

    @Override
    public List<EventLog> search(final Integer cursor, final String status, final int limit) {
        final QEventLog qEventLog = QEventLog.eventLog;
        final JPQLQuery query = from(qEventLog);

        final Predicate predicate = prepareSearchPredicate(cursor, status, qEventLog);

        query.where(predicate);
        query.limit(limit);
        query.orderBy(qEventLog.id.asc());

        return query.fetch();
    }

    private BooleanBuilder prepareSearchPredicate(final Integer cursor, final String status,
            final QEventLog qEventLog) {
        final BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (cursor != null) {
            booleanBuilder.and(qEventLog.id.gt(cursor));
        }

        if (!isNullOrEmpty(status)) {
            booleanBuilder.and(qEventLog.status.eq(status));
        }

        return booleanBuilder;
    }
}
