package org.zalando.nakadiproducer.eventlog.impl.batcher;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class QueryStatementBatcherTest
{
    @Test
    public void testComposeTemplateInsert() {
        QueryStatementBatcher<Integer> batcher = new QueryStatementBatcher<>(
                "INSERT INTO x (a, b) VALUES ",
                "(:a#, :b#)",
                ", ",
                " RETURNING id",
                (row, n) -> row.getInt("id")
        );

        assertThat(batcher.composeTemplate(1), is("INSERT INTO x (a, b) VALUES (:a0, :b0) RETURNING id"));
        assertThat(batcher.composeTemplate(2), is("INSERT INTO x (a, b) VALUES (:a0, :b0), (:a1, :b1) RETURNING id"));
    }

    @Test
    public void testComposeTemplateSelectWhere() {
        QueryStatementBatcher<Void> batcher = new QueryStatementBatcher<>(
                "SELECT a, b FROM x WHERE id IN (", ":id#", ", ", ")",
                (row, n) -> null
        );

        assertThat(batcher.composeTemplate(1), is("SELECT a, b FROM x WHERE id IN (:id0)"));
        assertThat(batcher.composeTemplate(2), is("SELECT a, b FROM x WHERE id IN (:id0, :id1)"));
    }

    @Test
    public void testCreateSubTemplates() {
        QueryStatementBatcher<Void> batcher = new QueryStatementBatcher<>(
                "SELECT a, b FROM x WHERE id IN (", ":id#", ", ", ")",
                (row, n) -> null,
                21, 6, 1);
        assertThat(batcher.subTemplates, hasSize(3));
        assertThat(batcher.subTemplates.get(0).expandedTemplate,
                is("SELECT a, b FROM x WHERE id IN (:id0, :id1, :id2, :id3, :id4, :id5, :id6, :id7," +
                        " :id8, :id9, :id10, :id11, :id12, :id13, :id14, :id15, :id16, :id17, :id18, :id19, :id20)") );
    }
}
