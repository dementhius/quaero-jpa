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
package quaero.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.apache.commons.lang3.StringUtils;

import quaero.query.QueryJoinTypesObject.QuaeroJoinType;

public class QueryUtils {
    /** 
     * Field path separator. Always use StringUtils.splitByWholeSeparator, never String.split() with this value. 
     */
    public static final String ENTITY_FIELD_SEPARATOR = ".";

    public static final JoinType getJoinType(final QuaeroJoinType jt) {
        if (jt != null) {
            switch (jt) {
                case LEFT:
                    return JoinType.LEFT;
                case RIGHT:
                    return JoinType.RIGHT;
                case INNER:
                    return JoinType.INNER;
                default:
                    return JoinType.INNER;
            }
        }
        return null;
    }

    public static final boolean isJoinParameter(final Attribute attribute) {
        if (attribute instanceof SingularAttribute) {
            final SingularAttribute<?, ?> singularAttribute = (SingularAttribute<?, ?>) attribute;
            if (singularAttribute.isAssociation()) {
                return true;
            }
        }
        // Check if the attribute is a PluralAttribute (OneToMany)
        else if (attribute instanceof PluralAttribute) {
            final PluralAttribute<?, ?, ?> pluralAttribute = (PluralAttribute<?, ?, ?>) attribute;
            if (pluralAttribute.getElementType().getPersistenceType() == Type.PersistenceType.ENTITY) {
                return true;
            }
        }
        return false;
    }

    public static Path<?> getParamByName(final From<?, ?> entity, final String fieldName) {
        Path<?> entityPath = null;

        if (fieldName.contains(ENTITY_FIELD_SEPARATOR)) {
            final String[] params = StringUtils.splitByWholeSeparator(fieldName, ENTITY_FIELD_SEPARATOR);

            From<?, ?> mainEntity = entity;
            From<?, ?> prevMain = null;
            boolean searchJoins = true;
            
            for (int i = 0; i < params.length; i++) {
                if (i < params.length - 1) {
                    if (!searchJoins) {
                        entityPath = entityPath.get(params[i]);
                    } else {
                        final int idx = i;
                        prevMain = mainEntity;
                        mainEntity = mainEntity.getJoins().stream().filter(x -> params[idx].equals(x.getAttribute().getName())).findFirst().orElse(null);

                        if (mainEntity == null) {
                            entityPath = prevMain.get(params[i]);
                            searchJoins = false;
                        }
                    }
                } else if (!searchJoins) {
                    entityPath = entityPath.get(params[i]);
                } else {
                    entityPath = mainEntity.get(params[i]);
                }
            }
        } else {
            entityPath = entity.get(fieldName);
        }

        return entityPath;
    }

    public static List<Map<String, Object>> tupleToMapList(final List<Tuple> tupleList) {
        final List<Map<String, Object>> toReturn = new ArrayList<>();

        for (final Tuple tpl : tupleList) {
            final Map<String, Object> rootMap = new LinkedHashMap<>();
            for (final TupleElement<?> tmpTElement : tpl.getElements()) {
                final String[] tplAlias = tmpTElement.getAlias().split("\\.");
                Map<String, Object> currentMap = rootMap;
                for (int j = 0; j < tplAlias.length - 1; j++) {
                    final Object node = currentMap.get(tplAlias[j]);
                    if (node == null) {
                        final Map<String, Object> childMap = new LinkedHashMap<>();
                        currentMap.put(tplAlias[j].trim(), childMap);
                        currentMap = childMap;
                    } else {
                        currentMap = (Map<String, Object>) node;
                    }
                }
                currentMap.put(tplAlias[tplAlias.length - 1], tpl.get(tmpTElement));
            }
            toReturn.add(rootMap);
        }

        return toReturn;
    }
}
