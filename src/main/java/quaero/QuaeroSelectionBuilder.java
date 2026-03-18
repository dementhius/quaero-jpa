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
package quaero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import quaero.query.QuerySelectObject;
import quaero.utils.CoercionMode;

/**
 * Translates a list of {@link QuerySelectObject} into JPA {@link Selection}
 * and GROUP BY {@link Expression} lists.
 *
 * <p>Inject this bean when you need dynamic SELECT or GROUP BY clauses inside
 * your own JPA criteria queries, without using the full {@link QueryExecutor}
 * pipeline.
 *
 * <pre>{@code
 * @Autowired QuaeroSelectionBuilder selectionBuilder;
 *
 * CriteriaBuilder cb = em.getCriteriaBuilder();
 * CriteriaQuery<Tuple> q = cb.createTupleQuery();
 * Root<Sale> root = q.from(Sale.class);
 *
 * List<Selection<?>> selects = selectionBuilder.buildSelections(querySelects, root, q, cb);
 * List<Expression<?>> groupBy = selectionBuilder.buildGroupBy(querySelects, root, q, cb);
 *
 * q.multiselect(selects);
 * if (!groupBy.isEmpty()) q.groupBy(groupBy);
 * }</pre>
 *
 * <h3>Join pre-building</h3>
 * <p>When a {@link QuerySelectObject} declares explicit join types (LEFT, RIGHT,
 * INNER) via {@code joinTypes}, this builder pre-registers those joins on the
 * root before resolving the expressions. This mirrors the behaviour of
 * {@link QueryExecutor} and ensures the correct SQL join type is emitted.
 * All joins are built in a first pass before any expression is resolved, which
 * prevents duplicate joins across selects that share path prefixes.
 *
 * <p>If you have pre-built joins from a {@code paramJoinTypes} configuration,
 * pass them via the {@code existingJoins} parameter of the overloaded methods
 * so they are reused rather than duplicated.
 */
@Component
public class QuaeroSelectionBuilder {

    @Autowired
    private Map<String, EntityType<?>> entities;
    @Autowired
    private Map<String, ManagedType<?>> managedTypes;
    @Autowired
    private EntityManagerFactory emf;

    // ─── buildSelections ──────────────────────────────────────────────────────

    /**
     * Build SELECT expressions using {@link CoercionMode#STRICT}.
     */
    public List<Selection<?>> buildSelections(final List<QuerySelectObject> selects,
            final Root<?> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        return buildSelections(CoercionMode.STRICT, selects, root, query, cb, new HashMap<>());
    }

    /**
     * Build SELECT expressions using the specified {@link CoercionMode}.
     */
    public List<Selection<?>> buildSelections(final CoercionMode mode,
            final List<QuerySelectObject> selects, final Root<?> root,
            final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        return buildSelections(mode, selects, root, query, cb, new HashMap<>());
    }

    /**
     * Build SELECT expressions, seeding the join cache with {@code existingJoins}
     * to avoid duplicating joins that were already pre-built (e.g. from a
     * {@code paramJoinTypes} configuration).
     */
    public List<Selection<?>> buildSelections(final CoercionMode mode,
            final List<QuerySelectObject> selects, final Root<?> root,
            final CriteriaQuery<?> query, final CriteriaBuilder cb,
            final Map<String, Join<?, ?>> existingJoins) {
        if (selects == null || selects.isEmpty()) {
            return Collections.emptyList();
        }

        // Phase 1 — pre-build all joins declared on the selects.
        final Metamodel metamodel = emf.getMetamodel();
        final Map<String, Join<?, ?>> joinMaps = new HashMap<>(existingJoins);
        for (final QuerySelectObject select : selects) {
            final Map<String, Join<?, ?>> tmp = select.defineJoin(root, query, metamodel, joinMaps);
            if (tmp != null) {
                joinMaps.putAll(tmp);
            }
        }

        // Phase 2 — resolve expressions now that all joins are in place.
        final List<Selection<?>> result = new ArrayList<>();
        for (final QuerySelectObject select : selects) {
            final Selection<?> s = select.resolve(mode, root, query, cb, entities, managedTypes);
            if (s != null) {
                result.add(s);
            }
        }
        return result;
    }

    // ─── buildGroupBy ─────────────────────────────────────────────────────────

    /**
     * Build GROUP BY expressions using {@link CoercionMode#STRICT}.
     * Only selects with {@code groupBy = true} contribute to the result.
     */
    public List<Expression<?>> buildGroupBy(final List<QuerySelectObject> selects,
            final Root<?> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        return buildGroupBy(CoercionMode.STRICT, selects, root, query, cb);
    }

    /**
     * Build GROUP BY expressions using the specified {@link CoercionMode}.
     * Only selects with {@code groupBy = true} contribute to the result.
     */
    public List<Expression<?>> buildGroupBy(final CoercionMode mode,
            final List<QuerySelectObject> selects, final Root<?> root,
            final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        if (selects == null || selects.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Expression<?>> result = new ArrayList<>();
        for (final QuerySelectObject select : selects) {
            final Expression<?> g = select.resolveGroupBy(mode, root, query, cb, entities, managedTypes);
            if (g != null) {
                result.add(g);
            }
        }
        return result;
    }
}
