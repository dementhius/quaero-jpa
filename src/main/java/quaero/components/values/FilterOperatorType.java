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
package quaero.components.values;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FilterOperatorType {
    EQUAL("Eq"),
    NOT_EQUAL("NtEq"),
    GREATER_THAN("Gr"),
    GREATER_THAN_OR_EQUAL("GrEq"),
    LESS_THAN("Ls"),
    LESS_THAN_OR_EQUAL("LsEq"),
    IS_NULL("Nl"),
    IS_NOT_NULL("NtNl"),
    IS_EMPTY("Emp"),
    IS_NOT_EMPTY("NtEmp"),
    LIKE("Lk"),
    NOT_LIKE("NtLk"),
    IN("In"),
    NOT_IN("NtIn"),
    EQUAL_TRIM("EqTr"),
    NOT_EQUAL_TRIM("NtEqTr"),
    LIKE_TRIM("LkTr"),
    NOT_LIKE_TRIM("NtLkTr"),
    IN_TRIM("InTr"),
    NOT_IN_TRIM("NtInTr");

    private final String value;

    FilterOperatorType(final String i) {
        this.value = i;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static FilterOperatorType fromValue(final String i) {
        for (final FilterOperatorType it : FilterOperatorType.values()) {
            if (it.getValue().equalsIgnoreCase(i)) {
                return it;
            }
        }
        return null;
    }
}
