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
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import quaero.utils.CoercionMode;

/**
 * Generates a SUBSTRING expression over a field.
 */
public class SelectSubstringObject implements ISelect {

    private ISelect value;
    private int position;
    private Integer length;

    public ISelect getValue() {
        return value;
    }

    public void setValue(final ISelect value) {
        this.value = value;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(final Integer length) {
        this.length = length;
    }

    @Override
    public Expression<?> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        if (value == null || position < 0)
            return null;

        final Expression<String> field = value.resolve(mode, entity, query, cb, entities, managedTypes).as(String.class);

        return length == null ? cb.substring(field, position) : cb.substring(field, position, length);
    }

    @Override
    public Map<String, Join<?, ?>> defineJoin(final Root<?> entity, final CriteriaQuery<?> query, final Metamodel metamodel,
            final Map<String, Join<?, ?>> joinMaps) {
        if (value != null) {
            value.defineJoin(entity, query, metamodel, joinMaps);
        }
        return joinMaps;
    }

}
