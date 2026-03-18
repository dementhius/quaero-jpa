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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.EntityType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import quaero.QuaeroOrderBuilder;
import quaero.QuaeroPredicateBuilder;
import quaero.QuaeroSelectionBuilder;
import quaero.components.filter.FilterSimpleObject;
import quaero.query.QueryOrderObject;
import quaero.query.QuerySelectObject;
import quaero.components.select.SelectSimpleObject;
import quaero.utils.CoercionMode;
import quaero.utils.QueryUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the standalone builder beans:
 * {@link QuaeroPredicateBuilder}, {@link QuaeroSelectionBuilder},
 * {@link QuaeroOrderBuilder}.
 *
 * <p>Each test wires the builder directly into a hand-crafted JPA Criteria query,
 * verifying that the builders work outside of the {@link quaero.QueryExecutor} pipeline.
 */
public class QuaeroBuildersIT extends QuaeroItBase {

    @Autowired
    private QuaeroPredicateBuilder predicateBuilder;

    @Autowired
    private QuaeroSelectionBuilder selectionBuilder;

    @Autowired
    private QuaeroOrderBuilder orderBuilder;

    /** The entity map registered by {@code QuaeroConfiguration} — keyed by JPA entity name. */
    @Autowired
    private Map<String, EntityType<?>> entities;

    // ─── PredicateBuilder ─────────────────────────────────────────────────────

    @Test
    void predicateBuilder_standalone_filtersCorrectly() {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        final Root<?> root = cq.from(entities.get("Product"));
        root.alias("Product");

        // WHERE price > 300
        final Predicate where = predicateBuilder.build(
                FilterSimpleObject.greaterThan("price", new BigDecimal("300.00")),
                root, cq, cb);

        final Selection<?> sel = root.get("name").alias("name");
        cq.select(cb.tuple(sel)).where(where);

        final List<Map<String, Object>> rows =
                QueryUtils.tupleToMapList(em.createQuery(cq).getResultList());

        assertEquals(1, rows.size());
        assertEquals("Prius", rows.get(0).get("name"));
    }

    // ─── SelectionBuilder ────────────────────────────────────────────────────

    @Test
    void selectionBuilder_standalone_buildsSelections() {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        final Root<?> root = cq.from(entities.get("Product"));
        root.alias("Product");

        final List<QuerySelectObject> selects = Arrays.asList(
                querySelectOf("name", "name"),
                querySelectOf("price", "price"));

        final List<Selection<?>> selections =
                selectionBuilder.buildSelections(CoercionMode.STRICT, selects, root, cq, cb);

        assertEquals(2, selections.size());
        cq.multiselect(selections);

        final List<Map<String, Object>> rows =
                QueryUtils.tupleToMapList(em.createQuery(cq).getResultList());

        assertEquals(3, rows.size());
        rows.forEach(r -> {
            assertTrue(r.containsKey("name"));
            assertTrue(r.containsKey("price"));
        });
    }

    // ─── OrderBuilder ────────────────────────────────────────────────────────

    @Test
    void orderBuilder_standalone_appliesOrdering() {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        final Root<?> root = cq.from(entities.get("Product"));
        root.alias("Product");

        final Selection<?> sel = root.get("name").alias("name");
        cq.select(cb.tuple(sel));

        final QueryOrderObject orderObj = new QueryOrderObject();
        final SelectSimpleObject field = new SelectSimpleObject();
        field.setField("name");
        orderObj.setField(field);
        orderObj.setAscending(false); // DESC

        final List<Order> orders = orderBuilder.build(
                CoercionMode.STRICT, Collections.singletonList(orderObj), root, cq, cb);
        assertFalse(orders.isEmpty());
        cq.orderBy(orders);

        final List<Map<String, Object>> rows =
                QueryUtils.tupleToMapList(em.createQuery(cq).getResultList());

        assertEquals(3, rows.size());
        assertEquals("Prius",   rows.get(0).get("name")); // P > C alphabetically DESC
        assertEquals("Corolla", rows.get(1).get("name"));
        assertEquals("Civic",   rows.get(2).get("name"));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static QuerySelectObject querySelectOf(final String field, final String alias) {
        final SelectSimpleObject simple = new SelectSimpleObject();
        simple.setField(field);
        final QuerySelectObject sel = new QuerySelectObject();
        sel.setField(simple);
        sel.setAlias(alias);
        return sel;
    }
}
