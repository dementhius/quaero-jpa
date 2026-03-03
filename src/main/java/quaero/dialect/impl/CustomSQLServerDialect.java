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

import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;
import quaero.dialect.core.AbstractQuaeroDialect;
import quaero.dialect.core.QuaeroFormatTranslator;

public class CustomSQLServerDialect extends SQLServer2012Dialect {

	public CustomSQLServerDialect() {
		super();
		AbstractQuaeroDialect.buildQuaeroFunctions(QuaeroFormatTranslator.Dialect.SQLSERVER)
				.forEach(this::registerFunction);
		registerNativeFunctions();
	}

	private void registerNativeFunctions() {
		registerFunction("format", new SQLFunctionTemplate(StandardBasicTypes.STRING, "format(?1, ?2)"));
		registerFunction("datepart", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "datepart(?1, ?2)"));
		registerFunction("datename", new SQLFunctionTemplate(StandardBasicTypes.STRING, "datename(?1, ?2)"));
		registerFunction("dateadd", new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP, "dateadd(?1, ?2, ?3)"));
		registerFunction("datediff", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "datediff(?1, ?2, ?3)"));
		registerFunction("convert", new SQLFunctionTemplate(StandardBasicTypes.STRING, "convert(?1, ?2, ?3)"));
		registerFunction("iif", new SQLFunctionTemplate(StandardBasicTypes.STRING, "iif(?1, ?2, ?3)"));
		registerFunction("isnull", new SQLFunctionTemplate(StandardBasicTypes.STRING, "isnull(?1, ?2)"));
		registerFunction("replicate", new SQLFunctionTemplate(StandardBasicTypes.STRING, "replicate(?1, ?2)"));
		registerFunction("charindex", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "charindex(?1, ?2)"));
		registerFunction("stuff", new SQLFunctionTemplate(StandardBasicTypes.STRING, "stuff(?1, ?2, ?3, ?4)"));
	}
}