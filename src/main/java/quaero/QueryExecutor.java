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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import quaero.query.Query;
import quaero.query.QueryJoinObject;
import quaero.query.QueryJoinParamsObject;
import quaero.query.QueryJoinTypesObject;
import quaero.query.QueryMultiJoinObject;
import quaero.query.QuerySelectObject;
import quaero.security.QuaeroSecurityValidator;
import quaero.utils.CoercionMode;
import quaero.utils.QueryUtils;

@Component
public class QueryExecutor {

    private final Logger logger = Logger.getLogger(QueryExecutor.class.getName());

    @Autowired
    protected ApplicationContext appContext;
    @Autowired
    public Map<String, EntityType<?>> entities;
    @Autowired
    public Map<String, ManagedType<?>> managedTypes;
    @PersistenceContext
    private EntityManager em;
    @Autowired
    private QuaeroSecurityValidator securityValidator;
    @Autowired
    private QuaeroPredicateBuilder predicateBuilder;
    @Autowired
    private QuaeroSelectionBuilder selectionBuilder;
    @Autowired
    private QuaeroOrderBuilder orderBuilder;

    public TypedQuery<Tuple> doQuery(final Query queryObj) {
        securityValidator.validate(queryObj);
        try {
            return buildQuery(em, queryObj);
        } finally {
            em.close();
        }
    }

    private TypedQuery<Tuple> buildQuery(final EntityManager em, final Query queryObj) {
        // ── Root entity ───────────────────────────────────────────────────────
        final EntityType<?> tableEntity = entities.get(queryObj.getTableName());
        if (tableEntity == null) {
            throw new IllegalArgumentException("generateCustomQuery. Entity not found: " + queryObj.getTableName());
        }

        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Tuple> tupleQu = cb.createTupleQuery();

        final String alias = queryObj.getTableAlias();
        final Root<?> root = (Root<?>) tupleQu.from(tableEntity).alias(
                (alias != null && !alias.trim().isEmpty()) ? alias.trim() : queryObj.getTableName());

        final CoercionMode mode = queryObj.getCoercionMode();

        // Map of all roots (main + joined), keyed by table name
        final Map<String, From<?, ?>> rootMap = new HashMap<>();
        rootMap.put(queryObj.getTableName(), root);

        // ── Pre-build joins from paramJoinTypes ───────────────────────────────
        final Map<String, Join<?, ?>> joinMaps = buildParamJoinTypes(
                em.getMetamodel(), root, queryObj.getParamJoinTypes());

        // ── Accumulators ─────────────────────────────────────────────────────
        final List<Selection<?>> selections = new ArrayList<>();
        final List<Expression<?>> groupByExps = new ArrayList<>();
        Predicate wherePr = null;

        // ── Dynamic joins (single main table) ────────────────────────────────
        final List<From<?, ?>> joinRoots = new ArrayList<>();
        if (queryObj.getDynamicJoins() != null) {
            for (final QueryJoinObject joinObj : queryObj.getDynamicJoins()) {
                if (joinObj.getJoinParamTuples() == null || joinObj.getJoinParamTuples().length == 0) {
                    continue;
                }

                final From<?, ?> mainRoot = rootMap.get(joinObj.getMainTableName());
                if (mainRoot == null) {
                    throw new IllegalArgumentException("DynamicJoin. Main table not found: " + joinObj.getMainTableName());
                }

                final From<?, ?> joinRoot = createJoinRoot(tupleQu, joinObj.getJoinTableName(), joinObj.getJoinTableAlias());
                rootMap.put(joinObj.getJoinTableName(), joinRoot);
                joinRoots.add(joinRoot);

                wherePr = andPredicate(cb, wherePr, buildJoinPredicate(mode, joinObj, joinRoot, mainRoot, tupleQu, cb));
                wherePr = andPredicate(cb, wherePr,
                        predicateBuilder.build(mode, joinObj.getJoinFilter(), joinRoot, tupleQu, cb));

                collectSelections(mode, joinObj.getJoinSelects(), joinRoot, tupleQu, cb, selections, groupByExps);
            }
        }

        // ── Dynamic multi-joins (each param carries its own mainTableName) ────
        final List<From<?, ?>> multiJoinRoots = new ArrayList<>();
        if (queryObj.getDynamicJoinsMultiple() != null) {
            for (final QueryMultiJoinObject multiJoin : queryObj.getDynamicJoinsMultiple()) {
                if (multiJoin.getJoinParamTuples() == null || multiJoin.getJoinParamTuples().length == 0) {
                    continue;
                }

                final From<?, ?> joinRoot = createJoinRoot(tupleQu, multiJoin.getJoinTableName(), multiJoin.getJoinTableAlias());
                rootMap.put(multiJoin.getJoinTableName(), joinRoot);
                multiJoinRoots.add(joinRoot);

                wherePr = andPredicate(cb, wherePr, buildMultiJoinPredicate(mode, multiJoin, joinRoot, rootMap, tupleQu, cb));
                wherePr = andPredicate(cb, wherePr,
                        predicateBuilder.build(mode, multiJoin.getJoinFilter(), joinRoot, tupleQu, cb));

                collectSelections(mode, multiJoin.getJoinSelects(), joinRoot, tupleQu, cb, selections, groupByExps);
            }
        }

        // ── Main selects (joins pre-built inside selectionBuilder) ────────────
        if (queryObj.getSelects() != null) {
            selections.addAll(selectionBuilder.buildSelections(mode, queryObj.getSelects(), root, tupleQu, cb, joinMaps));
            groupByExps.addAll(selectionBuilder.buildGroupBy(mode, queryObj.getSelects(), root, tupleQu, cb));
        }

        // ── Main WHERE ────────────────────────────────────────────────────────
        wherePr = andPredicate(cb, wherePr, predicateBuilder.build(mode, queryObj.getFilter(), root, tupleQu, cb));

        // ── Assemble CriteriaQuery ────────────────────────────────────────────
        if (selections.isEmpty()) {
            return null;
        }

        final CriteriaQuery<Tuple> selectQuery = selections.size() == 1
                ? tupleQu.select(cb.tuple(selections.toArray(new Selection[0])))
                : tupleQu.multiselect(selections);

        if (Boolean.TRUE.equals(queryObj.getDistinctResults())) {
            selectQuery.distinct(true);
        }
        if (wherePr != null) {
            selectQuery.where(wherePr);
        }
        if (!groupByExps.isEmpty()) {
            selectQuery.groupBy(groupByExps);
        }

        // ── ORDER BY ──────────────────────────────────────────────────────────
        if (queryObj.getOrders() != null && !queryObj.getOrders().isEmpty()) {
            final List<From<?, ?>> allRoots = buildAllRoots(root, joinRoots, multiJoinRoots);
            final List<Order> orders = orderBuilder.build(mode, queryObj.getOrders(), allRoots, tupleQu, cb);
            if (!orders.isEmpty()) {
                selectQuery.orderBy(orders);
            }
        }

        return em.createQuery(selectQuery);
    }

    // =========================================================================
    // JOIN HELPERS
    // =========================================================================

    private From<?, ?> createJoinRoot(final CriteriaQuery<Tuple> query, final String tableName, final String tableAlias) {
        final EntityType<?> entity = entities.get(tableName);
        if (entity == null) {
            throw new IllegalArgumentException("Join entity not found: " + tableName);
        }
        final From<?, ?> joinRoot = query.from(entity);
        if (tableAlias != null && !StringUtils.isBlank(tableAlias)) {
            joinRoot.alias(tableAlias);
        }
        return joinRoot;
    }

    private Predicate buildJoinPredicate(final CoercionMode mode, final QueryJoinObject joinObj,
            final From<?, ?> joinRoot, final From<?, ?> mainRoot, final CriteriaQuery<Tuple> query,
            final CriteriaBuilder cb) {
        Predicate result = null;
        for (final QueryJoinParamsObject params : joinObj.getJoinParamTuples()) {
            final Expression<?> mainExpr = params.getMainParam().resolve(mode, mainRoot, query, cb, entities, managedTypes);
            final Expression<?> joinExpr = params.getJoinParam().resolve(mode, joinRoot, query, cb, entities, managedTypes);
            result = andPredicate(cb, result, cb.equal(mainExpr, joinExpr));
        }
        return result;
    }

    private Predicate buildMultiJoinPredicate(final CoercionMode mode, final QueryMultiJoinObject multiJoin,
            final From<?, ?> joinRoot, final Map<String, From<?, ?>> rootMap, final CriteriaQuery<Tuple> query,
            final CriteriaBuilder cb) {
        Predicate result = null;
        for (final QueryJoinParamsObject params : multiJoin.getJoinParamTuples()) {
            final From<?, ?> mainRoot = rootMap.get(params.getMainTableName());
            if (mainRoot == null) {
                throw new IllegalArgumentException("MultiJoin. Main table not found: " + params.getMainTableName());
            }
            final Expression<?> mainExpr = params.getMainParam().resolve(mode, mainRoot, query, cb, entities, managedTypes);
            final Expression<?> joinExpr = params.getJoinParam().resolve(mode, joinRoot, query, cb, entities, managedTypes);
            result = andPredicate(cb, result, cb.equal(mainExpr, joinExpr));
        }
        return result;
    }

    private Map<String, Join<?, ?>> buildParamJoinTypes(final Metamodel metamodel, final Root<?> root,
            final List<QueryJoinTypesObject> paramJoinTypes) {
        final Map<String, Join<?, ?>> joinMaps = new HashMap<>();
        if (paramJoinTypes == null || paramJoinTypes.isEmpty()) {
            return joinMaps;
        }

        for (final QueryJoinTypesObject qjt : paramJoinTypes) {
            final String param = qjt.getParam();
            final List<QueryJoinTypesObject.QuaeroJoinType> joinTypes = qjt.getJoinType();

            if (param == null || param.isEmpty() || joinTypes == null || joinTypes.isEmpty()) {
                continue;
            }

            if (param.contains(QueryUtils.ENTITY_FIELD_SEPARATOR)) {
                buildNestedJoin(metamodel, root, param, joinTypes, joinMaps);
            } else {
                joinMaps.put(param, root.join(param, QueryUtils.getJoinType(joinTypes.get(0))));
            }
        }
        return joinMaps;
    }

    private void buildNestedJoin(final Metamodel metamodel, final Root<?> root, final String param,
            final List<QueryJoinTypesObject.QuaeroJoinType> joinTypes, final Map<String, Join<?, ?>> joinMaps) {
        final String[] parts = StringUtils.splitByWholeSeparator(param, QueryUtils.ENTITY_FIELD_SEPARATOR);
        String prevParam = null;
        Class<?> prevEntity = null;
        String currentLevel = "";

        for (int i = 0; i < parts.length; i++) {
            currentLevel = currentLevel.isEmpty() ? parts[i] : currentLevel + QueryUtils.ENTITY_FIELD_SEPARATOR + parts[i];

            if (joinTypes.size() <= i) {
                break;
            }

            final JoinType joinType = QueryUtils.getJoinType(joinTypes.get(i));
            final Attribute<?, ?> attribute = prevParam == null
                    ? root.getModel().getAttribute(parts[i])
                    : metamodel.entity(prevEntity).getAttribute(parts[i]);
            prevEntity = attribute.getJavaType();

            if (!joinMaps.containsKey(currentLevel)) {
                if (QueryUtils.isJoinParameter(attribute)) {
                    final Join<Object, Object> join = prevParam == null
                            ? root.join(parts[i], joinType)
                            : joinMaps.get(prevParam).join(parts[i], joinType);
                    joinMaps.put(currentLevel, join);
                }
            } else {
                final Join<?, ?> existing = joinMaps.get(currentLevel);
                if (!existing.getJoinType().equals(joinType)) {
                    logger.warning("buildNestedJoin. Cannot override join type for '" + currentLevel
                            + "' from " + existing.getJoinType() + " to " + joinType);
                }
            }
            prevParam = currentLevel;
        }
    }

    // =========================================================================
    // SMALL UTILITIES
    // =========================================================================

    /** Null-safe AND accumulator — avoids the repetitive if/else pattern. */
    private Predicate andPredicate(final CriteriaBuilder cb, final Predicate existing, final Predicate next) {
        if (next == null) {
            return existing;
        }
        if (existing == null) {
            return next;
        }
        return cb.and(existing, next);
    }

    private List<From<?, ?>> buildAllRoots(final From<?, ?> root, final List<From<?, ?>> joinRoots,
            final List<From<?, ?>> multiJoinRoots) {
        final List<From<?, ?>> all = new ArrayList<>();
        all.add(root);
        all.addAll(joinRoots);
        all.addAll(multiJoinRoots);
        return all;
    }

    /**
     * Resolve join-level selects ({@code QuerySelectObject[]}) against a join root.
     * This path does <em>not</em> pre-build join types because join roots are
     * already fully resolved by the time their selects are collected.
     */
    private void collectSelections(final CoercionMode mode, final QuerySelectObject[] selectObjects,
            final From<?, ?> from, final CriteriaQuery<Tuple> query, final CriteriaBuilder cb,
            final List<Selection<?>> selections, final List<Expression<?>> groupByExps) {
        if (selectObjects == null) {
            return;
        }
        for (final QuerySelectObject sel : selectObjects) {
            final Selection<?> s = sel.resolve(mode, from, query, cb, entities, managedTypes);
            if (s != null) {
                selections.add(s);
            }
            final Expression<?> g = sel.resolveGroupBy(mode, from, query, cb, entities, managedTypes);
            if (g != null) {
                groupByExps.add(g);
            }
        }
    }
}
