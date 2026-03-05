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
package quaero.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import quaero.components.filter.FilterArrayObject;
import quaero.components.filter.FilterSimpleObject;
import quaero.components.filter.IFilter;
import quaero.components.select.ISelect;
import quaero.components.select.SelectSimpleObject;
import quaero.components.select.SelectValueObject;
import quaero.components.values.FilterOperatorType;
import quaero.components.values.SelectOperatorType;
import quaero.query.QueryJoinTypesObject.QuaeroJoinType;
import quaero.utils.CoercionMode;

/**
 * Fluent builder for {@link Query} — use this when constructing queries
 * programmatically from Java instead of deserializing them from JSON.
 *
 * <h3>Example</h3>
 * 
 * <pre>{@code
 * Query q = Query.builder("Sale").alias("sale").select("finalPrice").as("price")
 * 		.select("vehicle.trim.model.brand.name", "Inner", "Inner", "Inner", "Inner").as("brand").select("id").count()
 * 		.as("total").filterEqual("vehicle.powertrain.fuelType", "Electric").orderDesc("finalPrice").page(0, 20)
 * 		.build();
 * }</pre>
 */
public class QueryBuilder {

    // ─── State ────────────────────────────────────────────────────────────
    private final String tableName;
    private String tableAlias;
    private final List<QuerySelectObject> selects = new ArrayList<>();
    private final List<QueryOrderObject> orders = new ArrayList<>();
    private final List<IFilter> rootFilters = new ArrayList<>();
    private FilterArrayObject.FilterOperationType rootOperation = FilterArrayObject.FilterOperationType.AND;
    private Integer pageIndex;
    private Integer pageSize;
    private boolean distinct = false;

    private CoercionMode coercionMode = CoercionMode.STRICT;

    // ─── Entry point ──────────────────────────────────────────────────────

    private QueryBuilder(final String tableName) {
        this.tableName = tableName;
    }

    /**
     * Start building a query against the given JPA entity name.
     *
     * @param tableName JPA entity simple name (e.g. {@code "Sale"})
     */
    public static QueryBuilder builder(final String tableName) {
        return new QueryBuilder(tableName);
    }

    // ─── Root config ──────────────────────────────────────────────────────

    /** Optional alias for the root entity (default: tableName). */
    public QueryBuilder alias(final String alias) {
        this.tableAlias = alias;
        return this;
    }

    /** Add DISTINCT to the query. */
    public QueryBuilder distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * Set the logical operator used to combine top-level filters. Defaults to AND.
     * Call this before adding filters if you need OR at the root level.
     */
    public QueryBuilder filterOperation(final FilterArrayObject.FilterOperationType operation) {
        this.rootOperation = operation;
        return this;
    }

    // ─── SELECT ───────────────────────────────────────────────────────────

    /**
     * Add a field to the SELECT clause. Returns a {@link SelectStep} to optionally
     * configure alias, aggregation, join types, etc.
     *
     * @param fieldPath dot-separated field path (e.g.
     *                  {@code "vehicle.trim.model.brand.name"})
     * @param joinTypes optional join types for each path segment (e.g.
     *                  {@code "Inner","Inner",...})
     */
    public SelectStep select(final String fieldPath, final String... joinTypes) {
        return new SelectStep(this, fieldPath, joinTypes);
    }

    /**
     * Add any {@link ISelect} expression to the SELECT clause — use this for
     * {@link quaero.components.select.SelectFunctionObject functions},
     * {@link quaero.components.select.SelectArithmeticObject arithmetic},
     * {@link quaero.components.select.SelectConditionalObject CASE-WHEN},
     * {@link quaero.components.select.SelectConcatObject concat},
     * {@link quaero.components.select.SelectCoalesceObject coalesce},
     * {@link quaero.components.select.SelectSubstringObject substring},
     * {@link quaero.components.select.SelectTrimObject trim},
     * {@link quaero.components.select.SelectNumericOperationObject numeric ops},
     * {@link quaero.components.select.SelectInnerSubselectObject subselects}, or
     * {@link quaero.components.select.SelectValueObject literal values}.
     *
     * @param field any {@link ISelect} expression
     */
    public SelectStep select(final ISelect field) {
        return new SelectStep(this, field);
    }

    // ─── FILTERS ──────────────────────────────────────────────────────────

    /** Add a filter: {@code field = value} */
    public QueryBuilder filterEqual(final String field, final Object value) {
        return addSimpleFilter(field, FilterOperatorType.EQUAL, value);
    }

    /** Add a filter: {@code field <> value} */
    public QueryBuilder filterNotEqual(final String field, final Object value) {
        return addSimpleFilter(field, FilterOperatorType.NOT_EQUAL, value);
    }

    /** Add a filter: {@code UPPER(field) LIKE UPPER(value)} */
    public QueryBuilder filterLike(final String field, final String value) {
        return addSimpleFilter(field, FilterOperatorType.LIKE, value);
    }

    /** Add a filter: {@code field NOT LIKE value} */
    public QueryBuilder filterNotLike(final String field, final String value) {
        return addSimpleFilter(field, FilterOperatorType.NOT_LIKE, value);
    }

    /** Add a filter: {@code field > value} */
    public QueryBuilder filterGreaterThan(final String field, final Object value) {
        return addSimpleFilter(field, FilterOperatorType.GREATER_THAN, value);
    }

    /** Add a filter: {@code field >= value} */
    public QueryBuilder filterGreaterThanOrEqual(final String field, final Object value) {
        return addSimpleFilter(field, FilterOperatorType.GREATER_THAN_OR_EQUAL, value);
    }

    /** Add a filter: {@code field < value} */
    public QueryBuilder filterLessThan(final String field, final Object value) {
        return addSimpleFilter(field, FilterOperatorType.LESS_THAN, value);
    }

    /** Add a filter: {@code field <= value} */
    public QueryBuilder filterLessThanOrEqual(final String field, final Object value) {
        return addSimpleFilter(field, FilterOperatorType.LESS_THAN_OR_EQUAL, value);
    }

    /** Add a filter: {@code field IN (values)} */
    public QueryBuilder filterIn(final String field, final List<?> values) {
        return addSimpleFilter(field, FilterOperatorType.IN, values);
    }

    /** Add a filter: {@code field IN (values)} — varargs convenience */
    public QueryBuilder filterIn(final String field, final Object... values) {
        return addSimpleFilter(field, FilterOperatorType.IN, Arrays.asList(values));
    }

    /** Add a filter: {@code field NOT IN (values)} */
    public QueryBuilder filterNotIn(final String field, final List<?> values) {
        return addSimpleFilter(field, FilterOperatorType.NOT_IN, values);
    }

    /** Add a filter: {@code field IS NULL} */
    public QueryBuilder filterIsNull(final String field) {
        return addSimpleFilter(field, FilterOperatorType.IS_NULL, null);
    }

    /** Add a filter: {@code field IS NOT NULL} */
    public QueryBuilder filterIsNotNull(final String field) {
        return addSimpleFilter(field, FilterOperatorType.IS_NOT_NULL, null);
    }

    /**
     * Add a BETWEEN filter via two GTE + LTE conditions combined with AND.
     * Equivalent to: {@code field >= from AND field <= to}
     */
    public QueryBuilder filterBetween(final String field, final Object from, final Object to) {
        final IFilter gte = buildSimpleFilter(field, FilterOperatorType.GREATER_THAN_OR_EQUAL, from);
        final IFilter lte = buildSimpleFilter(field, FilterOperatorType.LESS_THAN_OR_EQUAL, to);
        return addFilter(FilterArrayObject.and(gte, lte));
    }

    /**
     * Add a pre-built {@link IFilter} directly — use this for complex nested
     * conditions or subquery filters that the shorthand methods do not cover.
     *
     * <pre>{@code
     * // Example: (brand = "Toyota" AND fuel = "Hybrid") OR price < 30000
     * IFilter complex = FilterArrayObject.or(
     * 		FilterArrayObject.and(FilterSimpleObject.equal("brand.name", "Toyota"),
     * 				FilterSimpleObject.equal("powertrain.fuelType", "Hybrid")),
     * 		FilterSimpleObject.lessThan("finalPrice", 30000));
     * builder.filter(complex);
     * }</pre>
     */
    public QueryBuilder filter(final IFilter filter) {
        return addFilter(filter);
    }

    // ─── COERCION ─────────────────────────────────────────────────────────
    public QueryBuilder coercionMode(CoercionMode mode) {
        this.coercionMode = mode;
        return this;
    }

    // ─── ORDER BY ─────────────────────────────────────────────────────────

    /** Add an ORDER BY field ASC. */
    public QueryBuilder orderAsc(final String field) {
        return addOrder(field, true, null);
    }

    /** Add an ORDER BY field DESC. */
    public QueryBuilder orderDesc(final String field) {
        return addOrder(field, false, null);
    }

    /** Add an ORDER BY aggregate ASC (e.g. COUNT, SUM). */
    public QueryBuilder orderAsc(final String field, final SelectOperatorType operator) {
        return addOrder(field, true, operator);
    }

    /** Add an ORDER BY aggregate DESC (e.g. COUNT, SUM). */
    public QueryBuilder orderDesc(final String field, final SelectOperatorType operator) {
        return addOrder(field, false, operator);
    }

    // ─── PAGINATION ───────────────────────────────────────────────────────

    /**
     * Set pagination.
     *
     * @param pageIndex zero-based page number
     * @param pageSize  number of results per page
     */
    public QueryBuilder page(final int pageIndex, final int pageSize) {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        return this;
    }

    // ─── BUILD ────────────────────────────────────────────────────────────

    /**
     * Build the final {@link Query} object ready to be passed to
     * {@code QueryExecutorService} or
     * {@code quaero.query.QueryBuilder#generateCustomQuery}.
     */
    public Query build() {
        final Query query = new Query();
        query.setTableName(tableName);
        query.setTableAlias(tableAlias != null ? tableAlias : tableName);
        query.setSelects(selects.isEmpty() ? null : selects);
        query.setOrders(orders.isEmpty() ? null : orders);
        query.setDistinctResults(distinct);
        query.setPageIndex(pageIndex);
        query.setPageSize(pageSize);
        query.setCoercionMode(coercionMode);

        if (!rootFilters.isEmpty()) {
            query.setFilter(rootFilters.size() == 1 ? rootFilters.get(0) : buildFilterArray(rootOperation, rootFilters));
        }

        return query;
    }

    // ─── Internal helpers ─────────────────────────────────────────────────

    private QueryBuilder addFilter(final IFilter filter) {
        this.rootFilters.add(filter);
        return this;
    }

    private QueryBuilder addSimpleFilter(final String field, final FilterOperatorType type, final Object value) {
        return addFilter(buildSimpleFilter(field, type, value));
    }

    private static IFilter buildSimpleFilter(final String field, final FilterOperatorType type, final Object value) {
        final FilterSimpleObject f = new FilterSimpleObject();
        f.setField(simpleSelect(field));
        f.setOperatorType(type);
        f.setValue(new SelectValueObject(value));
        return f;
    }

    private QueryBuilder addOrder(final String field, final boolean ascending, final SelectOperatorType operator) {
        final QueryOrderObject order = new QueryOrderObject();
        order.setField(simpleSelect(field));
        order.setAscending(ascending);
        order.setOperatorType(operator);
        orders.add(order);
        return this;
    }

    private static SelectSimpleObject simpleSelect(final String field, final String... joinTypes) {
        final SelectSimpleObject sel = new SelectSimpleObject();
        sel.setField(field);
        if (joinTypes != null && joinTypes.length > 0) {
            final List<QuaeroJoinType> types = new ArrayList<>();
            for (final String jt : joinTypes) {
                types.add(QuaeroJoinType.valueOf(jt));
            }
            sel.setJoinTypes(types);
        }
        return sel;
    }

    private static IFilter buildFilterArray(final FilterArrayObject.FilterOperationType operation, final List<IFilter> filters) {
        final FilterArrayObject array = new FilterArrayObject();
        array.setOperation(operation);
        array.setFilters(filters.toArray(new IFilter[0]));
        return array;
    }

    // ═════════════════════════════════════════════════════════════════════
    // SelectStep — fluent sub-builder returned by select()
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Intermediate step when configuring a SELECT field. Allows chaining alias,
     * aggregation and GROUP BY before returning control to the parent
     * {@link QueryBuilder}.
     */
    public static class SelectStep {

        private final QueryBuilder parent;
        private final QuerySelectObject selectObj;
        private final SelectSimpleObject simpleField;

        private SelectStep(final QueryBuilder parent, final String fieldPath, final String[] joinTypes) {
            this.parent = parent;
            this.simpleField = simpleSelect(fieldPath, joinTypes);

            this.selectObj = new QuerySelectObject();
            this.selectObj.setField(simpleField);
            parent.selects.add(this.selectObj);
        }

        private SelectStep(final QueryBuilder parent, final ISelect field) {
            this.parent = parent;
            this.simpleField = null;

            this.selectObj = new QuerySelectObject();
            this.selectObj.setField(field);
            parent.selects.add(this.selectObj);
        }

        /** Set the alias for this select field (e.g. the Map key in the result). */
        public SelectStep as(final String alias) {
            selectObj.setAlias(alias);
            return this;
        }

        /** Mark this field as part of the GROUP BY clause. */
        public SelectStep groupBy() {
            selectObj.setGroupBy(true);
            return this;
        }

        /** Mark/Or Not this field as part of the GROUP BY clause. */
        public SelectStep groupBy(final boolean gb) {
            selectObj.setGroupBy(gb);
            return this;
        }

        /** Apply COUNT aggregate to this field. */
        public SelectStep count() {
            selectObj.setOperatorType(SelectOperatorType.COUNT);
            return this;
        }

        /** Apply COUNT(DISTINCT) aggregate to this field. */
        public SelectStep countDistinct() {
            selectObj.setOperatorType(SelectOperatorType.COUNT_DISTINCT);
            return this;
        }

        /** Apply SUM aggregate to this field. */
        public SelectStep sum() {
            selectObj.setOperatorType(SelectOperatorType.SUMMATORY);
            return this;
        }

        /** Apply AVG aggregate to this field. */
        public SelectStep avg() {
            selectObj.setOperatorType(SelectOperatorType.AVERAGE);
            return this;
        }

        /** Apply MAX aggregate to this field. */
        public SelectStep max() {
            selectObj.setOperatorType(SelectOperatorType.MAX);
            return this;
        }

        /** Apply MIN aggregate to this field. */
        public SelectStep min() {
            selectObj.setOperatorType(SelectOperatorType.MIN);
            return this;
        }

        // Delegate back to parent for chaining
        public SelectStep select(final String fieldPath, final String... joinTypes) {
            return parent.select(fieldPath, joinTypes);
        }

        public SelectStep select(final ISelect s) {
            return parent.select(s);
        }

        public QueryBuilder alias(final String alias) {
            return parent.alias(alias);
        }

        public QueryBuilder filterEqual(final String f, Object v) {
            return parent.filterEqual(f, v);
        }

        public QueryBuilder filterLike(final String f, String v) {
            return parent.filterLike(f, v);
        }

        public QueryBuilder filterIn(final String f, Object... v) {
            return parent.filterIn(f, v);
        }

        public QueryBuilder filterIsNull(final String f) {
            return parent.filterIsNull(f);
        }

        public QueryBuilder filterIsNotNull(final String f) {
            return parent.filterIsNotNull(f);
        }

        public QueryBuilder filterBetween(String f, Object a, Object b) {
            return parent.filterBetween(f, a, b);
        }

        public QueryBuilder filterGreaterThan(String f, Object v) {
            return parent.filterGreaterThan(f, v);
        }

        public QueryBuilder filterLessThan(String f, Object v) {
            return parent.filterLessThan(f, v);
        }

        public QueryBuilder filter(final IFilter filter) {
            return parent.filter(filter);
        }

        public QueryBuilder orderAsc(final String f) {
            return parent.orderAsc(f);
        }

        public QueryBuilder orderDesc(final String f) {
            return parent.orderDesc(f);
        }

        public QueryBuilder page(int i, int s) {
            return parent.page(i, s);
        }

        public Query build() {
            return parent.build();
        }
    }
}
