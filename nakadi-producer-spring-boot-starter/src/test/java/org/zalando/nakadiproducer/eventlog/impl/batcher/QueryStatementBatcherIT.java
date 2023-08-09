package org.zalando.nakadiproducer.eventlog.impl.batcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.zalando.nakadiproducer.BaseMockedExternalCommunicationIT;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class QueryStatementBatcherIT extends BaseMockedExternalCommunicationIT {

    private static final RowMapper<Integer> ID_ROW_MAPPER = (row, n) -> row.getInt("id");
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
        QueryStatementBatcher<Integer> batcher = createInsertPairsReturningIdBatcher();
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

    private static QueryStatementBatcher<Integer> createInsertPairsReturningIdBatcher() {
        return new QueryStatementBatcher<>(
                "INSERT INTO x (a, b) VALUES ", "(:a#, :b#)", " RETURNING id",
                ID_ROW_MAPPER,
                200, 51, 13, 4, 1);
    }
    private static QueryStatementBatcher<Void> createInsertPairsWithoutReturningBatcher() {
        return new QueryStatementBatcher<>(
                "INSERT INTO x (a, b) VALUES ", "(:a#, :b#)", "",
                null,
                200, 51, 13, 4, 1);
    }


    @Test
    //@Disabled("Running benchmarks takes too long.")
    public void benchmarkWithBatcher() {
        int totalCount = 5000;
        List<MapSqlParameterSource> inputs = prepareInputs(totalCount);
        Instant before = Instant.now();
        QueryStatementBatcher<Integer> batcher = createInsertPairsReturningIdBatcher();
        List<Integer> results = batcher.queryForStream(jdbcTemplate, inputs.stream()).collect(toList());
        Instant after = Instant.now();
        System.err.format("Inserting %s items took %s.\n", totalCount, Duration.between(before, after));
        System.out.println(results);
    }

    private static List<MapSqlParameterSource> prepareInputs(int totalCount) {
        return IntStream.range(0, totalCount)
                .mapToObj(i -> new MapSqlParameterSource()
                        .addValue("a#", 3 * i)
                        .addValue("b#", 5 * i))
                .collect(toList());
    }

    @Test
    @Disabled("Running benchmarks takes too long.")
    public void benchmarkWithoutBatcherSerial() {
        int totalCount = 5000;
        List<MapSqlParameterSource> inputs = prepareInputs(totalCount);
        Instant before = Instant.now();
        List<Integer> results = inputs.stream()
                .map(source -> jdbcTemplate.queryForObject(
                        "INSERT INTO x (a, b) VALUES (:a#, :b#) RETURNING id",
                        source, ID_ROW_MAPPER))
                .collect(toList());
        Instant after = Instant.now();
        System.err.format("Inserting %s items took %s.\n", totalCount, Duration.between(before, after));
        System.out.println(results);
    }

    @Test
    @Disabled("Running benchmarks takes too long.")
    public void benchmarkWithoutBatcherParallel() {
        int totalCount = 5000;
        List<MapSqlParameterSource> inputs = prepareInputs(totalCount);
        Instant before = Instant.now();
        List<Integer> results = inputs.parallelStream()
                .map(source -> jdbcTemplate.queryForObject(
                        "INSERT INTO x (a, b) VALUES (:a#, :b#) RETURNING id",
                        source, ID_ROW_MAPPER))
                .collect(toList());
        Instant after = Instant.now();
        System.err.format("Inserting %s items took %s.\n", totalCount, Duration.between(before, after));
        System.out.println(results);
    }

    @Test
    //@Disabled("Running benchmarks takes too long.")
    public void benchmarkBatchWithoutReturn() {
        int totalCount = 5000;
        List<MapSqlParameterSource> inputs = prepareInputs(totalCount);
        MapSqlParameterSource[] inputArray = inputs.toArray(new MapSqlParameterSource[0]);
        Instant before = Instant.now();
        int[] results = jdbcTemplate.batchUpdate(
                "INSERT INTO x (a, b) VALUES (:a#, :b#)",
                inputArray);
        Instant after = Instant.now();
        System.err.format("Inserting %s items took %s.\n", totalCount, Duration.between(before, after));
        System.out.println(Arrays.toString(results));
    }

    @Test
    //@Disabled("Running benchmarks takes too long.")
    public void benchmarkWithBatcherWithoutReturn() {
        int totalCount = 5000;
        List<MapSqlParameterSource> inputs = prepareInputs(totalCount);
        Instant before = Instant.now();
        QueryStatementBatcher<Void> batcher = createInsertPairsWithoutReturningBatcher();
        int updateCount = batcher.update(jdbcTemplate, inputs.stream());
        Instant after = Instant.now();
        System.err.format("Inserting %s items took %s.\n", totalCount, Duration.between(before, after));
        System.out.println(updateCount);
    }

}
