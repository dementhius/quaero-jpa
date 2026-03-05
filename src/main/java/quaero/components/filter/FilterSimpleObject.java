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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;


import quaero.components.select.ISelect;
import quaero.components.select.SelectValueObject;
import quaero.components.select.Selects;
import quaero.components.values.FilterOperatorType;
import quaero.utils.CoercionMode;
import quaero.utils.ParameterTypeCoercer;

/**
 * A single WHERE condition: {@code field operatorType value}.
 *
 * <p>
 * Both {@code field} and {@code value} are {@link ISelect} — this means value
 * can be a literal ({@link SelectValueObject}), a field path, or any other
 * expression, enabling field-to-field comparisons.
 *
 * <p>
 * Type coercion from the raw {@code value} to the Java type expected by the JPA
 * field is applied here, controlled by the {@link CoercionMode} passed from the
 * root {@link quaero.query.Query}.
 */
public class FilterSimpleObject implements IFilter {

	private ISelect field;
	private FilterOperatorType operatorType;
	private ISelect value;

	/**
	 * JPA entity name of the root this filter applies to (e.g. {@code "Sale"}).
	 * Used by {@link quaero.query.QueryExecutor} to select the correct {@link Root}
	 * when the query has multiple roots (dynamic joins / Cartesian products). If
	 * null, the default root entity is used.
	 */
	private String entityName;

	/**
	 * Alias of the root entity this filter applies to (e.g. {@code "sale"}). Takes
	 * precedence over {@link #entityName} when both are set. If null, the default
	 * root entity is used.
	 */
	private String entityAlias;

	// ─────────────────────────────────────────────────────────────────────
	// Static factories — field as String (simple field path)
	// ─────────────────────────────────────────────────────────────────────

	public static FilterSimpleObject equal(final String field, final Object value) {
		return of(field, FilterOperatorType.EQUAL, value);
	}

	public static FilterSimpleObject notEqual(final String field, final Object value) {
		return of(field, FilterOperatorType.NOT_EQUAL, value);
	}

	public static FilterSimpleObject like(final String field, final String value) {
		return of(field, FilterOperatorType.LIKE, value);
	}

	public static FilterSimpleObject notLike(final String field, final String value) {
		return of(field, FilterOperatorType.NOT_LIKE, value);
	}

	public static FilterSimpleObject greaterThan(final String field, final Object value) {
		return of(field, FilterOperatorType.GREATER_THAN, value);
	}

	public static FilterSimpleObject greaterThanOrEqual(final String field, final Object value) {
		return of(field, FilterOperatorType.GREATER_THAN_OR_EQUAL, value);
	}

	public static FilterSimpleObject lessThan(final String field, final Object value) {
		return of(field, FilterOperatorType.LESS_THAN, value);
	}

	public static FilterSimpleObject lessThanOrEqual(final String field, final Object value) {
		return of(field, FilterOperatorType.LESS_THAN_OR_EQUAL, value);
	}

	public static FilterSimpleObject in(final String field, final List<?> values) {
		return of(field, FilterOperatorType.IN, values);
	}

	public static FilterSimpleObject notIn(final String field, final List<?> values) {
		return of(field, FilterOperatorType.NOT_IN, values);
	}

	public static FilterSimpleObject isNull(final String field) {
		return of(field, FilterOperatorType.IS_NULL, null);
	}

	public static FilterSimpleObject isNotNull(final String field) {
		return of(field, FilterOperatorType.IS_NOT_NULL, null);
	}

	public static FilterSimpleObject isEmpty(final String field) {
		return of(field, FilterOperatorType.IS_EMPTY, null);
	}

	public static FilterSimpleObject isNotEmpty(final String field) {
		return of(field, FilterOperatorType.IS_NOT_EMPTY, null);
	}

	public static FilterSimpleObject equalTrim(final String field, final Object value) {
		return of(field, FilterOperatorType.EQUAL_TRIM, value);
	}

	public static FilterSimpleObject notEqualTrim(final String field, final Object value) {
		return of(field, FilterOperatorType.NOT_EQUAL_TRIM, value);
	}

	public static FilterSimpleObject likeTrim(final String field, final String value) {
		return of(field, FilterOperatorType.LIKE_TRIM, value);
	}

	public static FilterSimpleObject notLikeTrim(final String field, final String value) {
		return of(field, FilterOperatorType.NOT_LIKE_TRIM, value);
	}

	public static FilterSimpleObject inTrim(final String field, final List<?> values) {
		return of(field, FilterOperatorType.IN_TRIM, values);
	}

	public static FilterSimpleObject notInTrim(final String field, final List<?> values) {
		return of(field, FilterOperatorType.NOT_IN_TRIM, values);
	}

	// ─────────────────────────────────────────────────────────────────────
	// Static factories — field as ISelect expression (e.g. Selects.trim(...))
	// ─────────────────────────────────────────────────────────────────────

	public static FilterSimpleObject equal(final ISelect field, final Object value) {
		return of(field, FilterOperatorType.EQUAL, value);
	}

	/** Field-to-field equality: {@code expr1 = expr2} */
	public static FilterSimpleObject equal(final ISelect field, final ISelect value) {
		return of(field, FilterOperatorType.EQUAL, value);
	}

	public static FilterSimpleObject notEqual(final ISelect field, final Object value) {
		return of(field, FilterOperatorType.NOT_EQUAL, value);
	}

	public static FilterSimpleObject notEqual(final ISelect field, final ISelect value) {
		return of(field, FilterOperatorType.NOT_EQUAL, value);
	}

	public static FilterSimpleObject like(final ISelect field, final String value) {
		return of(field, FilterOperatorType.LIKE, value);
	}

	public static FilterSimpleObject notLike(final ISelect field, final String value) {
		return of(field, FilterOperatorType.NOT_LIKE, value);
	}

	public static FilterSimpleObject greaterThan(final ISelect field, final Object value) {
		return of(field, FilterOperatorType.GREATER_THAN, value);
	}

	public static FilterSimpleObject greaterThan(final ISelect field, final ISelect value) {
		return of(field, FilterOperatorType.GREATER_THAN, value);
	}

	public static FilterSimpleObject greaterThanOrEqual(final ISelect field, final Object value) {
		return of(field, FilterOperatorType.GREATER_THAN_OR_EQUAL, value);
	}

	public static FilterSimpleObject greaterThanOrEqual(final ISelect field, final ISelect value) {
		return of(field, FilterOperatorType.GREATER_THAN_OR_EQUAL, value);
	}

	public static FilterSimpleObject lessThan(final ISelect field, final Object value) {
		return of(field, FilterOperatorType.LESS_THAN, value);
	}

	public static FilterSimpleObject lessThan(final ISelect field, final ISelect value) {
		return of(field, FilterOperatorType.LESS_THAN, value);
	}

	public static FilterSimpleObject lessThanOrEqual(final ISelect field, final Object value) {
		return of(field, FilterOperatorType.LESS_THAN_OR_EQUAL, value);
	}

	public static FilterSimpleObject lessThanOrEqual(final ISelect field, final ISelect value) {
		return of(field, FilterOperatorType.LESS_THAN_OR_EQUAL, value);
	}

	public static FilterSimpleObject in(final ISelect field, final List<?> values) {
		return of(field, FilterOperatorType.IN, values);
	}

	public static FilterSimpleObject notIn(final ISelect field, final List<?> values) {
		return of(field, FilterOperatorType.NOT_IN, values);
	}

	public static FilterSimpleObject isNull(final ISelect field) {
		return of(field, FilterOperatorType.IS_NULL, (ISelect) null);
	}

	public static FilterSimpleObject isNotNull(final ISelect field) {
		return of(field, FilterOperatorType.IS_NOT_NULL, (ISelect) null);
	}

	public static FilterSimpleObject isEmpty(final ISelect field) {
		return of(field, FilterOperatorType.IS_EMPTY, (ISelect) null);
	}

	public static FilterSimpleObject isNotEmpty(final ISelect field) {
		return of(field, FilterOperatorType.IS_NOT_EMPTY, (ISelect) null);
	}

	public static FilterSimpleObject equalTrim(final ISelect field, final Object value) {
		return of(field, FilterOperatorType.EQUAL_TRIM, value);
	}

	public static FilterSimpleObject equalTrim(final ISelect field, final ISelect value) {
		return of(field, FilterOperatorType.EQUAL_TRIM, value);
	}

	public static FilterSimpleObject notEqualTrim(final ISelect field, final Object value) {
		return of(field, FilterOperatorType.NOT_EQUAL_TRIM, value);
	}

	public static FilterSimpleObject notEqualTrim(final ISelect field, final ISelect value) {
		return of(field, FilterOperatorType.NOT_EQUAL_TRIM, value);
	}

	public static FilterSimpleObject likeTrim(final ISelect field, final String value) {
		return of(field, FilterOperatorType.LIKE_TRIM, value);
	}

	public static FilterSimpleObject notLikeTrim(final ISelect field, final String value) {
		return of(field, FilterOperatorType.NOT_LIKE_TRIM, value);
	}

	public static FilterSimpleObject inTrim(final ISelect field, final List<?> values) {
		return of(field, FilterOperatorType.IN_TRIM, values);
	}

	public static FilterSimpleObject notInTrim(final ISelect field, final List<?> values) {
		return of(field, FilterOperatorType.NOT_IN_TRIM, values);
	}

	// ─────────────────────────────────────────────────────────────────────
	// Private helpers
	// ─────────────────────────────────────────────────────────────────────

	private static FilterSimpleObject of(final String fieldPath, final FilterOperatorType type, final Object value) {
		return of(Selects.field(fieldPath), type, value);
	}

	private static FilterSimpleObject of(final ISelect field, final FilterOperatorType type, final Object value) {
		final FilterSimpleObject f = new FilterSimpleObject();
		f.field = field;
		f.operatorType = type;
		f.value = new SelectValueObject(value);
		return f;
	}

	private static FilterSimpleObject of(final ISelect field, final FilterOperatorType type, final ISelect value) {
		final FilterSimpleObject f = new FilterSimpleObject();
		f.field = field;
		f.operatorType = type;
		f.value = value;
		return f;
	}

	// ─────────────────────────────────────────────────────────────────────
	// IFilter — resolve
	// ─────────────────────────────────────────────────────────────────────

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Predicate resolve(final CoercionMode mode, final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {

		// ── Resolve the field expression ──────────────────────────────────
		final Expression fieldExpr = field.resolve(mode, entity, query, cb, entities, managedTypes);

		// ── NULL / EMPTY checks — no value needed ─────────────────────────
		if (operatorType == FilterOperatorType.IS_NULL)
			return cb.isNull(fieldExpr);
		if (operatorType == FilterOperatorType.IS_NOT_NULL)
			return cb.isNotNull(fieldExpr);
		if (operatorType == FilterOperatorType.IS_EMPTY)
			return cb.isEmpty(fieldExpr);
		if (operatorType == FilterOperatorType.IS_NOT_EMPTY)
			return cb.isNotEmpty(fieldExpr);

		// ── Resolve the raw value ─────────────────────────────────────────
		final Object rawValue = extractRawValue();
		final Expression valueExpr;

		if (rawValue != null || isLiteralValue()) {
			// Determine target type from the field expression's Java type
			final Class<?> targetType = fieldExpr.getJavaType();
			final String fieldName = extractFieldName();
			valueExpr = buildLiteralExpression(rawValue, targetType, fieldName, mode, cb);
		} else {
			// Field-to-field comparison — resolve the value ISelect directly
			valueExpr = value.resolve(mode, entity, query, cb, entities, managedTypes);
		}

		// ── Build predicate ───────────────────────────────────────────────
		return buildPredicate(operatorType, fieldExpr, valueExpr, rawValue, mode, extractFieldName(),
				fieldExpr.getJavaType(), cb);
	}

	// ─────────────────────────────────────────────────────────────────────
	// Predicate construction
	// ─────────────────────────────────────────────────────────────────────

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Predicate buildPredicate(final FilterOperatorType type, final Expression fieldExpr,
			final Expression valueExpr, final Object rawValue, final CoercionMode mode, final String fieldName,
			final Class<?> targetType, final CriteriaBuilder cb) {
		switch (type) {

		case EQUAL:
			return cb.equal(fieldExpr, valueExpr);

		case NOT_EQUAL:
			return cb.notEqual(fieldExpr, valueExpr);

		case GREATER_THAN:
			return cb.greaterThan((Expression<Comparable>) fieldExpr, (Expression<Comparable>) valueExpr);

		case GREATER_THAN_OR_EQUAL:
			return cb.greaterThanOrEqualTo((Expression<Comparable>) fieldExpr, (Expression<Comparable>) valueExpr);

		case LESS_THAN:
			return cb.lessThan((Expression<Comparable>) fieldExpr, (Expression<Comparable>) valueExpr);

		case LESS_THAN_OR_EQUAL:
			return cb.lessThanOrEqualTo((Expression<Comparable>) fieldExpr, (Expression<Comparable>) valueExpr);

		case LIKE:
			return cb.like(cb.upper((Expression<String>) fieldExpr), cb.upper((Expression<String>) valueExpr));

		case NOT_LIKE:
			return cb.notLike(cb.upper((Expression<String>) fieldExpr), cb.upper((Expression<String>) valueExpr));

		case LIKE_TRIM:
			return cb.like(cb.upper(cb.trim((Expression<String>) fieldExpr)),
					cb.upper(cb.trim((Expression<String>) valueExpr)));

		case NOT_LIKE_TRIM:
			return cb.notLike(cb.upper(cb.trim((Expression<String>) fieldExpr)),
					cb.upper(cb.trim((Expression<String>) valueExpr)));

		case EQUAL_TRIM:
			return cb.equal(cb.trim((Expression<String>) fieldExpr), cb.trim((Expression<String>) valueExpr));

		case NOT_EQUAL_TRIM:
			return cb.notEqual(cb.trim((Expression<String>) fieldExpr), cb.trim((Expression<String>) valueExpr));

		case IN:
			return buildInPredicate(fieldExpr, rawValue, mode, fieldName, targetType, cb, false);

		case NOT_IN:
			return buildInPredicate(fieldExpr, rawValue, mode, fieldName, targetType, cb, true).not();

		case IN_TRIM:
			return buildInPredicate(cb.trim((Expression<String>) fieldExpr), rawValue, mode, fieldName, targetType, cb,
					false);

		case NOT_IN_TRIM:
			return buildInPredicate(cb.trim((Expression<String>) fieldExpr), rawValue, mode, fieldName, targetType, cb,
					true).not();

		default:
			throw new UnsupportedOperationException("Unsupported filter operator: " + type);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Predicate buildInPredicate(final Expression fieldExpr, final Object rawValue, final CoercionMode mode,
			final String fieldName, final Class<?> targetType, final CriteriaBuilder cb, final boolean negated) {

		final CriteriaBuilder.In inClause = cb.in(fieldExpr);

		if (rawValue instanceof Collection) {
			for (final Object item : (Collection<?>) rawValue) {
				final Object coerced = ParameterTypeCoercer.coerce(item, targetType, fieldName, mode);
				inClause.value(coerced);
			}
		} else if (rawValue != null) {
			// Single value wrapped in IN — treat as EQUAL
			inClause.value(ParameterTypeCoercer.coerce(rawValue, targetType, fieldName, mode));
		}

		return negated ? inClause.not() : inClause;
	}

	// ─────────────────────────────────────────────────────────────────────
	// Coercion helpers
	// ─────────────────────────────────────────────────────────────────────

	/**
	 * Coerce the raw value and wrap it in a {@link CriteriaBuilder#literal}. For IN
	 * operators, returns the first coerced element as a probe (the actual IN
	 * construction uses the collection directly in buildInPredicate).
	 */
	private Expression<?> buildLiteralExpression(final Object rawValue, final Class<?> targetType,
			final String fieldName, final CoercionMode mode, final CriteriaBuilder cb) {
		if (rawValue instanceof Collection) {
			return cb.literal(rawValue);
		}
		final Object coerced = ParameterTypeCoercer.coerce(rawValue, targetType, fieldName, mode);
		return cb.literal(coerced);
	}

	// ─────────────────────────────────────────────────────────────────────
	// Introspection helpers
	// ─────────────────────────────────────────────────────────────────────

	/**
	 * True if the value side is a SelectValueObject (literal), not a field path.
	 */
	private boolean isLiteralValue() {
		return value instanceof SelectValueObject;
	}

	/** Extract the raw Java object from a SelectValueObject, or null otherwise. */
	private Object extractRawValue() {
		if (value instanceof SelectValueObject) {
			return ((SelectValueObject) value).getValue();
		}
		return null;
	}

	private String extractFieldName() {
		final String fieldPart;
		if (field instanceof quaero.components.select.SelectSimpleObject) {
			fieldPart = ((quaero.components.select.SelectSimpleObject) field).getField();
		} else {
			fieldPart = field.getClass().getSimpleName();
		}

		if (entityAlias != null)
			return entityAlias + "." + fieldPart;
		if (entityName != null)
			return entityName + "." + fieldPart;
		return fieldPart;
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

	public ISelect getValue() {
		return value;
	}

	public void setValue(final ISelect value) {
		this.value = value;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(final String entityName) {
		this.entityName = entityName;
	}

	public String getEntityAlias() {
		return entityAlias;
	}

	public void setEntityAlias(final String entityAlias) {
		this.entityAlias = entityAlias;
	}
}