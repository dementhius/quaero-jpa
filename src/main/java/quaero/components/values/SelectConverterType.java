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

public enum SelectConverterType {
    NONE("None"),
    BIG_INTEGER("BInt"),
    BIG_DECIMAL("BDec"),
    DOUBLE("Dbl"),
    FLOAT("Fl"),
    LONG("Lng"),
    INTEGER("Int"),
    STRING("Str");

    private String value;

    SelectConverterType(final String i) {
        this.value = i;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static SelectConverterType fromValue(final String i) {
        for (final SelectConverterType it : SelectConverterType.values()) {
            if (it.getValue().equals(i)) {
                return it;
            }
        }
        return null;
    }
}
