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
package quaero.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import quaero.exception.IncorrectParameterTypeException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ParameterTypeCoercer} — no Spring context required.
 */
public class ParameterTypeCoercerTest {

    // ─── null ────────────────────────────────────────────────────────────────

    @Test
    void null_returnsNull() {
        assertNull(ParameterTypeCoercer.coerce(null, String.class, "f", CoercionMode.STRICT));
    }

    // ─── Exact match ─────────────────────────────────────────────────────────

    @Test
    void strict_exactMatch_returnsValue() {
        assertEquals("hello",
                ParameterTypeCoercer.coerce("hello", String.class, "f", CoercionMode.STRICT));
    }

    @Test
    void strict_assignableSubtype_returnsValue() {
        // Integer is assignable to Number
        final Object result = ParameterTypeCoercer.coerce(42, Number.class, "f", CoercionMode.STRICT);
        assertEquals(42, result);
    }

    // ─── STRICT widening ─────────────────────────────────────────────────────

    @Test
    void strict_widenIntToLong() {
        final Object result = ParameterTypeCoercer.coerce(5, Long.class, "f", CoercionMode.STRICT);
        assertEquals(5L, result);
        assertInstanceOf(Long.class, result);
    }

    @Test
    void strict_widenIntToDouble() {
        final Object result = ParameterTypeCoercer.coerce(3, Double.class, "f", CoercionMode.STRICT);
        assertEquals(3.0, result);
    }

    @Test
    void strict_widenIntToBigDecimal() {
        final Object result = ParameterTypeCoercer.coerce(7, BigDecimal.class, "f", CoercionMode.STRICT);
        assertEquals(BigDecimal.valueOf(7), result);
    }

    @Test
    void strict_widenLongToBigDecimal() {
        final Object result = ParameterTypeCoercer.coerce(100L, BigDecimal.class, "f", CoercionMode.STRICT);
        assertEquals(BigDecimal.valueOf(100L), result);
    }

    @Test
    void strict_localDateToLocalDateTime() {
        final LocalDate date = LocalDate.of(2026, 3, 16);
        final Object result = ParameterTypeCoercer.coerce(date, LocalDateTime.class, "f", CoercionMode.STRICT);
        assertEquals(date.atStartOfDay(), result);
    }

    @Test
    void strict_incompatibleType_throws() {
        assertThrows(IncorrectParameterTypeException.class,
                () -> ParameterTypeCoercer.coerce("text", Integer.class, "f", CoercionMode.STRICT));
    }

    // ─── LENIENT conversions ─────────────────────────────────────────────────

    @Test
    void lenient_stringToInteger() {
        final Object result = ParameterTypeCoercer.coerce("42", Integer.class, "f", CoercionMode.LENIENT);
        assertEquals(42, result);
    }

    @Test
    void lenient_stringToLong() {
        final Object result = ParameterTypeCoercer.coerce("999", Long.class, "f", CoercionMode.LENIENT);
        assertEquals(999L, result);
    }

    @Test
    void lenient_stringToBigDecimal() {
        final Object result = ParameterTypeCoercer.coerce("3.14", BigDecimal.class, "f", CoercionMode.LENIENT);
        assertEquals(new BigDecimal("3.14"), result);
    }

    @Test
    void lenient_stringToLocalDate_isoFormat() {
        final Object result = ParameterTypeCoercer.coerce("2026-03-16", LocalDate.class, "f", CoercionMode.LENIENT);
        assertEquals(LocalDate.of(2026, 3, 16), result);
    }

    @Test
    void lenient_stringToBoolean_true_variants() {
        for (final String s : new String[]{"true", "True", "1", "yes", "y", "si", "t"}) {
            assertEquals(Boolean.TRUE,
                    ParameterTypeCoercer.coerce(s, Boolean.class, "f", CoercionMode.LENIENT),
                    "Expected TRUE for: " + s);
        }
    }

    @Test
    void lenient_stringToBoolean_false_variants() {
        for (final String s : new String[]{"false", "False", "0", "no", "n", "f"}) {
            assertEquals(Boolean.FALSE,
                    ParameterTypeCoercer.coerce(s, Boolean.class, "f", CoercionMode.LENIENT),
                    "Expected FALSE for: " + s);
        }
    }

    @Test
    void lenient_localDateTimeToLocalDate() {
        final LocalDateTime dt = LocalDateTime.of(2026, 3, 16, 10, 30);
        final Object result = ParameterTypeCoercer.coerce(dt, LocalDate.class, "f", CoercionMode.LENIENT);
        assertEquals(LocalDate.of(2026, 3, 16), result);
    }

    @Test
    void lenient_anyToString() {
        final Object result = ParameterTypeCoercer.coerce(123, String.class, "f", CoercionMode.LENIENT);
        assertEquals("123", result);
    }

    @Test
    void lenient_unparsableString_throws() {
        assertThrows(IncorrectParameterTypeException.class,
                () -> ParameterTypeCoercer.coerce("notAnInt", Integer.class, "f", CoercionMode.LENIENT));
    }
}
