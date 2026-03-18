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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import quaero.components.select.SelectSimpleObject;
import quaero.query.Query;
import quaero.query.QueryBuilder;
import quaero.query.QueryJoinTypesObject;
import quaero.query.QueryJoinTypesObject.QuaeroJoinType;
import quaero.query.QuerySelectObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for join support — {@code paramJoinTypes} and implicit
 * nested-path traversal.
 */
public class QueryExecutorJoinIT extends QuaeroItBase {

    // ─── Implicit nested path (auto inner-join) ───────────────────────────────

    @Test
    void nestedPath_autoInnerJoin_fetchesBrandName() {
        // "brand.name" causes Hibernate to traverse the @ManyToOne association
        final Query q = QueryBuilder.builder("Product")
                .select("name").as("name")
                .select("brand.name", "INNER").as("brand.name")
                .orderAsc("name")
                .build();

        final List<Map<String, Object>> rows = executeAsMap(q);
        // All 3 products have a non-null brand (inner join), all should be returned
        assertEquals(3, rows.size());

        // Verify nested map structure produced by tupleToMapList
        final Map<String, Object> first = rows.get(0); // Civic (alphabetical)
        assertTrue(first.containsKey("name"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> brand = (Map<String, Object>) first.get("brand");
        assertNotNull(brand);
        assertTrue(brand.containsKey("name"));
    }

    // ─── paramJoinTypes — explicit LEFT JOIN ──────────────────────────────────

    @Test
    void paramJoinTypes_leftJoin_overridesDefaultJoinType() {
        // Declare a LEFT JOIN on the "brand" path so that products without a brand
        // would still appear. All our test products have a brand, so result count = 3.
        final QueryJoinTypesObject jt = new QueryJoinTypesObject();
        jt.setParam("brand");
        jt.setJoinType(Collections.singletonList(QuaeroJoinType.LEFT));

        final QuerySelectObject selName = selectObj("name", "name");
        final QuerySelectObject selBrand = selectObj("brand.name", "brand.name");

        final Query q = new Query();
        q.setTableName("Product");
        q.setParamJoinTypes(Collections.singletonList(jt));
        q.setSelects(Arrays.asList(selName, selBrand));

        final List<Map<String, Object>> rows = executeAsMap(q);
        assertEquals(3, rows.size());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static QuerySelectObject selectObj(final String field, final String alias) {
        final SelectSimpleObject simple = new SelectSimpleObject();
        simple.setField(field);
        final QuerySelectObject sel = new QuerySelectObject();
        sel.setField(simple);
        sel.setAlias(alias);
        return sel;
    }
}
