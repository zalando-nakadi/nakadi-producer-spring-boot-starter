package org.zalando.nakadiproducer.eventlog.impl.batcher;


import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.joining;
import static org.springframework.jdbc.core.namedparam.SqlParameterSource.TYPE_UNKNOWN;

/**
 * A helper class to simulate query batching for SQL statements which return data,
 * i.e. SELECT or anything with a RETURNING clause.
 * Inspired by https://javaranch.com/journal/200510/batching.html.
 * <p>
 * The idea here is to prepare prepared statements returning result sets of a common row type,
 * for several input batch sizes (e.g. 51, 11, 4, 1), and then split our actual input into these
 * sizes (mostly the largest one, the smaller ones are then used for the rest).
 * We then use the query*() methods to give the input to the DB, and compose the output
 * together into one stream/list.
 * <p>
 * Advantages:
 * - It's less round trips than sending each query one-by-one.
 * - Compared to building one statement for each input list, the DB only has a small
 * number of different prepared statements to look at and optimize.
 */
public class QueryStatementBatcher<T> {

    final private String templatePrefix;
    final private String templateSuffix;
    final private String templateRepeated;
    final private String templateSeparator;
    final private String templatePlaceholder;

    final private RowMapper<T> resultRowMapper;

    final List<SubTemplate> subTemplates;

    public QueryStatementBatcher(String templatePrefix, String templateRepeated, String templateSeparator, String templateSuffix, RowMapper<T> resultMapper) {
        this(templatePrefix, templateRepeated, "#", templateSeparator, templateSuffix, resultMapper);
    }

    QueryStatementBatcher(String templatePrefix, String templateRepeated, String templatePlaceholder, String templateSeparator, String templateSuffix, RowMapper<T> resultMapper) {
        this(templatePrefix, templateRepeated, templatePlaceholder, templateSeparator, templateSuffix, resultMapper, 51, 13, 4, 1);
    }

    QueryStatementBatcher(String templatePrefix, String templateRepeated, String templateSeparator, String templateSuffix, RowMapper<T> resultMapper,
                          int... templateSizes) {
        this(templatePrefix, templateRepeated, "#", templateSeparator, templateSuffix, resultMapper, templateSizes);
    }

    /**
     * @param templatePrefix
     * @param templateRepeated
     * @param templatePlaceholder
     * @param templateSeparator
     * @param templateSuffix
     * @param templateSizes       An descending ordered sequence of integers. Last one needs to be 1.
     */
    QueryStatementBatcher(String templatePrefix, String templateRepeated, String templatePlaceholder,
                          String templateSeparator, String templateSuffix, RowMapper<T> resultMapper,
                          int... templateSizes) {
        this.templatePrefix = templatePrefix;
        this.templateSuffix = templateSuffix;
        this.templateRepeated = templateRepeated;
        this.templateSeparator = templateSeparator;
        this.templatePlaceholder = templatePlaceholder;
        this.resultRowMapper = resultMapper;
        this.subTemplates = IntStream.of(templateSizes)
                .mapToObj(size -> new SubTemplate(size, composeTemplate(size), templatePlaceholder))
                .collect(Collectors.toList());
    }

    String composeTemplate(int valueCount) {
        return IntStream.range(0, valueCount)
                .mapToObj(i -> templateRepeated.replace(templatePlaceholder, String.valueOf(i)))
                .collect(joining(templateSeparator, templatePrefix, templateSuffix));
    }

    public Stream<T> queryForStream(NamedParameterJdbcTemplate template,
                                    Stream<MapSqlParameterSource> repeatedInputs) {
        return queryForStream(template, new MapSqlParameterSource(), repeatedInputs);
    }

    public Stream<T> queryForStream(NamedParameterJdbcTemplate template,
                                    MapSqlParameterSource commonArguments,
                                    Stream<MapSqlParameterSource> repeatedInputs) {
        return queryForStreamRecursive(template, commonArguments, repeatedInputs, 0);
    }

    private Stream<T> queryForStreamRecursive(NamedParameterJdbcTemplate template,
                                              MapSqlParameterSource commonArguments,
                                              Stream<MapSqlParameterSource> repeatedInputs,
                                              int subTemplateIndex) {
        SubTemplate firstSubTemplate = subTemplates.get(subTemplateIndex);

        Stream<List<MapSqlParameterSource>> chunkedStream = chunkStream(repeatedInputs, firstSubTemplate.inputCount);
        return chunkedStream.flatMap(chunk -> {
            if (chunk.size() == firstSubTemplate.inputCount) {
                return firstSubTemplate.queryForStream(template, commonArguments, chunk, resultRowMapper);
            } else {
                return queryForStreamRecursive(template, commonArguments, chunk.stream(), subTemplateIndex + 1);
            }
        });
    }

    static class SubTemplate {
        final int inputCount;
        final String expandedTemplate;
        final String namePlaceholder;

        private SubTemplate(int inputCount, String expandedTemplate, String namePlaceholder) {
            this.inputCount = inputCount;
            this.expandedTemplate = expandedTemplate;
            this.namePlaceholder = namePlaceholder;
        }

        <T> Stream<T> queryForStream(NamedParameterJdbcTemplate template,
                                     MapSqlParameterSource commonArguments,
                                     List<MapSqlParameterSource> repeatedInputs,
                                     RowMapper<T> mapper) {
            if (repeatedInputs.size() != inputCount) {
                throw new IllegalArgumentException(String.format("input size = %s != %s = inputCount", repeatedInputs.size(), inputCount));
            }
            MapSqlParameterSource params = new MapSqlParameterSource();
            Stream.of(commonArguments.getParameterNames())
                    .forEach(name -> copyTypeAndValue(commonArguments, name, params, name));
            IntStream.range(0, inputCount)
                    .forEach(index -> {
                        MapSqlParameterSource input = repeatedInputs.get(index);
                        String textIndex = String.valueOf(index);
                        Stream.of(input.getParameterNames())
                                .forEach(name -> copyTypeAndValue(input, name,
                                        params, name.replace(namePlaceholder, textIndex)));
                    });

            return template.queryForStream(expandedTemplate, params, mapper);
        }

        private void copyTypeAndValue(MapSqlParameterSource source, String sourceName, MapSqlParameterSource target, String targetName) {
            target.addValue(targetName, source.getValue(sourceName));
            int type = source.getSqlType(sourceName);
            if (type != TYPE_UNKNOWN) {
                target.registerSqlType(targetName, type);
            }
            String typeName = source.getTypeName(sourceName);
            if (typeName != null) {
                target.registerTypeName(targetName, typeName);
            }
        }

        @Override
        public String toString() {
            return "SubTemplate{" +
                    "inputCount=" + inputCount +
                    ", expandedTemplate='" + expandedTemplate + '\'' +
                    ", namePlaceholder='" + namePlaceholder + '\'' +
                    '}';
        }
    }

    /**
     * Splits a stream into a stream of chunks of equal size, with possibly one final chunk of smaller size.
     * This is a terminal operation on {@code input} (it's spliterator is requested), but its elements are
     * only accessed when the return stream is processed.
     *
     * @param input
     * @param chunkSize the size of each chunk.
     * @param <T>       the type of elements in input.
     * @return a new stream of lists. The returned lists can be modified, but that
     * doesn't have any impact on the source of input.
     */
    static <T> Stream<List<T>> chunkStream(Stream<T> input, int chunkSize) {
        // inspired by https://stackoverflow.com/a/59164175/600500
        // I think there might be a way of optimizing this by actually using the
        // spliterator for chunking, but that seems to become more complicated.
        Spliterator<T> inputSpliterator = input.spliterator();
        int characteristics = inputSpliterator.characteristics()
                // these characteristics should reflect onto the chunked spliterator
                & (Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.CONCURRENT | Spliterator.DISTINCT)
                // the lists returned are always non-null (even if they might contain null elements)
                | Spliterator.NONNULL
                // not transferring characteristics: Spliterator.SORTED, Spliterator.SIZED, Spliterator.SUBSIZED
                ;
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<>() {
            Iterator<T> sourceIterator = Spliterators.iterator(inputSpliterator);

            @Override
            public boolean hasNext() {
                return sourceIterator.hasNext();
            }

            @Override
            public List<T> next() {
                if (!sourceIterator.hasNext()) {
                    throw new NoSuchElementException("no more elements!");
                }
                List<T> result = new ArrayList<T>(chunkSize);
                for (int i = 0; i < chunkSize && sourceIterator.hasNext(); i++) {
                    result.add(sourceIterator.next());
                }
                return result;
            }
        }, characteristics), false);
    }
}
