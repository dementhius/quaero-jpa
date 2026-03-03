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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import quaero.dialect.core.QuaeroFunctionExpression;
import quaero.utils.CoercionMode;

/**
 * Calls a registered SQL function with a list of {@link ISelect} arguments.
 *
 * <p>
 * Use the portable {@code quaero_*} function names from
 * {@link quaero.dialect.core.QuaeroFunctions} to write engine-agnostic queries.
 *
 * <h3>Hibernate 5 GROUP BY fix</h3> Hibernate 5 has a bug where
 * {@link CriteriaBuilder#function} expressions that contain literal arguments
 * are not reproduced correctly in GROUP BY clauses. This class automatically
 * uses {@link QuaeroFunctionExpression} which overrides Hibernate's internal
 * render path so that the full SQL fragment is emitted identically in both
 * SELECT and GROUP BY.
 */
public class SelectFunctionObject implements ISelect {

	private String function;
	private String returnType;
	private List<ISelect> params = new ArrayList<>();

	private transient Expression<?> cachedExpression;

	@Override
	public Expression<?> resolve(final CoercionMode mode, final From<?, ?> entity, final CriteriaQuery<?> query,
			final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
			final Map<String, ManagedType<?>> managedTypes) {

		if (params == null || params.isEmpty() || function == null || function.isEmpty()) {
			return null;
		}

		if (cachedExpression != null) {
			return cachedExpression;
		}

		final List<Expression<?>> exList = new ArrayList<>();
		for (final ISelect sel : params) {
			exList.add(sel.resolve(mode, entity, query, cb, entities, managedTypes));
		}
		final Expression<?>[] exArray = exList.toArray(new Expression<?>[0]);

		// Determinar clase de retorno y construir expresión.
		// Usamos QuaeroFunctionExpression para que SELECT, GROUP BY y ORDER BY
		// emitan exactamente el mismo fragmento SQL (fix bug Hibernate 5 GROUP BY).
		try {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			final Expression<?> qfe = QuaeroFunctionExpression.of(cb, function, (Class) resolveReturnType(), exArray);
			cachedExpression = qfe;
		} catch (final IllegalArgumentException e) {
			// Fallback para proveedores JPA que no sean Hibernate
			cachedExpression = cb.function(function, resolveReturnType(), exArray);
		}
		return cachedExpression;
	}

	private Class<?> resolveReturnType() {
		if (returnType != null && !returnType.isEmpty()) {
			try {
				return Class.forName(returnType);
			} catch (ClassNotFoundException ignored) {
			}
		}

		if (function != null) {
			final String fn = function.toLowerCase();

			if (fn.contains("date_part")
					|| fn.contains("datepart")
					|| fn.contains("extract")
					|| fn.contains("date_diff")
					|| fn.contains("datediff")
					|| fn.contains("round")
					|| fn.contains("trunc_number")
					|| fn.contains("log")
					|| fn.contains("instr")
					|| fn.contains("locate")) {
				return Double.class;
			}

			if (fn.contains("to_char")
					|| fn.contains("format_date")
					|| fn.contains("formatdatetime")
					|| fn.contains("strftime")
					|| fn.contains("trunc_date")
					|| fn.contains("date_add")
					|| fn.contains("substring")
					|| fn.contains("lpad")
					|| fn.contains("rpad")
					|| fn.contains("initcap")
					|| fn.contains("replace")
					|| fn.contains("regexp_replace")) {
				return String.class;
			}
		}

		// 3 — Último recurso
		return Object.class;
	}

	@Override
	public Map<String, Join<?, ?>> defineJoin(final Root<?> entity,
			final CriteriaQuery<?> query,
			final Metamodel metamodel,
			final Map<String, Join<?, ?>> joinMaps) {
		final Map<String, Join<?, ?>> joins = new HashMap<>();
		for (final ISelect val : params) {
			final Map<String, Join<?, ?>> tmpJoin = val.defineJoin(entity, query, metamodel, joinMaps);
			if (tmpJoin != null && !tmpJoin.isEmpty()) {
				joins.putAll(tmpJoin);
			}
		}
		return joins;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(final String function) {
		this.function = function;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(final String returnType) {
		this.returnType = returnType;
	}

	public List<ISelect> getParams() {
		return params;
	}

	public void setParams(final List<ISelect> params) {
		this.params = params;
	}
}
