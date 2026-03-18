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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import quaero.components.filter.FilterArrayObject;
import quaero.components.filter.FilterSimpleObject;
import quaero.query.Query;
import quaero.query.QueryBuilder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for WHERE-clause operators.
 *
 * <p>Test data (from {@link QuaeroItBase}):
 * <pre>
 *   Corolla — price 200, active true,  brand Toyota
 *   Prius   — price 350, active true,  brand Toyota
 *   Civic   — price 250, active false, brand Honda
 * </pre>
 */
public class QueryExecutorFilterIT extends QuaeroItBase {

    @Test
    void filterEqual_stringField_returnsMatchingRow() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .filterEqual("name", "Corolla")
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(1, rows.size());
        assertEquals("Corolla", rows.get(0).get("name"));
    }

    @Test
    void filterNotEqual_excludesMatchedRow() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .filter(FilterSimpleObject.notEqual("name", "Civic"))
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(2, rows.size());
        assertTrue(rows.stream().noneMatch(r -> "Civic".equals(r.get("name"))));
    }

    @Test
    void filterLike_caseInsensitive_matchesSubstring() {
        // LIKE is uppercased on both sides in Quaero — matches "PRIUS" anywhere
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .filterLike("name", "%RIUS%")
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(1, rows.size());
        assertEquals("Prius", rows.get(0).get("name"));
    }

    @Test
    void filterGreaterThan_numericField() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .filterGreaterThan("price", new BigDecimal("300.00"))
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(1, rows.size());
        assertEquals("Prius", rows.get(0).get("name"));
    }

    @Test
    void filterLessThanOrEqual_numericField() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .filter(FilterSimpleObject.lessThanOrEqual("price", new BigDecimal("200.00")))
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(1, rows.size());
        assertEquals("Corolla", rows.get(0).get("name"));
    }

    @Test
    void filterIn_multipleValues() {
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .filterIn("name", "Corolla", "Civic")
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(2, rows.size());
    }

    @Test
    void filterIsNull_isNotNull() {
        // All products have a non-null name, so isNull returns 0 rows
        final Query qNull = QueryBuilder.builder("Product")
                .select("name").as("name")
                .filterIsNull("name")
                .build();
        assertEquals(0, executeAsMap(qNull).size());

        // isNotNull returns all 3
        final Query qNotNull = QueryBuilder.builder("Product")
                .select("name").as("name")
                .filterIsNotNull("name")
                .build();
        assertEquals(3, executeAsMap(qNotNull).size());
    }

    @Test
    void filterAnd_compositeCondition() {
        // active = true AND price > 300 → only Prius
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .filterEqual("active", Boolean.TRUE)
                .filterGreaterThan("price", new BigDecimal("300.00"))
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(1, rows.size());
        assertEquals("Prius", rows.get(0).get("name"));
    }

    @Test
    void filterOr_returnsUnion() {
        // name = "Corolla" OR name = "Civic"
        final Query q = new Query();
        q.setTableName("Product");
        q.setSelects(java.util.Collections.singletonList(
                buildSelect("name", "name")));
        q.setFilter(FilterArrayObject.or(
                FilterSimpleObject.equal("name", "Corolla"),
                FilterSimpleObject.equal("name", "Civic")));

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(2, rows.size());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private quaero.query.QuerySelectObject buildSelect(final String field, final String alias) {
        final quaero.components.select.SelectSimpleObject simple =
                new quaero.components.select.SelectSimpleObject();
        simple.setField(field);
        final quaero.query.QuerySelectObject sel = new quaero.query.QuerySelectObject();
        sel.setField(simple);
        sel.setAlias(alias);
        return sel;
    }
}
