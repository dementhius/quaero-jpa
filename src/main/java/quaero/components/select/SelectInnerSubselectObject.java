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
package quaero.components.select;

import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import quaero.components.filter.IFilter;
import quaero.utils.CoercionMode;

public class SelectInnerSubselectObject implements ISelect {

    private String tableName;
    private ISelect select;
    private IFilter filter;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public ISelect getSelect() {
        return select;
    }

    public void setSelect(ISelect select) {
        this.select = select;
    }

    public IFilter getFilter() {
        return filter;
    }

    public void setFilter(IFilter filter) {
        this.filter = filter;
    }

    @Override
    public Expression<?> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        if (tableName == null || select == null)
            return null;

        final EntityType<?> tableEntity = entities.get(tableName);
        if (tableEntity == null) {
            throw new IllegalArgumentException("SelectInnerSubselect. Entity not found: " + tableName);
        }

        final Subquery<Object> subQu = query.subquery(Object.class);
        final Root<?> subRoot = subQu.from(tableEntity);

        final Expression subExp = select.resolve(mode, subRoot, query, cb, entities, managedTypes);
        subQu.select(subExp);

        if (filter != null) {
            final Predicate wherePredicate = filter.resolve(mode, subRoot, query, cb, entities, managedTypes);
            if (wherePredicate != null) {
                subQu.where(wherePredicate);
            }
        }

        return subQu.getSelection();
    }

    @Override
    public Map<String, Join<?, ?>> defineJoin(final Root<?> entity, final CriteriaQuery<?> query, final Metamodel metamodel,
            final Map<String, Join<?, ?>> joinMaps) {
        if (select != null) {
            select.defineJoin(entity, query, metamodel, joinMaps);
        }
        return joinMaps;
    }

}
