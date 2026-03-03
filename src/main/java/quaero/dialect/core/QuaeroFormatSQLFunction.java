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

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import quaero.dialect.core.QuaeroFormatTranslator.Dialect;

import java.util.List;

public class QuaeroFormatSQLFunction implements SQLFunction {

	private final String nativeFunctionName;
	private final Dialect dialect;

	/**
	 * @param nativeFunctionName the SQL function name in this dialect (e.g.
	 *                           {@code "to_char"})
	 * @param dialect            the dialect to translate formats for
	 */
	public QuaeroFormatSQLFunction(final String nativeFunctionName, final QuaeroFormatTranslator.Dialect dialect) {
		this.nativeFunctionName = nativeFunctionName;
		this.dialect = dialect;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return false;
	}

	@Override
	public Type getReturnType(final Type firstArgumentType, final Mapping mapping) throws QueryException {
		return StandardBasicTypes.STRING;
	}

	/**
	 * Renders the SQL fragment, translating any format literal in the argument
	 * list.
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public String render(final Type firstArgumentType, final List args, final SessionFactoryImplementor factory)
			throws QueryException {

		if (args == null || args.isEmpty()) {
			throw new QueryException(nativeFunctionName + " requires at least one argument");
		}

		final StringBuilder sb = new StringBuilder(nativeFunctionName).append('(');

		for (int i = 0; i < args.size(); i++) {
			if (i > 0)
				sb.append(", ");

			final String arg = args.get(i).toString();

			if (i == 1 && isStringLiteral(arg)) {
				final String raw = unwrapLiteral(arg);
				final String native_ = QuaeroFormatTranslator.isNativeFormat(raw) ? raw
						: QuaeroFormatTranslator.translate(raw, dialect);
				sb.append('\'').append(native_).append('\'');
			} else {
				sb.append(arg);
			}
		}

		return sb.append(')').toString();
	}

	private static boolean isStringLiteral(final String arg) {
		return arg != null && arg.startsWith("'") && arg.endsWith("'") && arg.length() >= 2;
	}

	private static String unwrapLiteral(final String arg) {
		return arg.substring(1, arg.length() - 1);
	}
}
