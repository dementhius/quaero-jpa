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

import quaero.components.values.NumericOperationType;
import quaero.utils.CoercionMode;

/**
 * Generates a SELECT expression that applies a single numeric operation to a field.
 */
public class SelectNumericOperationObject implements ISelect {

    private NumericOperationType operation;
    private ISelect field;

    public SelectNumericOperationObject() {
        super();
    }

    public NumericOperationType getOperation() {
        return operation;
    }

    public void setOperation(final NumericOperationType operation) {
        this.operation = operation;
    }

    public ISelect getField() {
        return field;
    }

    public void setField(final ISelect field) {
        this.field = field;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Expression<? extends Number> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb,
            final Map<String, EntityType<?>> entities, final Map<String, ManagedType<?>> managedTypes) {
        if (operation == null || field == null)
            return null;

        final Expression<? extends Number> exp = (Expression<? extends Number>) field.resolve(mode, entity, query, cb, entities, managedTypes);

        switch (operation) {
            case AVERAGE:
                return cb.avg(exp);
            case ABSOLUTE:
                return cb.abs(exp);
            case SQUARE_ROOT:
                return cb.sqrt(exp);
            case MAX:
                return cb.max(exp);
            case MIN:
                return cb.min(exp);
            case NEGATION:
                return cb.neg(exp);
            case SUM:
                return cb.sum(exp);
            default:
                return null;
        }
    }

    @Override
    public Map<String, Join<?, ?>> defineJoin(final Root<?> entity, final CriteriaQuery<?> query, final Metamodel metamodel,
            final Map<String, Join<?, ?>> joinMaps) {
        if (field != null) {
            field.defineJoin(entity, query, metamodel, joinMaps);
        }
        return joinMaps;
    }
}
