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
package quaero.components.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import com.fasterxml.jackson.annotation.JsonValue;

import quaero.utils.CoercionMode;

public class FilterArrayObject implements IFilter {

	public enum FilterOperationType {
		AND("and"), OR("or");

		private String desc;

		private FilterOperationType(final String desc) {
			this.desc = desc;
		}

		@JsonValue
		public String getDescription() {
			return desc;
		}

		public static FilterOperationType fromValue(final String i) {
			for (final FilterOperationType it : FilterOperationType.values()) {
				if (it.getDescription().equalsIgnoreCase(i)) {
					return it;
				}
			}
			return null;
		}
	}

	private FilterOperationType operation;
	private IFilter[] filters;

	public FilterArrayObject() {

	}

	public FilterOperationType getOperation() {
		return operation;
	}

	public void setOperation(FilterOperationType operation) {
		this.operation = operation;
	}

	public IFilter[] getFilters() {
		return filters;
	}

	public void setFilters(IFilter[] filter) {
		this.filters = filter;
	}

	@Override
	public Predicate resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb,
			Map<String, EntityType<?>> entities, final Map<String, ManagedType<?>> managedTypes) {

		if (filters == null)
			return null;

		final List<Predicate> predicates = Arrays.stream(filters).filter(Objects::nonNull)
				.map(f -> f.resolve(mode,entity, query, cb, entities, managedTypes)).filter(Objects::nonNull)
				.collect(Collectors.toList());

		if (predicates.isEmpty())
			return null;

		final Predicate[] arr = predicates.toArray(new Predicate[predicates.size()]);

		return FilterOperationType.OR.equals(operation) ? cb.or(arr) : cb.and(arr);
	}

	public static FilterArrayObject and(final IFilter... filters) {
		final FilterArrayObject f = new FilterArrayObject();
		f.setOperation(FilterOperationType.AND);
		f.setFilters(filters);
		return f;
	}

	public static FilterArrayObject or(final IFilter... filters) {
		final FilterArrayObject f = new FilterArrayObject();
		f.setOperation(FilterOperationType.OR);
		f.setFilters(filters);
		return f;
	}

}
