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
package quaero.components.values;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NumericOperationType {
    AVERAGE("Avg"),
    ABSOLUTE("Abs"),
    SQUARE_ROOT("Sqrt"),
    MAX("Max"),
    MIN("Min"),
    NEGATION("Not"),
    SUM("Sum");

    private String value;

    NumericOperationType(final String i) {
        this.value = i;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static NumericOperationType fromValue(final String i) {
        for (final NumericOperationType it : NumericOperationType.values()) {
            if (it.getValue().equalsIgnoreCase(i)) {
                return it;
            }
        }
        return null;
    }
}
