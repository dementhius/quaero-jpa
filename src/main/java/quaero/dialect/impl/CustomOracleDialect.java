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

import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import quaero.dialect.core.AbstractQuaeroDialect;
import quaero.dialect.core.QuaeroFormatTranslator;

public class CustomOracleDialect extends Oracle12cDialect {

    public CustomOracleDialect() {
        super();
        AbstractQuaeroDialect.buildQuaeroFunctions(QuaeroFormatTranslator.Dialect.ORACLE)
            .forEach(this::registerFunction);
        registerNativeFunctions();
    }

    private void registerNativeFunctions() {
        registerFunction("to_char",        new SQLFunctionTemplate(StandardBasicTypes.STRING,      "to_char(?1, ?2)"));
        registerFunction("to_date",        new SQLFunctionTemplate(StandardBasicTypes.DATE,        "to_date(?1, ?2)"));
        registerFunction("to_timestamp",   new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP,   "to_timestamp(?1, ?2)"));
        registerFunction("trunc",          new SQLFunctionTemplate(StandardBasicTypes.DATE,        "trunc(?1, ?2)"));
        registerFunction("add_months",     new SQLFunctionTemplate(StandardBasicTypes.DATE,        "add_months(?1, ?2)"));
        registerFunction("months_between", new SQLFunctionTemplate(StandardBasicTypes.DOUBLE,      "months_between(?1, ?2)"));
        registerFunction("last_day",       new StandardSQLFunction("last_day",                     StandardBasicTypes.DATE));
        registerFunction("next_day",       new SQLFunctionTemplate(StandardBasicTypes.DATE,        "next_day(?1, ?2)"));
        registerFunction("lpad",           new SQLFunctionTemplate(StandardBasicTypes.STRING,      "lpad(?1, ?2, ?3)"));
        registerFunction("rpad",           new SQLFunctionTemplate(StandardBasicTypes.STRING,      "rpad(?1, ?2, ?3)"));
        registerFunction("initcap",        new StandardSQLFunction("initcap",                      StandardBasicTypes.STRING));
        registerFunction("translate",      new SQLFunctionTemplate(StandardBasicTypes.STRING,      "translate(?1, ?2, ?3)"));
        registerFunction("regexp_replace", new SQLFunctionTemplate(StandardBasicTypes.STRING,      "regexp_replace(?1, ?2, ?3)"));
        registerFunction("regexp_substr",  new SQLFunctionTemplate(StandardBasicTypes.STRING,      "regexp_substr(?1, ?2)"));
        registerFunction("log",            new SQLFunctionTemplate(StandardBasicTypes.DOUBLE,      "log(?1, ?2)"));
    }
}