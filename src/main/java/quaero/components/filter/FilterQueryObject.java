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
package quaero.components.filter;

import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import quaero.components.select.ISelect;
import quaero.components.select.Selects;
import quaero.components.values.FilterOperatorType;
import quaero.utils.CoercionMode;

public class FilterQueryObject implements IFilter {

    private ISelect field;
    private FilterOperatorType operatorType;

    private ISelect queryField;
    private String queryEntity;
    private IFilter queryFilter;

    // ─────────────────────────────────────────────────────────────────────
    // Static factories
    // ─────────────────────────────────────────────────────────────────────

    public static FilterQueryObject queryEqual(final String field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(Selects.field(field), FilterOperatorType.EQUAL, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryEqual(final ISelect field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(field, FilterOperatorType.EQUAL, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryNotEqual(final String field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(Selects.field(field), FilterOperatorType.NOT_EQUAL, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryNotEqual(final ISelect field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(field, FilterOperatorType.NOT_EQUAL, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryIn(final String field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(Selects.field(field), FilterOperatorType.IN, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryIn(final ISelect field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(field, FilterOperatorType.IN, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryNotIn(final String field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(Selects.field(field), FilterOperatorType.NOT_IN, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryNotIn(final ISelect field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(field, FilterOperatorType.NOT_IN, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryGreaterThan(final String field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(Selects.field(field), FilterOperatorType.GREATER_THAN, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryGreaterThan(final ISelect field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(field, FilterOperatorType.GREATER_THAN, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryGreaterThanOrEqual(final String field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(Selects.field(field), FilterOperatorType.GREATER_THAN_OR_EQUAL, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryGreaterThanOrEqual(final ISelect field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(field, FilterOperatorType.GREATER_THAN_OR_EQUAL, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryLessThan(final String field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(Selects.field(field), FilterOperatorType.LESS_THAN, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryLessThan(final ISelect field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(field, FilterOperatorType.LESS_THAN, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryLessThanOrEqual(final String field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(Selects.field(field), FilterOperatorType.LESS_THAN_OR_EQUAL, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryLessThanOrEqual(final ISelect field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(field, FilterOperatorType.LESS_THAN_OR_EQUAL, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryLike(final String field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(Selects.field(field), FilterOperatorType.LIKE, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryLike(final ISelect field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(field, FilterOperatorType.LIKE, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryNotLike(final String field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(Selects.field(field), FilterOperatorType.NOT_LIKE, entity, queryField, queryFilter);
    }

    public static FilterQueryObject queryNotLike(final ISelect field, final String entity, final ISelect queryField, final IFilter queryFilter) {
        return of(field, FilterOperatorType.NOT_LIKE, entity, queryField, queryFilter);
    }

    private static FilterQueryObject of(final ISelect field, final FilterOperatorType op, final String entity, final ISelect queryField, final IFilter queryFilter) {
        final FilterQueryObject f = new FilterQueryObject();
        f.field = field;
        f.operatorType = op;
        f.queryEntity = entity;
        f.queryField = queryField;
        f.queryFilter = queryFilter;
        return f;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Getters and setters
    // ─────────────────────────────────────────────────────────────────────

    public ISelect getField() {
        return field;
    }

    public void setField(final ISelect field) {
        this.field = field;
    }

    public FilterOperatorType getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(final FilterOperatorType operatorType) {
        this.operatorType = operatorType;
    }

    public ISelect getQueryField() {
        return queryField;
    }

    public void setQueryField(final ISelect queryField) {
        this.queryField = queryField;
    }

    public String getQueryEntity() {
        return queryEntity;
    }

    public void setQueryEntity(final String queryEntity) {
        this.queryEntity = queryEntity;
    }

    public IFilter getQueryFilter() {
        return queryFilter;
    }

    public void setQueryFilter(final IFilter queryFilter) {
        this.queryFilter = queryFilter;
    }

    private Expression generateQuery(final CoercionMode mode,final CriteriaQuery<?> mainQuery, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        final EntityType<?> tableEntity = entities.get(queryEntity);
        final Subquery subQuery = mainQuery.subquery(Object.class);
        final Root<?> sqEntity = subQuery.from(tableEntity);

        final Expression<?> queryParams = queryField.resolve(mode, sqEntity, mainQuery, cb, entities, managedTypes);

        if (queryFilter != null) {
            final Predicate where = queryFilter.resolve(mode, sqEntity, mainQuery, cb, entities, managedTypes);
            return subQuery.select(queryParams).where(where);
        }
        return subQuery.select(queryParams);
    }

    @Override
    public Predicate resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        final Expression fieldParams = field.resolve(mode, entity, query, cb, entities, managedTypes);
        final Expression subQueryExpr = generateQuery(mode, query, cb, entities, managedTypes);

        switch (operatorType) {
            case EQUAL:
                return cb.equal(fieldParams, subQueryExpr);
            case NOT_EQUAL:
                return cb.notEqual(fieldParams, subQueryExpr);
            case LESS_THAN:
                return cb.lessThan(fieldParams, subQueryExpr);
            case LESS_THAN_OR_EQUAL:
                return cb.lessThanOrEqualTo(fieldParams, subQueryExpr);
            case GREATER_THAN:
                return cb.greaterThan(fieldParams, subQueryExpr);
            case GREATER_THAN_OR_EQUAL:
                return cb.greaterThanOrEqualTo(fieldParams, subQueryExpr);
            case LIKE:
                return cb.like(cb.upper(fieldParams), cb.upper(subQueryExpr));
            case NOT_LIKE:
                return cb.notLike(cb.upper(fieldParams), cb.upper(subQueryExpr));
            case IS_EMPTY:
                return cb.isEmpty(fieldParams);
            case IS_NOT_EMPTY:
                return cb.isNotEmpty(fieldParams);
            case IS_NULL:
                return cb.isNull(fieldParams);
            case IS_NOT_NULL:
                return cb.isNotNull(fieldParams);
            case IN:
                return fieldParams.in(subQueryExpr);
            case NOT_IN:
                return fieldParams.in(subQueryExpr).not();
            case IN_TRIM:
                return cb.trim(fieldParams).in(subQueryExpr);
            case NOT_IN_TRIM:
                return cb.trim(fieldParams).in(subQueryExpr).not();
            case EQUAL_TRIM:
                return cb.equal(cb.trim(fieldParams), cb.trim(subQueryExpr));
            case NOT_EQUAL_TRIM:
                return cb.notEqual(cb.trim(fieldParams), cb.trim(subQueryExpr));
            case LIKE_TRIM:
                return cb.like(cb.trim(cb.upper(fieldParams)), cb.trim(cb.upper(subQueryExpr)));
            case NOT_LIKE_TRIM:
                return cb.notLike(cb.trim(cb.upper(fieldParams)), cb.trim(cb.upper(subQueryExpr)));
            default:
                break;
        }
        return null;
    }
}
