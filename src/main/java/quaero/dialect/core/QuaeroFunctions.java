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

/**
 * Portable function names registered by every Quaero dialect.
 *
 * <p>Use these names in {@link quaero.components.select.SelectFunctionObject}
 * to write queries that work regardless of the underlying SQL engine.
 *
 * <h3>Date format vocabulary</h3>
 * All date-formatting functions accept Oracle/PostgreSQL-style format tokens:
 * <pre>
 *   YYYY  — 4-digit year          MM    — 2-digit month (01-12)
 *   DD    — 2-digit day (01-31)   HH24  — hour in 24h format (00-23)
 *   MI    — minutes (00-59)       SS    — seconds (00-59)
 *   DY    — abbreviated day name  MON   — abbreviated month name
 *   Q     — quarter (1-4)         WW    — week of year (01-53)
 * </pre>
 * Each dialect translates these tokens to its native format automatically.
 *
 * <h3>Date unit vocabulary</h3>
 * Functions that accept a time unit (truncation, diff, add) use these strings:
 * <pre>
 *   'year'    'quarter'    'month'    'week'
 *   'day'     'hour'       'minute'   'second'
 * </pre>
 */
public final class QuaeroFunctions {

    private QuaeroFunctions() {}

    /**
     * DATE-TIME
     */

    /**
     * Format a date/timestamp as a string.
     * <pre>quaero_format_date(date, 'YYYY-MM')</pre>
     * Maps to: {@code to_char} (PG/Oracle) · {@code format} (SQL Server) ·
     *          {@code formatdatetime} (H2) · {@code strftime} (SQLite)
     */
    public static final String FORMAT_DATE = "quaero_format_date";

    /**
     * Truncate a date/timestamp to the given unit.
     * <pre>quaero_trunc_date(saleDate, 'month')</pre>
     * Maps to: {@code date_trunc} (PG) · {@code trunc} (Oracle) ·
     *          {@code datetrunc} (SQL Server 2022+ / H2 2+) · {@code strftime} (SQLite)
     */
    public static final String TRUNC_DATE = "quaero_trunc_date";

    /**
     * Extract a numeric part from a date/timestamp.
     * <pre>quaero_date_part('year', saleDate)</pre>
     * Maps to: {@code date_part} / {@code EXTRACT} (PG) · {@code EXTRACT} (Oracle) ·
     *          {@code datepart} (SQL Server) · {@code EXTRACT} (H2) · {@code strftime} (SQLite)
     */
    public static final String DATE_PART = "quaero_date_part";

    /**
     * Add an interval to a date.
     * <pre>quaero_date_add(saleDate, 3, 'month')</pre>
     * Maps to: {@code date + interval} (PG) · {@code add_months} (Oracle) ·
     *          {@code dateadd} (SQL Server) · {@code dateadd} (H2) · {@code date} (SQLite)
     */
    public static final String DATE_ADD = "quaero_date_add";

    /**
     * Difference between two dates in the given unit.
     * <pre>quaero_date_diff('day', startDate, endDate)</pre>
     * Maps to: {@code date_part('day', age(d2,d1))} (PG) ·
     *          {@code round(months_between)} (Oracle) ·
     *          {@code datediff} (SQL Server / H2) · {@code julianday} (SQLite)
     */
    public static final String DATE_DIFF = "quaero_date_diff";

    /**
     * Current timestamp (no arguments).
     * <pre>quaero_now()</pre>
     * Maps to: {@code now()} (PG) · {@code sysdate} (Oracle) ·
     *          {@code getdate()} (SQL Server) · {@code now()} (H2) · {@code datetime('now')} (SQLite)
     */
    public static final String NOW = "quaero_now";

    /**
     *  STRING
     */

    /**
     * Pad a string on the left to a given length.
     * <pre>quaero_lpad(code, 10, '0')</pre>
     * Maps to: {@code lpad} (PG/Oracle/H2) · {@code right(replicate+str)} (SQL Server) ·
     *          custom UDF (SQLite)
     */
    public static final String LPAD = "quaero_lpad";

    /**
     * Pad a string on the right to a given length.
     * <pre>quaero_rpad(code, 10, ' ')</pre>
     */
    public static final String RPAD = "quaero_rpad";

    /**
     * Capitalise the first letter of each word.
     * <pre>quaero_initcap(name)</pre>
     * Maps to: {@code initcap} (PG/Oracle) · custom UPPER/LOWER (SQL Server/H2/SQLite)
     */
    public static final String INITCAP = "quaero_initcap";

    /**
     * Replace substrings matching a regular expression.
     * <pre>quaero_regexp_replace(str, '[0-9]+', '#')</pre>
     * Maps to: {@code regexp_replace} (PG/Oracle/H2) ·
     *          not available in SQL Server / SQLite (throws at registration time)
     */
    public static final String REGEXP_REPLACE = "quaero_regexp_replace";

    /**
     * Replace all occurrences of a literal substring (non-regex).
     * <pre>quaero_replace(str, 'foo', 'bar')</pre>
     * Maps to: {@code replace} everywhere — this one is universal.
     */
    public static final String REPLACE = "quaero_replace";

    /**
     * Position of a substring within a string (1-based).
     * <pre>quaero_instr(haystack, needle)</pre>
     * Maps to: {@code strpos} (PG) · {@code instr} (Oracle) ·
     *          {@code charindex} (SQL Server) · {@code locate} (H2) · {@code instr} (SQLite)
     */
    public static final String INSTR = "quaero_instr";

    /**
     * NUMERIC
     */

    /**
     * Round a number to N decimal places.
     * <pre>quaero_round(price, 2)</pre>
     * Maps to: {@code round} everywhere — this one is universal.
     */
    public static final String ROUND = "quaero_round";

    /**
     * Truncate a number to N decimal places (no rounding).
     * <pre>quaero_trunc_number(price, 2)</pre>
     * Maps to: {@code trunc} (PG/Oracle) · {@code round(?1, ?2, 1)} (SQL Server) ·
     *          {@code truncate} (H2) · {@code round} with floor trick (SQLite)
     */
    public static final String TRUNC_NUMBER = "quaero_trunc_number";

    /**
     * Logarithm in base N.
     * <pre>quaero_log(value, base)</pre>
     * Maps to: {@code log(?2, ?1)} (PG) · {@code log(?1, ?2)} (Oracle) ·
     *          {@code log(?2) / log(?1)} (SQL Server) · {@code log(?1) / log(?2)} (H2/SQLite)
     */
    public static final String LOG = "quaero_log";
}
