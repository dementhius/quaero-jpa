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
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import quaero.components.select.ISelect;
import quaero.components.values.SelectOperatorType;
import quaero.utils.CoercionMode;

public class QueryOrderObject {
    private ISelect field;
    private SelectOperatorType operatorType;
    private boolean ascending;
    private String entityName;
    private String entityAlias;

    public ISelect getField() {
        return field;
    }

    public void setField(final ISelect field) {
        this.field = field;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(final boolean ascending) {
        this.ascending = ascending;
    }

    public SelectOperatorType getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(final SelectOperatorType operatorType) {
        this.operatorType = operatorType;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityAlias() {
        return entityAlias;
    }

    public void setEntityAlias(String entityAlias) {
        this.entityAlias = entityAlias;
    }

    public Order resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        if (null == field) {
            return null;
        }

        final From<?, ?> entityToUse = getToUseEntity(query, entity);
        Expression exp = field.resolve(mode, entityToUse, query, cb, entities, managedTypes);

        if (null != operatorType) {
            switch (operatorType) {
                case SUMMATORY:
                    exp = cb.sum((Expression<Number>) exp);
                    break;
                case COUNT:
                    exp = cb.count(exp);
                    break;
                case COUNT_DISTINCT:
                    exp = cb.countDistinct(exp);
                    break;
                case AVERAGE:
                    exp = cb.avg((Expression<Number>) exp);
                    break;
                case MAX:
                    exp = cb.greatest(exp);
                    break;
                case MIN:
                    exp = cb.least(exp);
                    break;
            }
        }
        if (ascending) {
            return cb.asc(exp);
        }
        return cb.desc(exp);
    }

    private From<?, ?> getToUseEntity(final CriteriaQuery<?> query, final From<?, ?> defaultEntity) {
        From<?, ?> toReturn = null;
        if (isValid(entityName) || isValid(entityAlias)) {
            final Set<Root<?>> queryEntities = query.getRoots();

            if (isValid(entityAlias)) {
                toReturn = queryEntities.stream().filter(ent -> ent.getAlias().equalsIgnoreCase(entityAlias)).findFirst().orElse(null);
            } else {
                toReturn = queryEntities.stream().filter(ent -> ent.getModel().getName().equalsIgnoreCase(entityName)).findFirst().orElse(null);
            }
        }
        return null != toReturn ? toReturn : defaultEntity;
    }

    private boolean isValid(final String txt) {
        return null != txt && !txt.isEmpty();
    }
}
