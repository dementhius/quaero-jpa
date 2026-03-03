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
package quaero.dialect.impl;

import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import quaero.dialect.core.AbstractQuaeroDialect;
import quaero.dialect.core.QuaeroFormatTranslator;

public class CustomPostgreSQLDialect extends PostgreSQL95Dialect {

	public CustomPostgreSQLDialect() {
		super();
		AbstractQuaeroDialect.buildQuaeroFunctions(QuaeroFormatTranslator.Dialect.POSTGRESQL)
				.forEach(this::registerFunction);
		registerNativeFunctions();
	}

	private void registerNativeFunctions() {
		registerFunction("to_char", new SQLFunctionTemplate(StandardBasicTypes.STRING, "to_char(?1, ?2)"));
		registerFunction("date_trunc", new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP, "date_trunc(?1, ?2)"));
		registerFunction("date_part", new SQLFunctionTemplate(StandardBasicTypes.DOUBLE, "date_part(?1, ?2)"));
		registerFunction("age", new SQLFunctionTemplate(StandardBasicTypes.STRING, "age(?1, ?2)"));
		registerFunction("make_date", new SQLFunctionTemplate(StandardBasicTypes.DATE, "make_date(?1, ?2, ?3)"));
		registerFunction("to_timestamp", new StandardSQLFunction("to_timestamp", StandardBasicTypes.TIMESTAMP));
		registerFunction("regexp_replace",
				new SQLFunctionTemplate(StandardBasicTypes.STRING, "regexp_replace(?1, ?2, ?3)"));
		registerFunction("regexp_substr", new SQLFunctionTemplate(StandardBasicTypes.STRING, "regexp_substr(?1, ?2)"));
		registerFunction("lpad", new SQLFunctionTemplate(StandardBasicTypes.STRING, "lpad(?1, ?2, ?3)"));
		registerFunction("rpad", new SQLFunctionTemplate(StandardBasicTypes.STRING, "rpad(?1, ?2, ?3)"));
		registerFunction("initcap", new StandardSQLFunction("initcap", StandardBasicTypes.STRING));
		registerFunction("translate", new SQLFunctionTemplate(StandardBasicTypes.STRING, "translate(?1, ?2, ?3)"));
		registerFunction("trunc", new SQLFunctionTemplate(StandardBasicTypes.BIG_DECIMAL, "trunc(?1, ?2)"));
		registerFunction("log", new SQLFunctionTemplate(StandardBasicTypes.DOUBLE, "log(?1, ?2)"));
	}
}