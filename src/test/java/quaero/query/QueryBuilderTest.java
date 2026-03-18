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
package quaero.query;

import org.junit.jupiter.api.Test;

import quaero.components.filter.FilterArrayObject;
import quaero.components.filter.FilterArrayObject.FilterOperationType;
import quaero.components.filter.FilterSimpleObject;
import quaero.components.filter.IFilter;
import quaero.components.select.SelectSimpleObject;
import quaero.components.values.FilterOperatorType;
import quaero.components.values.SelectOperatorType;
import quaero.utils.CoercionMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QueryBuilder} — no Spring context required.
 */
public class QueryBuilderTest {

    @Test
    void builder_setsTableName() {
        final Query q = QueryBuilder.builder("Sale").build();
        assertEquals("Sale", q.getTableName());
    }

    @Test
    void alias_setsTableAlias() {
        final Query q = QueryBuilder.builder("Sale").alias("s").build();
        assertEquals("s", q.getTableAlias());
    }

    @Test
    void noAlias_defaultsToTableName() {
        final Query q = QueryBuilder.builder("Sale").build();
        assertEquals("Sale", q.getTableAlias());
    }

    @Test
    void select_addsFieldToSelects() {
        final Query q = QueryBuilder.builder("Sale").select("finalPrice").as("price").build();
        assertNotNull(q.getSelects());
        assertEquals(1, q.getSelects().size());
        final QuerySelectObject sel = q.getSelects().get(0);
        assertEquals("price", sel.getAlias());
        assertTrue(sel.getField() instanceof SelectSimpleObject);
        assertEquals("finalPrice", ((SelectSimpleObject) sel.getField()).getField());
    }

    @Test
    void select_withCountAggregate() {
        final Query q = QueryBuilder.builder("Sale").select("id").count().as("total").build();
        assertEquals(SelectOperatorType.COUNT, q.getSelects().get(0).getOperatorType());
    }

    @Test
    void select_withGroupBy() {
        final Query q = QueryBuilder.builder("Sale").select("status").groupBy().build();
        assertTrue(q.getSelects().get(0).isGroupBy());
    }

    @Test
    void noSelects_selectsIsNull() {
        final Query q = QueryBuilder.builder("Sale").build();
        assertNull(q.getSelects());
    }

    @Test
    void filterEqual_singleFilter_noWrapping() {
        final Query q = QueryBuilder.builder("Sale").filterEqual("status", "ACTIVE").build();
        assertNotNull(q.getFilter());
        assertTrue(q.getFilter() instanceof FilterSimpleObject);
        final FilterSimpleObject f = (FilterSimpleObject) q.getFilter();
        assertEquals(FilterOperatorType.EQUAL, f.getOperatorType());
    }

    @Test
    void multipleFilters_wrappedInAndArray() {
        final Query q = QueryBuilder.builder("Sale")
                .filterEqual("status", "ACTIVE")
                .filterGreaterThan("finalPrice", 1000)
                .build();
        assertTrue(q.getFilter() instanceof FilterArrayObject);
        final FilterArrayObject arr = (FilterArrayObject) q.getFilter();
        assertEquals(FilterOperationType.AND, arr.getOperation());
        assertEquals(2, arr.getFilters().length);
    }

    @Test
    void filterOperation_or_wrapsInOrArray() {
        final Query q = QueryBuilder.builder("Sale")
                .filterOperation(FilterOperationType.OR)
                .filterEqual("status", "A")
                .filterEqual("status", "B")
                .build();
        final FilterArrayObject arr = (FilterArrayObject) q.getFilter();
        assertEquals(FilterOperationType.OR, arr.getOperation());
    }

    @Test
    void filter_rawIFilter_addedDirectly() {
        final IFilter complex = FilterArrayObject.or(
                FilterSimpleObject.equal("x", 1),
                FilterSimpleObject.equal("y", 2));
        final Query q = QueryBuilder.builder("Sale").filter(complex).build();
        assertSame(complex, q.getFilter());
    }

    @Test
    void filterBetween_wrapsGteAndLte() {
        final Query q = QueryBuilder.builder("Sale")
                .filterBetween("finalPrice", 100, 500)
                .build();
        // filterBetween adds an AND array with 2 conditions
        assertTrue(q.getFilter() instanceof FilterArrayObject);
        assertEquals(2, ((FilterArrayObject) q.getFilter()).getFilters().length);
    }

    @Test
    void orderAsc_orderDesc() {
        final Query q = QueryBuilder.builder("Sale")
                .orderAsc("createdAt")
                .orderDesc("finalPrice")
                .build();
        assertNotNull(q.getOrders());
        assertEquals(2, q.getOrders().size());
        assertTrue(q.getOrders().get(0).isAscending());
        assertFalse(q.getOrders().get(1).isAscending());
    }

    @Test
    void noOrders_ordersIsNull() {
        assertNull(QueryBuilder.builder("Sale").build().getOrders());
    }

    @Test
    void page_setsPaginationFields() {
        final Query q = QueryBuilder.builder("Sale").page(2, 20).build();
        assertEquals(Integer.valueOf(2), q.getPageIndex());
        assertEquals(Integer.valueOf(20), q.getPageSize());
    }

    @Test
    void distinct_setsFlag() {
        assertTrue(QueryBuilder.builder("Sale").distinct().build().getDistinctResults());
    }

    @Test
    void coercionMode_propagatesToQuery() {
        final Query q = QueryBuilder.builder("Sale").coercionMode(CoercionMode.LENIENT).build();
        assertEquals(CoercionMode.LENIENT, q.getCoercionMode());
    }
}
