/*
 * Copyright 2026 Ddementhius
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

import java.util.List;

import quaero.components.filter.IFilter;
import quaero.utils.CoercionMode;

/**
 * Root DTO for a dynamic query.
 *
 * Deserialised from JSON on the frontend side, or constructed programmatically
 * via {@link QueryBuilder}.
 */
public class Query {

	/** JPA entity simple name (e.g. {@code "Sale"}). */
	private String tableName;
	/** Optional alias for the root entity in generated expressions. */
	private String tableAlias;

	/**
	 * Controls how type mismatches between filter values and JPA field types are
	 * handled. Defaults to {@link CoercionMode#STRICT}.
	 *
	 * <ul>
	 * <li>{@link CoercionMode#STRICT} — value must already match the field type;
	 * any mismatch throws
	 * {@link quaero.exception.IncorrectParameterTypeException}.</li>
	 * <li>{@link CoercionMode#LENIENT} — best-effort conversion is attempted
	 * (String→date, String→number, numeric widening, etc.) before failing.</li>
	 * </ul>
	 */
	private CoercionMode coercionMode = CoercionMode.STRICT;
	/** Fields to include in SELECT. If null or empty, all fields are selected. */

	private List<QuerySelectObject> selects;
	/**
	 * WHERE clause. Can be a single
	 * {@link quaero.components.filter.FilterSimpleObject}, a nested
	 * {@link quaero.components.filter.FilterArrayObject} (AND/OR), or a
	 * {@link quaero.components.filter.FilterQueryObject} (subquery).
	 */
	private IFilter filter;

	/** ORDER BY clauses, applied in list order. */
	private List<QueryOrderObject> orders;

	/** Zero-based page number. Used together with {@link #pageSize}. */
	private Integer pageIndex;
	/** Number of results per page. */
	private Integer pageSize;

	/**
	 * Explicit cartesian joins where the main table is fixed. Each join specifies
	 * mainTableName + joinTableName + ON conditions.
	 */
	private List<QueryJoinObject> dynamicJoins;
	/**
	 * Explicit cartesian joins where each ON-condition parameter declares its own
	 * main table (multi-root joins).
	 */
	private List<QueryMultiJoinObject> dynamicJoinsMultiple;
	/**
	 * Override the join type (INNER / LEFT / RIGHT) for specific field paths.
	 * Useful when the default join inferred from the path is not desired.
	 */
	private List<QueryJoinTypesObject> paramJoinTypes;

	/** Add DISTINCT to the query. Defaults to false. */
	private Boolean distinctResults = false;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(final String tableName) {
		this.tableName = tableName;
	}

	public String getTableAlias() {
		return tableAlias;
	}

	public void setTableAlias(final String tableAlias) {
		this.tableAlias = tableAlias;
	}

	/**
	 * Returns the coercion mode for this request. Defaults to
	 * {@link CoercionMode#STRICT}.
	 */
	public CoercionMode getCoercionMode() {
		return coercionMode != null ? coercionMode : CoercionMode.STRICT;
	}

	public void setCoercionMode(final CoercionMode coercionMode) {
		this.coercionMode = coercionMode;
	}

	public List<QuerySelectObject> getSelects() {
		return selects;
	}

	public void setSelects(List<QuerySelectObject> s) {
		this.selects = s;
	}

	public IFilter getFilter() {
		return filter;
	}

	public void setFilter(final IFilter filter) {
		this.filter = filter;
	}

	public List<QueryOrderObject> getOrders() {
		return orders;
	}

	public void setOrders(List<QueryOrderObject> o) {
		this.orders = o;
	}

	public Integer getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(final Integer pageIndex) {
		this.pageIndex = pageIndex;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(final Integer pageSize) {
		this.pageSize = pageSize;
	}

	public List<QueryJoinObject> getDynamicJoins() {
		return dynamicJoins;
	}

	public void setDynamicJoins(List<QueryJoinObject> j) {
		this.dynamicJoins = j;
	}

	public List<QueryMultiJoinObject> getDynamicJoinsMultiple() {
		return dynamicJoinsMultiple;
	}

	public void setDynamicJoinsMultiple(List<QueryMultiJoinObject> j) {
		this.dynamicJoinsMultiple = j;
	}

	public List<QueryJoinTypesObject> getParamJoinTypes() {
		return paramJoinTypes;
	}

	public void setParamJoinTypes(List<QueryJoinTypesObject> p) {
		this.paramJoinTypes = p;
	}

	public Boolean getDistinctResults() {
		return distinctResults;
	}

	public void setDistinctResults(final Boolean distinctResults) {
		this.distinctResults = distinctResults;
	}

}
