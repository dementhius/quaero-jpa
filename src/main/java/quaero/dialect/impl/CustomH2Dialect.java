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

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;
import quaero.dialect.core.AbstractQuaeroDialect;
import quaero.dialect.core.QuaeroFormatTranslator;

public class CustomH2Dialect extends H2Dialect {

    public CustomH2Dialect() {
        super();
        AbstractQuaeroDialect.buildQuaeroFunctions(QuaeroFormatTranslator.Dialect.H2)
            .forEach(this::registerFunction);
        registerNativeFunctions();
    }

    private void registerNativeFunctions() {
        registerFunction("formatdatetime", new SQLFunctionTemplate(StandardBasicTypes.STRING,      "formatdatetime(?1, ?2)"));
        registerFunction("dateadd",        new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP,   "dateadd(?1, ?2, ?3)"));
        registerFunction("datediff",       new SQLFunctionTemplate(StandardBasicTypes.LONG,        "datediff(?1, ?2, ?3)"));
        registerFunction("datetrunc",      new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP,   "datetrunc(?1, ?2)"));
        registerFunction("regexp_replace", new SQLFunctionTemplate(StandardBasicTypes.STRING,      "regexp_replace(?1, ?2, ?3)"));
        registerFunction("lpad",           new SQLFunctionTemplate(StandardBasicTypes.STRING,      "lpad(?1, ?2, ?3)"));
        registerFunction("rpad",           new SQLFunctionTemplate(StandardBasicTypes.STRING,      "rpad(?1, ?2, ?3)"));
        registerFunction("truncate",       new SQLFunctionTemplate(StandardBasicTypes.BIG_DECIMAL, "truncate(?1, ?2)"));
        registerFunction("locate",         new SQLFunctionTemplate(StandardBasicTypes.INTEGER,     "locate(?1, ?2)"));
    }
}