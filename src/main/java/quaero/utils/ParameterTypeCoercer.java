/*
 * Copyright 2026 Ddementhius
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
package quaero.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import quaero.exception.IncorrectParameterTypeException;

/**
 * Converts filter values to the Java type expected by a JPA field.
 *
 * <p>
 * In {@link CoercionMode#STRICT} mode only exact-type matches and safe numeric
 * widening promotions (e.g. {@code Integer → Long}) are allowed. Any other
 * mismatch throws {@link IncorrectParameterTypeException}.
 *
 * <p>
 * In {@link CoercionMode#LENIENT} mode a wider set of conversions is attempted
 * (String → date/number/boolean, numeric widening, etc.) before falling back to
 * the same exception.
 */
public class ParameterTypeCoercer {

	/**
	 * Coerce {@code value} to {@code targetType}, respecting {@code mode}.
	 *
	 * @param value      the raw value from the Query (never mutated)
	 * @param targetType the Java type of the JPA field
	 * @param fieldName  used only in exception messages
	 * @param mode       STRICT or LENIENT
	 * @return the value cast or converted to targetType
	 * @throws IncorrectParameterTypeException if coercion is not possible
	 */
	public static Object coerce(final Object value, final Class<?> targetType, final String fieldName,
			final CoercionMode mode) {
		if (value == null)
			return null;

		final Class<?> valueType = value.getClass();

		if (targetType.isAssignableFrom(valueType))
			return value;

		final Object widened = tryWiden(value, valueType, targetType);
		if (widened != null)
			return widened;

		if (mode == CoercionMode.STRICT) {
			throw new IncorrectParameterTypeException(fieldName, targetType, valueType);
		}

		final Object lenient = tryLenient(value, valueType, targetType, fieldName);
		if (lenient != null)
			return lenient;

		throw new IncorrectParameterTypeException(fieldName, targetType, valueType,
				"LENIENT coercion attempted but no conversion from " + valueType.getSimpleName() + " to "
						+ targetType.getSimpleName() + " is available.");
	}

	// ─────────────────────────────────────────────────────────────────────
	// STRICT — safe numeric widening only
	// ─────────────────────────────────────────────────────────────────────

	/**
	 * Lossless numeric widening and LocalDate→LocalDateTime. Returns null if no
	 * widening rule applies.
	 */
	private static Object tryWiden(final Object value, final Class<?> from, final Class<?> to) {
		if (from == Integer.class) {
			if (to == Long.class)
				return ((Integer) value).longValue();
			if (to == Double.class)
				return ((Integer) value).doubleValue();
			if (to == Float.class)
				return ((Integer) value).floatValue();
			if (to == BigDecimal.class)
				return BigDecimal.valueOf((Integer) value);
			if (to == BigInteger.class)
				return BigInteger.valueOf((Integer) value);
		}
		if (from == Long.class) {
			if (to == Double.class)
				return ((Long) value).doubleValue();
			if (to == BigDecimal.class)
				return BigDecimal.valueOf((Long) value);
			if (to == BigInteger.class)
				return BigInteger.valueOf((Long) value);
		}
		if (from == Double.class) {
			if (to == Float.class)
				return ((Double) value).floatValue();
			if (to == BigDecimal.class)
				return BigDecimal.valueOf((Double) value);
		}
		if (from == Float.class) {
			if (to == BigDecimal.class)
				return BigDecimal.valueOf(((Float) value).doubleValue());
		}
		// LocalDate → LocalDateTime is always lossless (atStartOfDay)
		if (from == LocalDate.class && to == LocalDateTime.class) {
			return ((LocalDate) value).atStartOfDay();
		}
		return null;
	}

	// ─────────────────────────────────────────────────────────────────────
	// LENIENT — wider conversions
	// ─────────────────────────────────────────────────────────────────────

	private static Object tryLenient(final Object value, final Class<?> from, final Class<?> to,
			final String fieldName) {
		if (from == String.class) {
			return tryFromString((String) value, to, fieldName);
		}
		if (to == String.class) {
			return value.toString();
		}
		// LocalDateTime → LocalDate (truncate time)
		if (from == LocalDateTime.class && to == LocalDate.class) {
			return ((LocalDateTime) value).toLocalDate();
		}

		if (from == Date.class) {
			if (to == LocalDate.class) {
				return ((Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
			}
			if (to == LocalDateTime.class) {
				return ((Date) value).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
			}
		}
		return null;
	}

	private static Object tryFromString(final String s, final Class<?> to, final String fieldName) {
		final String t = s.trim();
		try {
			if (to == LocalDate.class) {
				return parseLocalDate(t, fieldName);
			}
			
			if (to == LocalDateTime.class) {
				final LocalDateTime dt = parseLocalDateTimeOrNull(t);
				if (dt != null)
					return dt;

				return parseLocalDate(t, fieldName).atStartOfDay();
			}
			
			if (to == Date.class) {
				return java.sql.Date.valueOf(parseLocalDate(t, fieldName));
			}

			// ── Numeric ───────────────────────────────────────────────────
			if (to == Integer.class || to == int.class)
				return Integer.parseInt(t);
			if (to == Long.class || to == long.class)
				return Long.parseLong(t);
			if (to == Double.class || to == double.class)
				return Double.parseDouble(t);
			if (to == Float.class || to == float.class)
				return Float.parseFloat(t);
			if (to == BigDecimal.class)
				return new BigDecimal(t);
			if (to == BigInteger.class)
				return new BigInteger(t);

			// ── Boolean ───────────────────────────────────────────────────
			if (to == Boolean.class || to == boolean.class) {
				return parseBoolean(t, fieldName);
			}

		} catch (NumberFormatException ex) {
			throw new IncorrectParameterTypeException(fieldName, to, String.class,
					"Cannot parse \"" + t + "\" as " + to.getSimpleName() + ": " + ex.getMessage());
		}

		return null; // unsupported target type — caller will throw
	}

	// ─────────────────────────────────────────────────────────────────────
	// Date helpers
	// ─────────────────────────────────────────────────────────────────────

	private static LocalDate parseLocalDate(final String s, final String fieldName) {
		for (final DateTimeFormatter fmt : DateDeserializer.FORMATTERS) {
			try {
				return LocalDate.parse(s, fmt);
			} catch (DateTimeParseException ignored) {
				/* try next */ }
		}
		// Last resort: try datetime formats and truncate
		final LocalDateTime dt = parseLocalDateTimeOrNull(s);
		if (dt != null)
			return dt.toLocalDate();

		throw new IncorrectParameterTypeException(fieldName, LocalDate.class, String.class, "Cannot parse \"" + s
				+ "\" as a date");
	}

	private static LocalDateTime parseLocalDateTimeOrNull(final String s) {
		for (final DateTimeFormatter fmt : DateTimeDeserializer.FORMATTERS) {
			try {
				return LocalDateTime.parse(s, fmt);
			} catch (DateTimeParseException ignored) {
				/* try next */ }
		}
		return null;
	}

	// ─────────────────────────────────────────────────────────────────────
	// Boolean helper
	// ─────────────────────────────────────────────────────────────────────

	private static Boolean parseBoolean(final String s, final String fieldName) {
		switch (s.toLowerCase()) {
		case "true":
		case "t":
		case "1":
		case "yes":
		case "y":
		case "si":
		case "sí":
			return Boolean.TRUE;
		case "false":
		case "f":
		case "0":
		case "no":
		case "n":
			return Boolean.FALSE;
		default:
			throw new IncorrectParameterTypeException(fieldName, Boolean.class, String.class,
					"Cannot parse \"" + s + "\" as Boolean. " + "Accepted values: true/false, t/f, 1/0, yes/no, y/n, si/no or s/n.");
		}
	}
}
