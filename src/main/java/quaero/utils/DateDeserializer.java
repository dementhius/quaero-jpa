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

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class DateDeserializer extends JsonDeserializer<LocalDate> {

    public static final List<DateTimeFormatter> FORMATTERS = Collections.unmodifiableList(Arrays.asList(
            DateTimeFormatter.ISO_LOCAL_DATE, // 2024-01-15
            DateTimeFormatter.ofPattern("dd/MM/yyyy"), // 15/01/2024
            DateTimeFormatter.ofPattern("dd-MM-yyyy"), // 15-01-2024
            DateTimeFormatter.ofPattern("yyyyMMdd"), // 20240115
            DateTimeFormatter.ofPattern("yyyy/MM/dd"), // 2024/01/15
            DateTimeFormatter.ofPattern("yyyy-MM-dd"), // 2024-01-15
            DateTimeFormatter.ofPattern("d/M/yyyy"),    // 5/1/2024
            DateTimeFormatter.ofPattern("MM/dd/yyyy")   // 01/15/2024  (US format)
    ));

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        final String value = p.getText().trim();

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (final DateTimeParseException ignored) {
            }
        }

        throw new RuntimeException("Cannot parse date: " + value);
    }
}
