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

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

/**
 * Builds the full set of portable {@code quaero_*} functions for a given
 * dialect.
 */
public final class AbstractQuaeroDialect {

    private AbstractQuaeroDialect() {
    }

    /**
     * Returns the complete set of portable {@code quaero_*} functions for the given dialect.
     *
     * @param dialectId which SQL engine to build functions for
     * @return ordered map of function name → SQLFunction, ready for bulk registration
     */
    public static Map<String, SQLFunction> buildQuaeroFunctions(final QuaeroFormatTranslator.Dialect dialectId) {

        final Map<String, SQLFunction> map = new LinkedHashMap<>();
        buildDateFunctions(map, dialectId);
        buildStringFunctions(map, dialectId);
        buildNumericFunctions(map, dialectId);
        return map;
    }

    /**
     * DATE FUNCTIONS
     */

    private static void buildDateFunctions(final Map<String, SQLFunction> map, final QuaeroFormatTranslator.Dialect d) {
        map.put(QuaeroFunctions.FORMAT_DATE, new QuaeroFormatSQLFunction(formatDateFn(d), d));
        map.put(QuaeroFunctions.TRUNC_DATE, new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP, truncDateTemplate(d)));
        map.put(QuaeroFunctions.DATE_PART, new SQLFunctionTemplate(StandardBasicTypes.DOUBLE, datePartTemplate(d)));
        map.put(QuaeroFunctions.DATE_ADD, new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP, dateAddTemplate(d)));
        map.put(QuaeroFunctions.DATE_DIFF, new SQLFunctionTemplate(StandardBasicTypes.LONG, dateDiffTemplate(d)));
        map.put(QuaeroFunctions.NOW, new SQLFunctionTemplate(StandardBasicTypes.TIMESTAMP, nowTemplate(d)));
    }

    /**
     * STRING FUNCTIONS
     */

    private static void buildStringFunctions(final Map<String, SQLFunction> map, final QuaeroFormatTranslator.Dialect d) {

        map.put(QuaeroFunctions.LPAD, new SQLFunctionTemplate(StandardBasicTypes.STRING, lpadTemplate(d)));

        map.put(QuaeroFunctions.RPAD, new SQLFunctionTemplate(StandardBasicTypes.STRING, rpadTemplate(d)));

        map.put(QuaeroFunctions.INITCAP, new SQLFunctionTemplate(StandardBasicTypes.STRING, initcapTemplate(d)));

        map.put(QuaeroFunctions.REPLACE, new SQLFunctionTemplate(StandardBasicTypes.STRING, "replace(?1, ?2, ?3)"));

        final String regexpTpl = regexpReplaceTemplate(d);
        if (regexpTpl != null) {
            map.put(QuaeroFunctions.REGEXP_REPLACE, new SQLFunctionTemplate(StandardBasicTypes.STRING, regexpTpl));
        }

        map.put(QuaeroFunctions.INSTR, new SQLFunctionTemplate(StandardBasicTypes.INTEGER, instrTemplate(d)));
    }

    /**
     * NUMERIC FUNCTIONS
     */

    private static void buildNumericFunctions(final Map<String, SQLFunction> map, final QuaeroFormatTranslator.Dialect d) {

        map.put(QuaeroFunctions.ROUND, new SQLFunctionTemplate(StandardBasicTypes.BIG_DECIMAL, "round(?1, ?2)"));

        map.put(QuaeroFunctions.TRUNC_NUMBER, new SQLFunctionTemplate(StandardBasicTypes.BIG_DECIMAL, truncNumberTemplate(d)));

        map.put(QuaeroFunctions.LOG, new SQLFunctionTemplate(StandardBasicTypes.DOUBLE, logTemplate(d)));
    }

    private static String formatDateFn(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
            case ORACLE:
                return "to_char";
            case SQLSERVER:
                return "format";
            case H2:
                return "formatdatetime";
            case SQLITE:
                return "strftime";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String truncDateTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
                return "date_trunc(?2, ?1)";
            case ORACLE:
                return "trunc(?1, " + oracleTruncUnitExpr() + ")";
            case SQLSERVER:
                return "datetrunc(?2, ?1)";
            case H2:
                return "datetrunc(?2, ?1)";
            case SQLITE:
                return "date(?1, 'start of ' || ?2)";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String oracleTruncUnitExpr() {
        return "CASE ?2 " + "WHEN 'year'    THEN 'YYYY' " + "WHEN 'quarter' THEN 'Q'    " + "WHEN 'month'   THEN 'MM'   " + "WHEN 'week'    THEN 'IW'   "
                + "WHEN 'day'     THEN 'DD'   " + "WHEN 'hour'    THEN 'HH'   " + "WHEN 'minute'  THEN 'MI'   " + "ELSE ?2 END";
    }

    private static String datePartTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
                return "date_part(?1, ?2)";
            case ORACLE:
                return "CASE ?1 " + "WHEN 'year'   THEN EXTRACT(YEAR   FROM ?2) " + "WHEN 'month'  THEN EXTRACT(MONTH  FROM ?2) "
                        + "WHEN 'day'    THEN EXTRACT(DAY    FROM ?2) " + "WHEN 'hour'   THEN EXTRACT(HOUR   FROM ?2) "
                        + "WHEN 'minute' THEN EXTRACT(MINUTE FROM ?2) " + "WHEN 'second' THEN EXTRACT(SECOND FROM ?2) " + "ELSE NULL END";
            case SQLSERVER:
                return "CASE ?1 " + "WHEN 'year'   THEN DATEPART(year,   ?2) " + "WHEN 'month'  THEN DATEPART(month,  ?2) "
                        + "WHEN 'day'    THEN DATEPART(day,    ?2) " + "WHEN 'hour'   THEN DATEPART(hour,   ?2) " + "WHEN 'minute' THEN DATEPART(minute, ?2) "
                        + "WHEN 'second' THEN DATEPART(second, ?2) " + "ELSE NULL END";
            case H2:
                return "CASE ?1 " + "WHEN 'year'   THEN YEAR(?2)   " + "WHEN 'month'  THEN MONTH(?2)  " + "WHEN 'day'    THEN DAY(?2)    "
                        + "WHEN 'hour'   THEN HOUR(?2)   " + "WHEN 'minute' THEN MINUTE(?2) " + "WHEN 'second' THEN SECOND(?2) " + "ELSE NULL END";
            case SQLITE:
                return "cast(strftime(" + "CASE ?1 " + "WHEN 'year'   THEN '%Y' " + "WHEN 'month'  THEN '%m' " + "WHEN 'day'    THEN '%d' "
                        + "WHEN 'hour'   THEN '%H' " + "WHEN 'minute' THEN '%M' " + "WHEN 'second' THEN '%S' " + "ELSE ?1 END, ?2) as integer)";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String dateAddTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
                return "(?1 + (?2 || ' ' || ?3)::interval)";
            case ORACLE:
                return "CASE ?3 " + "WHEN 'year'   THEN add_months(?1, ?2 * 12) " + "WHEN 'month'  THEN add_months(?1, ?2) " + "WHEN 'day'    THEN ?1 + ?2 "
                        + "WHEN 'hour'   THEN ?1 + ?2/24 " + "WHEN 'minute' THEN ?1 + ?2/1440 " + "ELSE ?1 + ?2 END";
            case SQLSERVER:
                return "dateadd(?3, ?2, ?1)";
            case H2:
                return "dateadd(?3, ?2, ?1)";
            case SQLITE:
                return "datetime(?1, '+' || ?2 || ' ' || ?3 || 's')";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String dateDiffTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
                return "CASE ?1 " + "WHEN 'year'   THEN date_part('year', age(?3, ?2)) "
                        + "WHEN 'month'  THEN (date_part('year', age(?3, ?2)) * 12 + date_part('month', age(?3, ?2))) "
                        + "WHEN 'day'    THEN (?3::date - ?2::date) " + "WHEN 'hour'   THEN extract(epoch from (?3 - ?2)) / 3600 "
                        + "WHEN 'minute' THEN extract(epoch from (?3 - ?2)) / 60 " + "ELSE extract(epoch from (?3 - ?2)) END";
            case ORACLE:
                return "CASE ?1 " + "WHEN 'year'   THEN round(months_between(?3, ?2) / 12) " + "WHEN 'month'  THEN round(months_between(?3, ?2)) "
                        + "WHEN 'day'    THEN round(?3 - ?2) " + "WHEN 'hour'   THEN round((?3 - ?2) * 24) " + "WHEN 'minute' THEN round((?3 - ?2) * 1440) "
                        + "ELSE round((?3 - ?2) * 86400) END";
            case SQLSERVER:
                return "datediff(?1, ?2, ?3)";
            case H2:
                return "datediff(?1, ?2, ?3)";
            case SQLITE:
                return "CASE ?1 " + "WHEN 'day'   THEN cast(julianday(?3) - julianday(?2) as integer) "
                        + "WHEN 'hour'  THEN cast((julianday(?3) - julianday(?2)) * 24 as integer) "
                        + "WHEN 'month' THEN cast((strftime('%Y', ?3) - strftime('%Y', ?2)) * 12 " + "+ strftime('%m', ?3) - strftime('%m', ?2) as integer) "
                        + "WHEN 'year'  THEN cast(strftime('%Y', ?3) - strftime('%Y', ?2) as integer) "
                        + "ELSE cast(julianday(?3) - julianday(?2) as integer) END";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String nowTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
                return "now()";
            case ORACLE:
                return "sysdate";
            case SQLSERVER:
                return "getdate()";
            case H2:
                return "now()";
            case SQLITE:
                return "datetime('now')";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String lpadTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
            case ORACLE:
            case H2:
                return "lpad(?1, ?2, ?3)";
            case SQLSERVER:
                return "right(replicate(?3, ?2) + cast(?1 as varchar(max)), ?2)";
            case SQLITE:
                return "substr(replace(hex(zeroblob(?2)), '00', ?3), 1, ?2 - length(?1)) || ?1";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String rpadTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
            case ORACLE:
            case H2:
                return "rpad(?1, ?2, ?3)";
            case SQLSERVER:
                return "left(cast(?1 as varchar(max)) + replicate(?3, ?2), ?2)";
            case SQLITE:
                return "?1 || substr(replace(hex(zeroblob(?2)), '00', ?3), 1, ?2 - length(?1))";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String initcapTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
            case ORACLE:
                return "initcap(?1)";
            case SQLSERVER:
                return "upper(left(?1, 1)) + lower(substring(?1, 2, len(?1)))";
            case H2:
                return "upper(left(?1, 1)) || lower(substring(?1, 2))";
            case SQLITE:
                return "upper(substr(?1, 1, 1)) || lower(substr(?1, 2))";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String regexpReplaceTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
            case ORACLE:
            case H2:
                return "regexp_replace(?1, ?2, ?3)";
            case SQLSERVER:
            case SQLITE:
                return null;
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String instrTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
                return "strpos(?1, ?2)";
            case ORACLE:
            case SQLITE:
                return "instr(?1, ?2)";
            case SQLSERVER:
                return "charindex(?2, ?1)";
            case H2:
                return "locate(?2, ?1)";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String truncNumberTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
            case ORACLE:
                return "trunc(?1, ?2)";
            case SQLSERVER:
                return "round(?1, ?2, 1)";
            case H2:
                return "truncate(?1, ?2)";
            case SQLITE:
                return "round(?1 - sign(?1) * 0.5 * power(10, -?2), ?2)";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }

    private static String logTemplate(final QuaeroFormatTranslator.Dialect d) {
        switch (d) {
            case POSTGRESQL:
            case ORACLE:
                return "log(?2, ?1)";
            case SQLSERVER:
            case H2:
            case SQLITE:
                return "(log(?1) / log(?2))";
            default:
                throw new IllegalArgumentException("Unknown dialect: " + d);
        }
    }
}
