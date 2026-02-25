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

/**
 * Controls how Quaero handles type mismatches between the value received
 * in the {@link quaero.query.Query} and the Java type expected by the JPA field.
 *
 * <p>Set per-request on the {@link quaero.query.Query} object:
 * <pre>{@code
 * // JSON
 * { "tableName": "Sale", "coercionMode": "LENIENT", ... }
 *
 * // Java builder
 * QueryBuilder.builder("Sale").coercionMode(CoercionMode.LENIENT)...build();
 * }</pre>
 *
 * <p>Defaults to {@link #STRICT} when not specified.
 */
public enum CoercionMode {

    /**
     * No automatic type conversion is performed.
     *
     * <p>The value received must already match the Java type of the JPA field.
     * Any mismatch throws {@link quaero.exception.IncorrectParameterTypeException}.
     */
    STRICT,

    /**
     * Best-effort automatic type conversion is performed before the comparison.
     *
     * <p>Conversions attempted:
     * <ul>
     *   <li>{@code String} → {@code LocalDate} / {@code LocalDateTime} / {@code Date}
     *       (ISO-8601 and common locale formats)</li>
     *   <li>{@code String} → {@code Integer} / {@code Long} / {@code Double} /
     *       {@code Float} / {@code BigDecimal} / {@code BigInteger}</li>
     *   <li>{@code String} → {@code Boolean} ("true"/"false"/"1"/"0"/"yes"/"no")</li>
     *   <li>{@code Integer} → {@code Long} / {@code Double} / {@code BigDecimal}</li>
     *   <li>{@code Long} → {@code BigDecimal} / {@code Double}</li>
     *   <li>{@code LocalDate} → {@code LocalDateTime} (atStartOfDay)</li>
     * </ul>
     *
     * <p>If conversion is not possible, {@link quaero.exception.IncorrectParameterTypeException}
     * is still thrown — LENIENT only means "try harder before failing".
     */
    LENIENT
}