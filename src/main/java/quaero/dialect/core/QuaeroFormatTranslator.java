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

/**
 * Translates the common Quaero date format vocabulary
 * (Oracle/PostgreSQL style) into the native format expected by each SQL engine.
 *
 * <h3>Common → Native mappings</h3>
 * <pre>
 *  Token    PG/Oracle   SQL Server   H2 (Java)   SQLite
 *  YYYY     YYYY        yyyy         yyyy         %Y
 *  YY       YY          yy           yy           %y
 *  MM       MM          MM           MM           %m
 *  DD       DD          dd           dd           %d
 *  HH24     HH24        HH           HH           %H
 *  HH       HH12        hh           hh           %I
 *  MI       MI          mm           mm           %M
 *  SS       SS          ss           ss           %S
 *  DY       DY          ddd          EEE          %a
 *  DAY      DAY         dddd         EEEE         %A
 *  MON      MON         MMM          MMM          %b
 *  MONTH    MONTH       MMMM         MMMM         %B
 *  Q        Q           (no native)  Q            (no native → formula)
 *  WW       WW          ww           ww           %W
 * </pre>
 */
public final class QuaeroFormatTranslator {

    public enum Dialect { POSTGRESQL, ORACLE, SQLSERVER, H2, SQLITE }

    private static final String[][] TOKENS = {
        // { common, pg/oracle, sqlserver, h2, sqlite  }
        { "YYYY",  "YYYY",  "yyyy",  "yyyy", "%Y"  },
        { "YY",    "YY",    "yy",    "yy",   "%y"  },
        { "MM",    "MM",    "MM",    "MM",   "%m"  },
        { "DD",    "DD",    "dd",    "dd",   "%d"  },
        { "HH24",  "HH24",  "HH",    "HH",   "%H"  },
        { "HH12",  "HH12",  "hh",    "hh",   "%I"  },
        { "HH",    "HH24",  "HH",    "HH",   "%H"  }, 
        { "MI",    "MI",    "mm",    "mm",   "%M"  },
        { "SS",    "SS",    "ss",    "ss",   "%S"  },
        { "DAY",   "DAY",   "dddd",  "EEEE", "%A"  },
        { "DY",    "DY",    "ddd",   "EEE",  "%a"  },
        { "MONTH", "MONTH", "MMMM",  "MMMM", "%B"  },
        { "MON",   "MON",   "MMM",   "MMM",  "%b"  },
        { "WW",    "WW",    "ww",    "ww",   "%W"  },
        { "Q",     "Q",     null,    "Q",    null  }, 
    };

    private QuaeroFormatTranslator() {}

    /**
     * Translate a common format string to the native format for the given dialect.
     *
     * @param commonFormat format using the common Quaero vocabulary (e.g. {@code "YYYY-MM-DD"})
     * @param dialect      target SQL engine
     * @return native format string ready to embed in SQL
     * @throws UnsupportedOperationException if a token has no equivalent in the dialect
     */
    public static String translate(final String commonFormat, final Dialect dialect) {
        if (commonFormat == null || commonFormat.isEmpty()) return commonFormat;
        final Map<String, String> replacements = buildReplacementMap(dialect);

        String result = commonFormat.toUpperCase();
        for (final Map.Entry<String, String> entry : replacements.entrySet()) {
            if (entry.getValue() == null) {
                throw new UnsupportedOperationException(
                    "Format token '" + entry.getKey() + "' has no equivalent in "
                    + dialect + ". Use a dialect-specific function instead."
                );
            }
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Returns true if the given format string is already in native dialect format
     */
    public static boolean isNativeFormat(final String format) {
        final String upper = format.toUpperCase();
        for (final String[] row : TOKENS) {
            if (upper.contains(row[0])) return false;
        }
        return true;
    }

    private static Map<String, String> buildReplacementMap(final Dialect dialect) {
        final int col = dialectColumn(dialect);
        final Map<String, String> map = new LinkedHashMap<>();
        for (final String[] row : TOKENS) {
            map.put(row[0], row[col]);
        }
        return map;
    }

    private static int dialectColumn(final Dialect dialect) {
        switch (dialect) {
            case POSTGRESQL: case ORACLE: return 1;
            case SQLSERVER:              return 2;
            case H2:                     return 3;
            case SQLITE:                 return 4;
            default: throw new IllegalArgumentException("Unknown dialect: " + dialect);
        }
    }
}
