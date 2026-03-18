/*
 * Copyright 2026 Dementhius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package quaero.it;

import java.util.List;
import java.util.Map;

import javax.persistence.TypedQuery;

import org.junit.jupiter.api.Test;

import quaero.components.values.SelectOperatorType;
import quaero.query.Query;
import quaero.query.QueryBuilder;
import quaero.utils.QueryUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SELECT projections, aggregates, ORDER BY and pagination.
 */
public class QueryExecutorSelectAndOrderIT extends QuaeroItBase {

    // ─── SELECT ───────────────────────────────────────────────────────────────

    @Test
    void selectSingleField_returnsOneKeyPerRow() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(3, rows.size());
        rows.forEach(r -> {
            assertTrue(r.containsKey("name"));
            assertEquals(1, r.size());
        });
    }

    @Test
    void selectMultipleFields_allKeysPresent() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .select("price").as("price")
                .select("active").as("active")
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(3, rows.size());
        rows.forEach(r -> {
            assertTrue(r.containsKey("name"));
            assertTrue(r.containsKey("price"));
            assertTrue(r.containsKey("active"));
        });
    }

    @Test
    void countAggregate_returnsCorrectCount() {
        final Query q = QueryBuilder.builder("Product")
                .select("id").count().as("total")
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(1, rows.size());
        assertEquals(3L, rows.get(0).get("total"));
    }

    @Test
    void countDistinct_withGroupBy_returnsGroupCounts() {
        // Count products per active flag
        final Query q = QueryBuilder.builder("Product")
                .select("active").as("active").groupBy()
                .select("id").countDistinct().as("cnt")
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        // Two groups: active=true (2 rows), active=false (1 row)
        assertEquals(2, rows.size());
    }

    // ─── ORDER BY ─────────────────────────────────────────────────────────────

    @Test
    void orderAsc_sortsByPriceAscending() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .select("price").as("price")
                .orderAsc("price")
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(3, rows.size());
        assertEquals("Corolla", rows.get(0).get("name")); // 200
        assertEquals("Civic",   rows.get(1).get("name")); // 250
        assertEquals("Prius",   rows.get(2).get("name")); // 350
    }

    @Test
    void orderDesc_sortsByPriceDescending() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .orderDesc("price")
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals("Prius",   rows.get(0).get("name")); // 350
        assertEquals("Civic",   rows.get(1).get("name")); // 250
        assertEquals("Corolla", rows.get(2).get("name")); // 200
    }

    @Test
    void orderAsc_byAggregate() {
        // Order by COUNT(id) ASC — SelectStep doesn't expose the aggregate overload,
        // so we keep a reference to the builder and call it there.
        final QueryBuilder qb = QueryBuilder.builder("Product");
        qb.select("active").as("active").groupBy();
        qb.select("id").count().as("cnt");
        final Query q = qb.orderAsc("id", SelectOperatorType.COUNT).build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(2, rows.size());
        // Group with 1 product (active=false) should come first
        assertEquals(1L, rows.get(0).get("cnt"));
        assertEquals(2L, rows.get(1).get("cnt"));
    }

    // ─── PAGINATION ───────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void pagination_firstPage_returnsPageSize() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .orderAsc("name")
                .page(0, 2)
                .build();

        final TypedQuery<?> tq = queryExecutor.doQuery(q);
        tq.setFirstResult(q.getPageIndex() * q.getPageSize());
        tq.setMaxResults(q.getPageSize());

        final List<Map<String, Object>> rows =
                QueryUtils.tupleToMapList((List<javax.persistence.Tuple>) tq.getResultList());

        assertEquals(2, rows.size());
        assertEquals("Civic",   rows.get(0).get("name")); // alphabetical
        assertEquals("Corolla", rows.get(1).get("name"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pagination_secondPage_returnsRemainder() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .orderAsc("name")
                .page(1, 2)
                .build();

        final TypedQuery<?> tq = queryExecutor.doQuery(q);
        tq.setFirstResult(q.getPageIndex() * q.getPageSize());
        tq.setMaxResults(q.getPageSize());

        final List<Map<String, Object>> rows =
                QueryUtils.tupleToMapList((List<javax.persistence.Tuple>) tq.getResultList());

        assertEquals(1, rows.size());
        assertEquals("Prius", rows.get(0).get("name"));
    }
}
