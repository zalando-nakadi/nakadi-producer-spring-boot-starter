package org.zalando.nakadiproducer.eventlog.impl.batcher;


import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.jdbc.core.namedparam.SqlParameterSource.TYPE_UNKNOWN;
import static org.zalando.fahrschein.Preconditions.checkArgument;

/**
 * A helper class to simulate query batching for SQL statements which return data,
 * i.e. SELECT or anything with a RETURNING clause.
 * Inspired by <a href="https://javaranch.com/journal/200510/batching.html">Batching Select Statements in JDBC</a>
 * (by Jeanne Boyarski).
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

    public static final String DEFAULT_TEMPLATE_PLACEHOLDER = "#";
    public static final String DEFAULT_TEMPLATE_SEPARATOR = ", ";

    private static final int[] DEFAULT_TEMPLATE_SIZES = {51, 13, 4, 1};

    final private RowMapper<T> resultRowMapper;
    final List<SubTemplate> subTemplates;

    /**
     * Sets up a QueryStatementBatcher for a specific set of statements, composed from prefix, repeated part, (default separator) and suffix.
     * Sizes will be determined
     * @param templatePrefix A prefix which will be prepended to the repeated part of the query. It can contain
     *                      parameter placeholders as usual for NamedParameterJdbcTemplate.
     * @param templateRepeated The part of the query string which will be repeated according to the number of parameter sets.
     *                         The parameter placeholders in here should contain {@code "#"}.
     *                        Occurrences of this in the generated queries will
     *                        be separated by the default operator (a comma).
     * @param templateSuffix A suffix which will be used after the repeated part.  It can contain
     *                            parameter placeholders as usual for NamedParameterJdbcTemplate.
     * @param resultMapper A mapper which will be used to map the results of the queries (JDBC ResultSets) to whatever
     *                    output format is desired.
     */
    public QueryStatementBatcher(String templatePrefix, String templateRepeated, String templateSuffix, RowMapper<T> resultMapper) {
        this(templatePrefix, templateRepeated, DEFAULT_TEMPLATE_PLACEHOLDER, DEFAULT_TEMPLATE_SEPARATOR, templateSuffix, resultMapper, DEFAULT_TEMPLATE_SIZES);
    }

    QueryStatementBatcher(String templatePrefix, String templateRepeated, String templatePlaceholder, String templateSeparator, String templateSuffix, RowMapper<T> resultMapper) {
        this(templatePrefix, templateRepeated, templatePlaceholder, templateSeparator, templateSuffix, resultMapper, DEFAULT_TEMPLATE_SIZES);
    }

    QueryStatementBatcher(String templatePrefix, String templateRepeated, String templateSuffix, RowMapper<T> resultMapper,
                          int... templateSizes) {
        this(templatePrefix, templateRepeated, DEFAULT_TEMPLATE_PLACEHOLDER, DEFAULT_TEMPLATE_SEPARATOR, templateSuffix, resultMapper, templateSizes);
    }

    /**
     * Sets up a QueryStatementBatcher for a specific set of statements composed from prefix, repeated part, separator and suffix.
     * @param templatePrefix A prefix which will be prepended to the repeated part of the query. It can contain
     *                      parameter placeholders as usual for NamedParameterJdbcTemplate.
     * @param templateRepeated The part of the query string which will be repeated according to the number of parameter sets.
     *                         The parameter placeholders in here (if they vary between parameter sets) should contain the
     *                        templatePlaceholder.
     * @param templatePlaceholder This placeholder is to be used as part of the parameter names in the repeated templates.
     * @param templateSeparator This separator will be used between the repeated parts of the query.
     * @param templateSuffix A suffix which will be used after the repeated part.  It can contain
     *                            parameter placeholders as usual for NamedParameterJdbcTemplate.
     * @param resultMapper A mapper which will be used to map the results of the queries (JDBC ResultSets) to whatever
     *                    output format is desired.
     * @param templateSizes       A sequence of integers. Smallest one needs to be 1.
     *                     This indicates the sizes (number of parameter sets used) to be used for the individual queries.
     */
    QueryStatementBatcher(String templatePrefix, String templateRepeated, String templatePlaceholder,
                          String templateSeparator, String templateSuffix, RowMapper<T> resultMapper,
                          int... templateSizes) {
        this.resultRowMapper = resultMapper;

        sortDescending(templateSizes);
        checkArgument(templateSizes[templateSizes.length-1] == 1,
                "smallest template size is not 1!");
        this.subTemplates = IntStream.of(templateSizes)
                .mapToObj(size -> new SubTemplate(
                                        size,
                                        composeTemplate(size, templatePrefix, templateRepeated, templatePlaceholder,
                                                templateSeparator, templateSuffix),
                                        templatePlaceholder))
                .collect(toList());
    }

    static String composeTemplate(int valueCount, String prefix, String repeated, String placeholder, String separator, String suffix) {
        return IntStream.range(0, valueCount)
                        .mapToObj(i -> repeated.replace(placeholder, String.valueOf(i)))
                        .collect(joining(separator, prefix, suffix));
    }

    /**
     * Queries the database for a set of parameter sources, in an optimized way.
     * This version should be used if there are no parameters in the non-repeated part
     * of the query tempate.
     * @param database the DB connection in form of a spring NamedParameterJdbcTemplate.
     * @param repeatedInputs A stream of repeated inputs. The names of the parameters here
     *                       should contain the placeholder (by default "#").
     * @return A stream of results, one for each parameter source in the repeated input.
     */
    public Stream<T> queryForStream(NamedParameterJdbcTemplate database,
                                    Stream<MapSqlParameterSource> repeatedInputs) {
        return queryForStream(database, new MapSqlParameterSource(), repeatedInputs);
    }

    /**
     * Queries the database for a set of parameter sources, in an optimized way.
     * This version should be used if there are parameters in the non-repeated part
     * of the template.
     * @param database the DB connection in form of a spring NamedParameterJdbcTemplate.
     * @param commonArguments a parameter source for any template parameters in the
     *                       non-repeated part of the query (or parameters in the
     *                       repeated part which don't change between input).
     * @param repeatedInputs A stream of repeated inputs. The names of the parameters here
     *      *                       should contain the placeholder (by default "#").
     * @return A stream of results, one for each parameter source in the repeated input.
     */
    public Stream<T> queryForStream(NamedParameterJdbcTemplate database,
                                    MapSqlParameterSource commonArguments,
                                    Stream<MapSqlParameterSource> repeatedInputs) {
        return queryForStreamRecursive(database, commonArguments, repeatedInputs, 0);
    }

    private Stream<T> queryForStreamRecursive(NamedParameterJdbcTemplate database,
                                              MapSqlParameterSource commonArguments,
                                              Stream<MapSqlParameterSource> repeatedInputs,
                                              int subTemplateIndex) {
        SubTemplate firstSubTemplate = subTemplates.get(subTemplateIndex);

        Stream<List<MapSqlParameterSource>> chunkedStream = chunkStream(repeatedInputs, firstSubTemplate.inputCount);
        return chunkedStream.flatMap(chunk -> {
            if (chunk.size() == firstSubTemplate.inputCount) {
                return firstSubTemplate.queryForStream(database, commonArguments, chunk, resultRowMapper);
            } else {
                return queryForStreamRecursive(database, commonArguments, chunk.stream(), subTemplateIndex + 1);
            }
        });
    }

    public int update(NamedParameterJdbcTemplate database, Stream<MapSqlParameterSource> repeatedInputs) {
        return update(database, new MapSqlParameterSource(), repeatedInputs);
    }

    public int update(NamedParameterJdbcTemplate database,
                      MapSqlParameterSource commonArguments,
                      Stream<MapSqlParameterSource> repeatedInputs) {
        return updateRecursive(database, commonArguments, repeatedInputs, 0);
    }

    private int updateRecursive(NamedParameterJdbcTemplate database, MapSqlParameterSource commonArguments, Stream<MapSqlParameterSource> repeatedInputs, int subTemplateIndex) {
        SubTemplate firstSubTemplate = subTemplates.get(subTemplateIndex);

        Stream<List<MapSqlParameterSource>> chunkedStream = chunkStream(repeatedInputs, firstSubTemplate.inputCount);
        return chunkedStream.mapToInt(chunk -> {
            if (chunk.size() == firstSubTemplate.inputCount) {
                return firstSubTemplate.update(database, commonArguments, chunk);
            } else {
                return updateRecursive(database, commonArguments, chunk.stream(), subTemplateIndex + 1);
            }
        }).sum();
    }

    /**
     * This nested class handles a single "batch size".
     */
    static class SubTemplate {
        final int inputCount;
        final String expandedTemplate;
        final String namePlaceholder;

        private SubTemplate(int inputCount, String expandedTemplate, String namePlaceholder) {
            this.inputCount = inputCount;
            this.expandedTemplate = expandedTemplate;
            this.namePlaceholder = namePlaceholder;
        }

        <T> Stream<T> queryForStream(NamedParameterJdbcTemplate database,
                                     MapSqlParameterSource commonArguments,
                                     List<? extends MapSqlParameterSource> repeatedInputs,
                                     RowMapper<T> mapper) {
            MapSqlParameterSource params = expandParameters(commonArguments, repeatedInputs);

            return database.queryForStream(expandedTemplate, params, mapper);
        }

        private MapSqlParameterSource expandParameters(MapSqlParameterSource commonArguments, List<? extends MapSqlParameterSource> repeatedInputs) {
            checkArgument(repeatedInputs.size() == inputCount,
                    "input size = %s != %s = inputCount", repeatedInputs.size(), inputCount);
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
            return params;
        }

        int update(NamedParameterJdbcTemplate database,
                   MapSqlParameterSource commonArguments,
                   List<? extends MapSqlParameterSource> repeatedInputs) {
            MapSqlParameterSource params = expandParameters(commonArguments, repeatedInputs);
            return database.update(expandedTemplate, params);
        }

        private static void copyTypeAndValue(MapSqlParameterSource source, String sourceName,
                                             MapSqlParameterSource target, String targetName) {
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
     * @param input a stream of elements to be chunked.
     * @param chunkSize the size of each chunk.
     * @param <T>       the type of elements in input.
     * @return a new stream of lists. The returned lists can be modified, but that
     *              doesn't have any impact on the source of input.
     *              The stream is non-null, and preserves the ordered/immutable/concurrent/distinct
     *              properties of the input stream.
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

    private static void sortDescending(int[] templateSizes) {
        // there is no Arrays.sort with comparator (or with flag to tell "descending"), so we sort it normally and then reverse it.
        Arrays.sort(templateSizes);
        reverse(templateSizes);
    }

    private static void reverse(int[] array) {
        // https://stackoverflow.com/a/3523066/600500
        for(int left = 0, right = array.length -1; left < right; left++, right --) {
            int temp = array[left];
            array[left] = array[right];
            array[right] = temp;
        }
    }

}
