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

import quaero.utils.CoercionMode;

/**
 * Clase para generar una cadena de texto con los parametros en una SELECT que sea la concatenacion de literales o campos en la tabla de BBDD
 *
 */
public class SelectConcatObject implements ISelect {

    private List<ISelect> values;

    public List<ISelect> getValues() {
        return values;
    }

    public void setValues(final List<ISelect> values) {
        this.values = values;
    }

    @Override
    public Expression<?> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        if (values.size() == 1) {
            return values.get(0).resolve(mode, entity, query, cb, entities, managedTypes);
        } else {
            Expression<String> finalConcat = values.get(0).resolve(mode, entity, query, cb, entities, managedTypes).as(String.class);

            for (int i = 1; i < values.size(); i++) {
                finalConcat = cb.concat(finalConcat, values.get(i).resolve(mode, entity, query, cb, entities, managedTypes).as(String.class));
            }
            return finalConcat;
        }
    }

    @Override
    public Map<String, Join<?, ?>> defineJoin(final Root<?> entity, final CriteriaQuery<?> query, final Metamodel metamodel,
            final Map<String, Join<?, ?>> joinMaps) {
        if (values != null) {
            for (final ISelect val : values) {
                val.defineJoin(entity, query, metamodel, joinMaps);
            }
        }
        return joinMaps;
    }

}
