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
package quaero.components.select;

import java.util.Collection;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import quaero.utils.CoercionMode;

/**
 * Returns a literal value or null literal.
 * No joins are ever required for a literal, so defineJoin uses the default no-op.
 */
public class SelectValueObject implements ISelect {

    private Object value;
    
    public SelectValueObject() {}
    
    public SelectValueObject(final Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    @Override
    public Expression<?> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        if (value == null || value instanceof Collection || (value instanceof String && "null".equalsIgnoreCase((String) value))) {
            return cb.nullLiteral(Object.class);
        }
        return cb.literal(value);
    }

}
