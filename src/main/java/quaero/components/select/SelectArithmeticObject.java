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

import java.util.List;
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

import quaero.components.values.ArithmeticOperationType;
import quaero.utils.CoercionMode;

/**
 * Generates a SELECT expression that applies an arithmetic operation
 * across a list of ISelect fields.
 */
public class SelectArithmeticObject implements ISelect {

    private ArithmeticOperationType operation;
    private List<ISelect> fields;

    public SelectArithmeticObject() {
        super();
    }

    public ArithmeticOperationType getOperation() {
        return operation;
    }

    public void setOperation(final ArithmeticOperationType operation) {
        this.operation = operation;
    }

    public List<ISelect> getFields() {
        return fields;
    }

    public void setFields(final List<ISelect> fields) {
        this.fields = fields;
    }

    @Override
    public Expression<? extends Number> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb,
            final Map<String, EntityType<?>> entities, final Map<String, ManagedType<?>> managedTypes) {
        if (fields == null || fields.isEmpty())
            return null;

        // Single field � no operation needed
        if (fields.size() == 1) {
            return (Expression<? extends Number>) fields.get(0).resolve(mode, entity, query, cb, entities, managedTypes);
        }

        if (operation == null)
            return null;

        switch (operation) {
            case SUMMATION:
                return fields.stream()
                        .map(f -> (Expression<Number>) f.resolve(mode, entity, query, cb, entities, managedTypes))
                        .reduce((a, b) -> (Expression<Number>) cb.sum(a, b))
                        .orElse(null);
            case DIFFERENCE:
                return fields.stream()
                        .map(f -> (Expression<Number>) f.resolve(mode, entity, query, cb, entities, managedTypes))
                        .reduce((a, b) -> (Expression<Number>) cb.diff(a, b))
                        .orElse(null);
            case MULTIPLY:
                return fields.stream()
                        .map(f -> (Expression<Number>) f.resolve(mode, entity, query, cb, entities, managedTypes))
                        .reduce((a, b) -> (Expression<Number>) cb.prod(a, b))
                        .orElse(null);
            case DIVISION:
                return fields.stream()
                        .map(f -> (Expression<Number>) f.resolve(mode, entity, query, cb, entities, managedTypes))
                        .reduce((a, b) -> (Expression<Number>) cb.quot(a, b))
                        .orElse(null);
            case MOD:
                return fields.stream()
                        .map(f -> cb.toInteger((Expression<? extends Number>) f.resolve(mode, entity, query, cb, entities, managedTypes)))
                        .reduce((a, b) -> cb.mod(a, b))
                        .orElse(null);

            default:
                return null;
        }
    }

    @Override
    public Map<String, Join<?, ?>> defineJoin(final Root<?> entity, final CriteriaQuery<?> query, final Metamodel metamodel,
            final Map<String, Join<?, ?>> joinMaps) {
        if (fields != null) {
            for (final ISelect field : fields) {
                field.defineJoin(entity, query, metamodel, joinMaps);
            }
        }
        return joinMaps;
    }
}
