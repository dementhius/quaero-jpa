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
import javax.persistence.criteria.CriteriaBuilder.Case;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import quaero.utils.CoercionMode;

public class SelectConditionalObject implements ISelect {

    private List<SelectConditionValueObject> conditions;
    private ISelect otherwise;

    public List<SelectConditionValueObject> getConditions() {
        return conditions;
    }

    public void setConditions(final List<SelectConditionValueObject> conditions) {
        this.conditions = conditions;
    }

    public ISelect getOtherwise() {
        return otherwise;
    }

    public void setOtherwise(final ISelect otherwise) {
        this.otherwise = otherwise;
    }

    @Override
    public Expression<?> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        if (conditions == null || conditions.isEmpty())
            return null;

        Case selectCase = cb.selectCase();
        for (final SelectConditionValueObject cond : conditions) {
            selectCase = selectCase.when(cond.getCondition().resolve(mode, entity, query, cb, entities, managedTypes),
                    cond.getValue().resolve(mode, entity, query, cb, entities, managedTypes));
        }

        return otherwise != null ? selectCase.otherwise(otherwise.resolve(mode, entity, query, cb, entities, managedTypes))
                : selectCase.otherwise(cb.nullLiteral(Object.class));
    }

    @Override
    public Map<String, Join<?, ?>> defineJoin(final Root<?> entity, final CriteriaQuery<?> query, final Metamodel metamodel,
            final Map<String, Join<?, ?>> joinMaps) {
        if (conditions != null) {
            for (final SelectConditionValueObject cond : conditions) {
                cond.getValue().defineJoin(entity, query, metamodel, joinMaps);
            }
        }

        if (otherwise != null) {
            otherwise.defineJoin(entity, query, metamodel, joinMaps);
        }

        return joinMaps;
    }

}
