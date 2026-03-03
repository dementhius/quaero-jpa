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
package quaero.exception;

/**
 * Thrown when a filter or select value cannot be used as-is or coerced
 * into the expected Java type for a JPA expression.
 *
 * <p>This exception covers two scenarios:
 * <ul>
 *   <li><b>STRICT mode</b>: the received type does not exactly match the expected type.</li>
 *   <li><b>LENIENT mode</b>: a conversion was attempted but failed
 *       (e.g. the string {@code "not-a-date"} cannot be parsed as {@code LocalDate}).</li>
 * </ul>
 *
 * <p>Examples:
 * <pre>
 *   // Field 'sale_date' expects LocalDate but received Integer
 *   throw new IncorrectParameterTypeException("sale_date", LocalDate.class, Integer.class);
 *
 *   // Field 'sale_date' expects LocalDate, value is a String but unparseable
 *   throw new IncorrectParameterTypeException("sale_date", LocalDate.class, String.class,
 *       "Cannot parse \"not-a-date\" as LocalDate. Accepted: yyyy-MM-dd, dd/MM/yyyy...");
 * </pre>
 */
public class IncorrectParameterTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;
    private final Class<?> expectedType;
    private final Class<?> actualType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Basic constructor type mismatch with no extra detail.
     *
     * @param fieldName    name of the filter/select field (e.g. {@code "sale_date"})
     * @param expectedType Java type the field requires
     * @param actualType   Java type of the value that was received
     */
    public IncorrectParameterTypeException(final String fieldName, final Class<?> expectedType, final Class<?> actualType) {
        super(buildMessage(fieldName, expectedType, actualType, null));
        this.fieldName = fieldName;
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    /**
     * Constructor with an additional human-readable detail message.
     * Useful for lenient-mode parse failures where you want to include
     * the original value and accepted formats.
     *
     * @param fieldName    name of the filter/select field
     * @param expectedType Java type the field requires
     * @param actualType   Java type of the value that was received
     * @param detail       extra context appended to the message
     */
    public IncorrectParameterTypeException(final String fieldName, final Class<?> expectedType, final Class<?> actualType, final String detail) {
        super(buildMessage(fieldName, expectedType, actualType, detail));
        this.fieldName = fieldName;
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    /**
     * Constructor that wraps an underlying parse/conversion exception.
     *
     * @param fieldName    name of the filter/select field
     * @param expectedType Java type the field requires
     * @param actualType   Java type of the value that was received
     * @param cause        the underlying exception that triggered this one
     */
    public IncorrectParameterTypeException(final String fieldName, final Class<?> expectedType, final Class<?> actualType, final Throwable cause) {
        super(buildMessage(fieldName, expectedType, actualType, cause.getMessage()), cause);
        this.fieldName = fieldName;
        this.expectedType = expectedType;
        this.actualType = actualType;
    }

    /**
     * Name of the field that caused the type mismatch.
     * Matches the {@code field} value inside a {@link quaero.components.select.SelectSimpleObject}
     * or the path used in a {@link quaero.components.filter.FilterSimpleObject}.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * The Java type that was expected based on JPA entity metadata.
     */
    public Class<?> getExpectedType() {
        return expectedType;
    }

    /**
     * The Java type of the value that was actually received
     * (typically deserialized by Jackson from the request JSON).
     */
    public Class<?> getActualType() {
        return actualType;
    }

    // -------------------------------------------------------------------------
    // Message builder
    // -------------------------------------------------------------------------

    private static String buildMessage(final String fieldName, final Class<?> expected, final Class<?> actual, final String detail) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Field '").append(fieldName).append("'");
        sb.append(": expected ").append(expected != null ? expected.getSimpleName() : "unknown");
        sb.append(" but received ").append(actual != null ? actual.getSimpleName() : "null");
        if (detail != null && !detail.isEmpty()) {
            sb.append(" - ").append(detail);
        }
        return sb.toString();
    }
}
