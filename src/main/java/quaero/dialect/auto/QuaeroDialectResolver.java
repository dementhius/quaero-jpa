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
package quaero.dialect.auto;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import quaero.dialect.impl.CustomH2Dialect;
import quaero.dialect.impl.CustomOracleDialect;
import quaero.dialect.impl.CustomPostgreSQLDialect;
import quaero.dialect.impl.CustomSQLServerDialect;
import quaero.dialect.impl.CustomSQLiteDialect;

public class QuaeroDialectResolver implements DialectResolver {

	private static final long serialVersionUID = 7044786235665024045L;

	@Override
	public Dialect resolveDialect(final DialectResolutionInfo info) {
		final String name = info.getDatabaseName();

		if (name == null)
			return null;

		final String nameLower = name.toLowerCase();

		if (nameLower.contains("postgresql") || nameLower.contains("cockroach")) {
			return new CustomPostgreSQLDialect();
		}
		if (nameLower.contains("oracle")) {
			return new CustomOracleDialect();
		}
		if (nameLower.contains("sql server") || nameLower.contains("sqlserver") || nameLower.contains("microsoft")) {
			return new CustomSQLServerDialect();
		}
		if (nameLower.contains("h2")) {
			return new CustomH2Dialect();
		}
		if (nameLower.contains("sqlite")) {
			return new CustomSQLiteDialect();
		}
		return null;
	}
}
