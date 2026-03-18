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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QueryUtils#tupleToMapList} — no Spring context required.
 *
 * <p>{@link Tuple} and {@link TupleElement} are JPA interfaces; we implement them
 * as minimal anonymous classes to avoid Mockito dependency.
 */
@SuppressWarnings("unchecked")
public class QueryUtilsTupleToMapListTest {

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Build a minimal TupleElement with a fixed alias and type. */
    private static <X> TupleElement<X> element(final String alias, final Class<X> type) {
        return new TupleElement<X>() {
            @Override public Class<? extends X> getJavaType() { return type; }
            @Override public String getAlias() { return alias; }
        };
    }

    /**
     * Build a minimal Tuple from a list of (alias, value) pairs.
     * Values are returned by position matching the elements list order.
     */
    private static Tuple tuple(final TupleElement<?>[] elements, final Object[] values) {
        return new Tuple() {
            @Override
            public List<TupleElement<?>> getElements() { return Arrays.asList(elements); }
            @Override
            public <X> X get(final TupleElement<X> tupleElement) {
                for (int i = 0; i < elements.length; i++) {
                    if (elements[i] == tupleElement) return (X) values[i];
                }
                throw new IllegalArgumentException("Element not found: " + tupleElement.getAlias());
            }
            // Unused Tuple methods — not exercised in these tests
            @Override public <X> X get(String alias, Class<X> type) { throw new UnsupportedOperationException(); }
            @Override public Object get(String alias) { throw new UnsupportedOperationException(); }
            @Override public <X> X get(int i, Class<X> type) { throw new UnsupportedOperationException(); }
            @Override public Object get(int i) { throw new UnsupportedOperationException(); }
            @Override public Object[] toArray() { throw new UnsupportedOperationException(); }
        };
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Test
    void emptyList_returnsEmptyList() {
        final List<Map<String, Object>> result = QueryUtils.tupleToMapList(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void singleFlatField_returnsSingleEntry() {
        final TupleElement<?>[] elements = {element("id", Long.class)};
        final Object[] values = {1L};

        final List<Map<String, Object>> result =
                QueryUtils.tupleToMapList(Collections.singletonList(tuple(elements, values)));

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).get("id"));
    }

    @Test
    void multipleFlatFields_allInRootMap() {
        final TupleElement<?>[] elements = {
                element("id", Long.class),
                element("name", String.class)
        };
        final Object[] values = {42L, "Corolla"};

        final Map<String, Object> row =
                QueryUtils.tupleToMapList(Collections.singletonList(tuple(elements, values))).get(0);

        assertEquals(42L, row.get("id"));
        assertEquals("Corolla", row.get("name"));
    }

    @Test
    void dotNotation_buildsNestedMap() {
        // "brand.name" → row["brand"]["name"]
        final TupleElement<?>[] elements = {
                element("name", String.class),
                element("brand.name", String.class)
        };
        final Object[] values = {"Corolla", "Toyota"};

        final Map<String, Object> row =
                QueryUtils.tupleToMapList(Collections.singletonList(tuple(elements, values))).get(0);

        assertEquals("Corolla", row.get("name"));
        final Map<String, Object> brand = (Map<String, Object>) row.get("brand");
        assertNotNull(brand);
        assertEquals("Toyota", brand.get("name"));
    }

    @Test
    void deepNesting_threeLevels() {
        // "a.b.c" → row["a"]["b"]["c"]
        final TupleElement<?>[] elements = {element("a.b.c", String.class)};
        final Object[] values = {"deep"};

        final Map<String, Object> row =
                QueryUtils.tupleToMapList(Collections.singletonList(tuple(elements, values))).get(0);

        final Map<String, Object> a = (Map<String, Object>) row.get("a");
        final Map<String, Object> b = (Map<String, Object>) a.get("b");
        assertEquals("deep", b.get("c"));
    }

    @Test
    void multipleRows_producesMultipleResults() {
        final TupleElement<?>[] elements = {element("name", String.class)};

        final List<Tuple> tuples = Arrays.asList(
                tuple(elements, new Object[]{"Corolla"}),
                tuple(elements, new Object[]{"Prius"}),
                tuple(elements, new Object[]{"Civic"})
        );

        final List<Map<String, Object>> result = QueryUtils.tupleToMapList(tuples);
        assertEquals(3, result.size());
        assertEquals("Corolla", result.get(0).get("name"));
        assertEquals("Prius", result.get(1).get("name"));
        assertEquals("Civic", result.get(2).get("name"));
    }
}
