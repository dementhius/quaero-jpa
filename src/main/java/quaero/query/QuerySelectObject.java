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

import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import quaero.components.select.ISelect;
import quaero.components.values.SelectOperatorType;
import quaero.utils.CoercionMode;

public class QuerySelectObject {

    private ISelect field;
    private String alias;
    private boolean groupBy;
    private SelectOperatorType operatorType;

    public QuerySelectObject() {

    }

    public ISelect getField() {
        return field;
    }

    public void setField(final ISelect field) {
        this.field = field;
    }

    public SelectOperatorType getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(final SelectOperatorType operatorType) {
        this.operatorType = operatorType;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(final String alias) {
        this.alias = alias;
    }

    public boolean isGroupBy() {
        return groupBy;
    }

    public void setGroupBy(final boolean groupBy) {
        this.groupBy = groupBy;
    }

    public Selection<?> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        if (field == null)
            return null;

        final Expression<?> exp = applyOperator(cb, field.resolve(mode, entity, query, cb, entities, managedTypes));

        return (alias != null && !alias.isEmpty()) ? exp.alias(alias) : exp;
    }

    @SuppressWarnings("unchecked")
    private Expression applyOperator(final CriteriaBuilder cb, final Expression exp) {
        if (operatorType == null)
            return exp;
        switch (operatorType) {
            case SUMMATORY:
                return cb.sum((Expression<Number>) exp);
            case COUNT:
                return cb.count(exp);
            case COUNT_DISTINCT:
                return cb.countDistinct(exp);
            case AVERAGE:
                return cb.avg((Expression<Number>) exp);
            case MAX:
                return cb.greatest(exp);
            case MIN:
                return cb.least(exp);
            default:
                return exp;
        }
    }

    public Expression<?> resolveGroupBy(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb,
            final Map<String, EntityType<?>> entities, final Map<String, ManagedType<?>> managedTypes) {
        if (groupBy && null != field) {
            return field.resolve(mode, entity, query, cb, entities, managedTypes);
        }
        return null;
    }

    public Map<String, Join<?, ?>> defineJoin(Root<?> entity, CriteriaQuery<?> query, Metamodel metamodel, final Map<String, Join<?, ?>> joins) {
        final Map<String, Join<?, ?>> tmpJoin = field.defineJoin(entity, query, metamodel, joins);
        if (null != tmpJoin && !tmpJoin.isEmpty()) {
            joins.putAll(tmpJoin);
        }

        return joins;
    }
}
