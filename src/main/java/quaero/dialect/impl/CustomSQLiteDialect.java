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

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.sqlite.hibernate.dialect.SQLiteDialect;

import quaero.dialect.core.AbstractQuaeroDialect;
import quaero.dialect.core.QuaeroFormatTranslator;

/**
 * Requires com.github.gwenn:sqlite-dialect on the classpath for Hibernate 5.
 * The base class org.hibernate.dialect.SQLiteDialect is provided by that
 * library.
 */
public class CustomSQLiteDialect extends SQLiteDialect {

	public CustomSQLiteDialect() {
		super();
		AbstractQuaeroDialect.buildQuaeroFunctions(QuaeroFormatTranslator.Dialect.SQLITE)
				.forEach(this::registerFunction);
		registerNativeFunctions();
	}

	private void registerNativeFunctions() {
		registerFunction("strftime", new SQLFunctionTemplate(StandardBasicTypes.STRING, "strftime(?1, ?2)"));
		registerFunction("date", new StandardSQLFunction("date", StandardBasicTypes.DATE));
		registerFunction("time", new StandardSQLFunction("time", StandardBasicTypes.STRING));
		registerFunction("datetime", new StandardSQLFunction("datetime", StandardBasicTypes.TIMESTAMP));
		registerFunction("julianday", new StandardSQLFunction("julianday", StandardBasicTypes.DOUBLE));
		registerFunction("instr", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "instr(?1, ?2)"));
	}
}