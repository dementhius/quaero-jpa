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
package quaero.components.select;

import quaero.components.filter.IFilter;
import quaero.components.values.ArithmeticOperationType;
import quaero.components.values.NumericOperationType;
import quaero.query.QueryJoinTypesObject.QuaeroJoinType;

import javax.persistence.criteria.CriteriaBuilder.Trimspec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Static factories for all {@link ISelect} variants — the counterpart of
 * {@link quaero.components.filter.FilterSimpleObject}'s static helpers.
 *
 * <p>Use these to compose nested expressions without leaving the fluent style:
 *
 * <pre>{@code
 * Query.builder("Sale")
 *     .select(Selects.add(Selects.field("price"), Selects.field("tax"))).as("total")
 *     .select(Selects.coalesce(Selects.field("nickname"), Selects.field("name"))).as("displayName")
 *     .select(Selects.caseWhen(Selects.value("N/A"),
 *         Selects.when(FilterSimpleObject.equal("status", "ACTIVE"), Selects.value("Active")),
 *         Selects.when(FilterSimpleObject.equal("status", "CLOSED"), Selects.value("Closed"))))
 *     .as("statusLabel")
 *     .build();
 * }</pre>
 */
public final class Selects {

    private Selects() {}

    // ─── Simple field ──────────────────────────────────────────────────────

    /** Reference a plain entity field. */
    public static SelectSimpleObject field(final String fieldPath) {
        final SelectSimpleObject s = new SelectSimpleObject();
        s.setField(fieldPath);
        return s;
    }

    /** Reference a field with explicit join types for each path segment. */
    public static SelectSimpleObject field(final String fieldPath, final String... joinTypes) {
        final SelectSimpleObject s = new SelectSimpleObject();
        s.setField(fieldPath);
        if (joinTypes != null && joinTypes.length > 0) {
            final List<QuaeroJoinType> types = new ArrayList<>();
            for (final String jt : joinTypes) {
                types.add(QuaeroJoinType.valueOf(jt));
            }
            s.setJoinTypes(types);
        }
        return s;
    }

    // ─── Literal value ────────────────────────────────────────────────────

    /** Wrap a Java literal (String, Number, etc.) as a SELECT expression. */
    public static SelectValueObject value(final Object value) {
        return new SelectValueObject(value);
    }

    // ─── Function ─────────────────────────────────────────────────────────

    /** Call a registered SQL function with the given parameters. */
    public static SelectFunctionObject function(final String function, final String returnType, final ISelect... params) {
        final SelectFunctionObject s = new SelectFunctionObject();
        s.setFunction(function);
        s.setReturnType(returnType);
        s.setParams(Arrays.asList(params));
        return s;
    }

    // ─── Arithmetic ───────────────────────────────────────────────────────

    /** Generic arithmetic operation across a list of operands. */
    public static SelectArithmeticObject arithmetic(final ArithmeticOperationType operation, final ISelect... fields) {
        final SelectArithmeticObject s = new SelectArithmeticObject();
        s.setOperation(operation);
        s.setFields(Arrays.asList(fields));
        return s;
    }

    /** {@code a + b + ...} */
    public static SelectArithmeticObject add(final ISelect... fields) {
        return arithmetic(ArithmeticOperationType.SUMMATION, fields);
    }

    /** {@code a - b - ...} */
    public static SelectArithmeticObject subtract(final ISelect... fields) {
        return arithmetic(ArithmeticOperationType.DIFFERENCE, fields);
    }

    /** {@code a * b * ...} */
    public static SelectArithmeticObject multiply(final ISelect... fields) {
        return arithmetic(ArithmeticOperationType.MULTIPLY, fields);
    }

    /** {@code a / b / ...} */
    public static SelectArithmeticObject divide(final ISelect... fields) {
        return arithmetic(ArithmeticOperationType.DIVISION, fields);
    }

    /** {@code a % b} */
    public static SelectArithmeticObject mod(final ISelect a, final ISelect b) {
        return arithmetic(ArithmeticOperationType.MOD, a, b);
    }

    // ─── Numeric operations ───────────────────────────────────────────────

    /** Generic single-operand numeric operation. */
    public static SelectNumericOperationObject numericOp(final NumericOperationType operation, final ISelect field) {
        final SelectNumericOperationObject s = new SelectNumericOperationObject();
        s.setOperation(operation);
        s.setField(field);
        return s;
    }

    /** {@code ABS(field)} */
    public static SelectNumericOperationObject abs(final ISelect field) {
        return numericOp(NumericOperationType.ABSOLUTE, field);
    }

    /** {@code SQRT(field)} */
    public static SelectNumericOperationObject sqrt(final ISelect field) {
        return numericOp(NumericOperationType.SQUARE_ROOT, field);
    }

    /** {@code -field} */
    public static SelectNumericOperationObject negate(final ISelect field) {
        return numericOp(NumericOperationType.NEGATION, field);
    }

    /** {@code AVG(field)} */
    public static SelectNumericOperationObject avg(final ISelect field) {
        return numericOp(NumericOperationType.AVERAGE, field);
    }

    /** {@code SUM(field)} */
    public static SelectNumericOperationObject sum(final ISelect field) {
        return numericOp(NumericOperationType.SUM, field);
    }

    /** {@code MAX(field)} */
    public static SelectNumericOperationObject max(final ISelect field) {
        return numericOp(NumericOperationType.MAX, field);
    }

    /** {@code MIN(field)} */
    public static SelectNumericOperationObject min(final ISelect field) {
        return numericOp(NumericOperationType.MIN, field);
    }

    // ─── Concat ───────────────────────────────────────────────────────────

    /** Concatenate the given expressions as strings. */
    public static SelectConcatObject concat(final ISelect... values) {
        final SelectConcatObject s = new SelectConcatObject();
        s.setValues(Arrays.asList(values));
        return s;
    }

    // ─── Coalesce ─────────────────────────────────────────────────────────

    /** Return the first non-null value among the given expressions. */
    public static SelectCoalesceObject coalesce(final ISelect... values) {
        final SelectCoalesceObject s = new SelectCoalesceObject();
        s.setValues(Arrays.asList(values));
        return s;
    }

    // ─── Substring ────────────────────────────────────────────────────────

    /** {@code SUBSTRING(value, position)} */
    public static SelectSubstringObject substring(final ISelect value, final int position) {
        final SelectSubstringObject s = new SelectSubstringObject();
        s.setValue(value);
        s.setPosition(position);
        return s;
    }

    /** {@code SUBSTRING(value, position, length)} */
    public static SelectSubstringObject substring(final ISelect value, final int position, final int length) {
        final SelectSubstringObject s = new SelectSubstringObject();
        s.setValue(value);
        s.setPosition(position);
        s.setLength(length);
        return s;
    }

    // ─── Trim ─────────────────────────────────────────────────────────────

    /** {@code TRIM(value)} */
    public static SelectTrimObject trim(final ISelect value) {
        final SelectTrimObject s = new SelectTrimObject();
        s.setField(value);
        return s;
    }

    /** {@code TRIM(spec FROM value)} — e.g. {@code LEADING}, {@code TRAILING}, {@code BOTH}. */
    public static SelectTrimObject trim(final ISelect value, final Trimspec spec) {
        final SelectTrimObject s = new SelectTrimObject();
        s.setField(value);
        s.setSpec(spec);
        return s;
    }

    /** {@code TRIM(spec character FROM value)} */
    public static SelectTrimObject trim(final ISelect value, final Trimspec spec, final char character) {
        final SelectTrimObject s = new SelectTrimObject();
        s.setField(value);
        s.setSpec(spec);
        s.setCharacter(character);
        return s;
    }

    // ─── Conditional (CASE-WHEN) ──────────────────────────────────────────

    /**
     * Build a single WHEN branch: {@code WHEN condition THEN value}.
     * Pass the results to {@link #caseWhen(ISelect, SelectConditionValueObject...)}.
     */
    public static SelectConditionValueObject when(final IFilter condition, final ISelect value) {
        final SelectConditionValueObject c = new SelectConditionValueObject();
        c.setCondition(condition);
        c.setValue(value);
        return c;
    }

    /**
     * Build a CASE-WHEN-ELSE expression.
     *
     * <pre>{@code
     * Selects.caseWhen(
     *     Selects.value("N/A"),
     *     Selects.when(FilterSimpleObject.equal("status", "ACTIVE"), Selects.value("Active")),
     *     Selects.when(FilterSimpleObject.equal("status", "CLOSED"), Selects.value("Closed")))
     * }</pre>
     *
     * @param otherwise the ELSE value (use {@link #value(Object)} for a literal)
     * @param conditions one or more WHEN branches built with {@link #when(IFilter, ISelect)}
     */
    public static SelectConditionalObject caseWhen(final ISelect otherwise, final SelectConditionValueObject... conditions) {
        final SelectConditionalObject s = new SelectConditionalObject();
        s.setConditions(Arrays.asList(conditions));
        s.setOtherwise(otherwise);
        return s;
    }

    // ─── Subselect ────────────────────────────────────────────────────────

    /**
     * Build a correlated subquery: {@code (SELECT select FROM tableName WHERE filter)}.
     *
     * @param tableName JPA entity name of the subquery root
     * @param select    what to select in the subquery
     * @param filter    optional WHERE clause ({@code null} = no filter)
     */
    public static SelectInnerSubselectObject subselect(final String tableName, final ISelect select, final IFilter filter) {
        final SelectInnerSubselectObject s = new SelectInnerSubselectObject();
        s.setTableName(tableName);
        s.setSelect(select);
        s.setFilter(filter);
        return s;
    }
}
