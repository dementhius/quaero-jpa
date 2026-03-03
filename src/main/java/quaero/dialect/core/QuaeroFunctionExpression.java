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
package quaero.dialect.core;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.ExpressionImpl;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fixes the Hibernate 5 GROUP BY bug for function expressions containing
 * literals, AND handles portable {@code quaero_*} function name/format
 * translation.
 */
public class QuaeroFunctionExpression<T> extends ExpressionImpl<T> implements Renderable, Serializable {

	private final String functionName;
	private final List<Expression<?>> arguments;

	private QuaeroFunctionExpression(final CriteriaBuilderImpl cb, final Class<T> returnType, final String functionName,
			final List<Expression<?>> arguments) {
		super(cb, returnType);
		this.functionName = functionName;
		this.arguments = arguments;
	}

	/**
	 * Creates an expression safe for both SELECT and GROUP BY.
	 *
	 * @param cb           must be Hibernate's {@code CriteriaBuilderImpl}
	 * @param functionName registered function name, e.g.
	 *                     {@code "quaero_format_date"}
	 * @param returnType   Java return type
	 * @param arguments    function arguments
	 * @throws IllegalArgumentException if {@code cb} is not Hibernate's
	 *                                  CriteriaBuilderImpl
	 */
	public static <T> Expression<T> of(final CriteriaBuilder cb, final String functionName, final Class<T> returnType,
			final Expression<?>... arguments) {
		if (!(cb instanceof CriteriaBuilderImpl)) {
			throw new IllegalArgumentException("QuaeroFunctionExpression requires Hibernate's CriteriaBuilderImpl. "
					+ "Received: " + cb.getClass().getName());
		}
		return new QuaeroFunctionExpression<>((CriteriaBuilderImpl) cb, returnType, functionName,
				Arrays.asList(arguments));
	}

	/**
	 * Renders the complete SQL fragment for both SELECT and GROUP BY.
	 */
	@Override
	public String render(final RenderingContext renderingContext) {
		try {
			final SessionFactoryImplementor sfi = criteriaBuilder().getEntityManagerFactory()
					.unwrap(SessionFactoryImplementor.class);
			final org.hibernate.dialect.function.SQLFunction registered = sfi.getDialect().getFunctions()
					.get(functionName);

			if (registered != null) {
				final List<String> renderedArgs = new ArrayList<>();
				for (final Expression<?> arg : arguments) {
					renderedArgs.add(renderArg(arg, renderingContext));
				}
				return registered.render(sfi.getTypeHelper().basic(getJavaType()), renderedArgs, sfi);
			}
		} catch (final Exception ignored) {
		}

		final StringBuilder sb = new StringBuilder(functionName).append('(');
		for (int i = 0; i < arguments.size(); i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(renderArg(arguments.get(i), renderingContext));
		}
		return sb.append(')').toString();
	}

	/**
	 * Renders a single function argument to its SQL string representation.
	 *
	 * <p>{@link org.hibernate.query.criteria.internal.expression.LiteralExpression#render}
	 * binds the value as a {@code ?} parameter, which breaks GROUP BY / ORDER BY
	 * matching. String and numeric literals are inlined directly so that the SQL
	 * fragment is textually identical in SELECT, GROUP BY, and ORDER BY.
	 */
	private static String renderArg(final Expression<?> arg, final RenderingContext rc) {
		if (arg instanceof org.hibernate.query.criteria.internal.expression.LiteralExpression) {
			final Object val =
					((org.hibernate.query.criteria.internal.expression.LiteralExpression<?>) arg).getLiteral();
			if (val == null)   return "null";
			if (val instanceof String) return "'" + val + "'";
			return val.toString();
		}
		return ((Renderable) arg).render(rc);
	}

	@Override
	public String renderProjection(final RenderingContext renderingContext) {
		return render(renderingContext);
	}

	@Override
	public void registerParameters(final ParameterRegistry registry) {
		for (final Expression<?> arg : arguments) {
			try {
				final java.lang.reflect.Method m = arg.getClass().getMethod("registerParameters",
						ParameterRegistry.class);
				m.invoke(arg, registry);
			} catch (final Exception ignored) {
			}
		}
	}
}