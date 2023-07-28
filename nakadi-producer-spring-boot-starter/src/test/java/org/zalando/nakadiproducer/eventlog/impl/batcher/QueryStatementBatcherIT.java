package org.zalando.nakadiproducer.eventlog.impl.batcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class QueryStatementBatcherIT extends BaseMockedExternalCommunicationIT {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUpTable() {
        jdbcTemplate.update("CREATE TABLE x (id SERIAL, a INT, b INT)", Map.of());
    }
    @AfterEach
    public void dropTable() {
        jdbcTemplate.update("DROP TABLE x;", Map.of());
    }

    @Test
    public void testStreamEvents() {
        QueryStatementBatcher<Integer> batcher = new QueryStatementBatcher<>(
                "INSERT INTO x (a, b) VALUES ", "(:a#, :b#)", " RETURNING id",
                (row, n) -> row.getInt("id"),
                51, 13, 4, 1);
        MapSqlParameterSource commonArguments = new MapSqlParameterSource();
        int expectedCount = 31;
        List<MapSqlParameterSource> repeatedInputs = IntStream.range(0, expectedCount)
                .mapToObj(i -> new MapSqlParameterSource()
                        .addValue("a#", i)
                        .addValue("b#", 5 * i))
                .collect(toList());

        List<Integer> resultList = batcher.queryForStream(
                jdbcTemplate, repeatedInputs.stream())
                .collect(toList());
        assertThat(resultList, hasSize(expectedCount));

        List<Integer> secondResultList = batcher.queryForStream(
                        jdbcTemplate, commonArguments, repeatedInputs.stream())
                .collect(toList());

        assertThat(secondResultList, hasSize(expectedCount));
        assertThat(secondResultList.get(0), is(expectedCount+1));
    }


}
