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

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.apache.commons.lang3.StringUtils;

import quaero.components.values.SelectConverterType;
import quaero.components.values.SelectOperatorType;
import quaero.query.QueryJoinTypesObject.QuaeroJoinType;
import quaero.utils.CoercionMode;
import quaero.utils.QueryUtils;

/**
 * Clase para generar un parametro a devolver en una SELECT que sea uno de los parametros que se encuentran en la tabla de BBDD
 */
public class SelectSimpleObject implements ISelect {
    private final Logger logger = Logger.getLogger(SelectSimpleObject.class.getName());

    private String field;
    private String entityAlias;
    private SelectOperatorType operatorType;
    private SelectConverterType converterType;
    private List<QuaeroJoinType> joinTypes;

    public String getField() {
        return field;
    }

    public void setField(final String field) {
        this.field = field;
    }

    public SelectOperatorType getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(final SelectOperatorType operatorType) {
        this.operatorType = operatorType;
    }

    public SelectConverterType getConverterType() {
        return converterType;
    }

    public void setConverterType(SelectConverterType converterType) {
        this.converterType = converterType;
    }

    public String getEntityAlias() {
        return entityAlias;
    }

    public void setEntityAlias(final String entityAlias) {
        this.entityAlias = entityAlias;
    }

    public List<QuaeroJoinType> getJoinTypes() {
        return joinTypes;
    }

    public void setJoinTypes(List<QuaeroJoinType> joinTypes) {
        this.joinTypes = joinTypes;
    }

    @Override
    public Expression<?> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        Path entityParams = null;
        if (entityAlias != null) {
            final Root<?> foreignEntity = query.getRoots().stream().filter(x -> entityAlias.equals(x.getAlias())).findFirst().orElse(null);
            if (foreignEntity == null) {
                throw new NullPointerException("RequestSimpleObject. Foreign Entity Alias not found: " + entityAlias);
            }
            entityParams = QueryUtils.getParamByName(foreignEntity, field);
        } else {
            entityParams = QueryUtils.getParamByName(entity, field);
        }

        if (null == operatorType) {
            return convertParam(cb, entityParams);
        }

        switch (operatorType) {
            case COUNT:
                return cb.count(convertParam(cb, entityParams));
            case COUNT_DISTINCT:
                return cb.countDistinct(convertParam(cb, entityParams));
            case SUMMATORY:
                return cb.sum(convertParam(cb, entityParams));
            case AVERAGE:
                return cb.avg(convertParam(cb, entityParams));
            case MAX:
                return cb.greatest(convertParam(cb, entityParams));
            case MIN:
                return cb.least(convertParam(cb, entityParams));
            default:
                return convertParam(cb, entityParams);
        }
    }

    private Expression convertParam(final CriteriaBuilder cb, final Path entityParams) {
        if (null == converterType) {
            return entityParams;
        }
        switch (converterType) {
            case BIG_DECIMAL:
                return cb.toBigDecimal(entityParams);
            case BIG_INTEGER:
                return cb.toBigInteger(entityParams);
            case DOUBLE:
                return cb.toDouble(entityParams);
            case FLOAT:
                return cb.toFloat(entityParams);
            case LONG:
                return cb.toLong(entityParams);
            case INTEGER:
                return cb.toInteger(entityParams);
            case STRING:
                return cb.toString(entityParams);
            default:
                return entityParams;
        }
    }

    @Override
    public Map<String, Join<?, ?>> defineJoin(Root<?> entity, CriteriaQuery<?> query, Metamodel metamodel, Map<String, Join<?, ?>> joinMaps) {
        Attribute attribute;

        final String param = getField();
        final List<QuaeroJoinType> joinTypes = getJoinTypes();

        Root<?> root;
        if (isValid(entityAlias)) {
            root = query.getRoots().stream().filter(x -> entityAlias.equals(x.getAlias())).findFirst().orElse(null);
        } else {
            root = query.getRoots().stream().filter(ent -> ent.getAlias().equalsIgnoreCase(entity.getAlias())).findFirst().orElse(null);
        }

        if (param != null && !param.isEmpty() && joinTypes != null && !joinTypes.isEmpty()) {
            if (param.contains(QueryUtils.ENTITY_FIELD_SEPARATOR)) {
                final String[] params = StringUtils.splitByWholeSeparator(param, QueryUtils.ENTITY_FIELD_SEPARATOR);
                String prevParam = null;
                Class<?> prevEntity = null;
                String currentLevelParam = "";
                for (int i = 0; i < params.length; i++) {
                    final String paramLoop = params[i];
                    if (currentLevelParam.isEmpty()) {
                        currentLevelParam = paramLoop;
                    } else {
                        currentLevelParam += QueryUtils.ENTITY_FIELD_SEPARATOR + paramLoop;
                    }
                    if (joinTypes.size() > i) {
                        final JoinType joinType = QueryUtils.getJoinType(joinTypes.get(i));
                        if (prevParam == null) {
                            attribute = root.getModel().getAttribute(paramLoop);
                            prevEntity = attribute.getJavaType();
                        } else {
                            attribute = metamodel.entity(prevEntity).getAttribute(paramLoop);
                            prevEntity = attribute.getJavaType();
                        }

                        if (!joinMaps.containsKey(currentLevelParam)) {
                            if (QueryUtils.isJoinParameter(attribute)) {
                                if (prevParam == null) {
                                    final Join<Object, Object> join = root.join(paramLoop, joinType);
                                    joinMaps.put(currentLevelParam, join);
                                } else {
                                    final Join<Object, Object> join = joinMaps.get(prevParam).join(paramLoop, joinType);
                                    joinMaps.put(currentLevelParam, join);
                                }
                            }
                        } else {
                            final Join<?, ?> prevJoin = joinMaps.get(currentLevelParam);
                            if (!((Join<?, ?>) prevJoin).getJoinType().equals(joinType)) {
                                logger.warning("defineJoin. Cannot change previous join with " + prevJoin.getJavaType().getSimpleName() + " from "
                                        + ((Join<?, ?>) prevJoin).getJoinType() + " to " + joinType);
                            }
                        }
                    }
                    // Save previous param
                    prevParam = currentLevelParam;
                }
            } else {
                joinMaps.put(param, root.join(param, QueryUtils.getJoinType(joinTypes.get(0))));
            }
        }

        return joinMaps;
    }

    private boolean isValid(final String txt) {
        return null != txt && !txt.isEmpty();
    }
}
