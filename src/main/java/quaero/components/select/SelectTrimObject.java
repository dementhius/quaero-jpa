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
import javax.persistence.criteria.CriteriaBuilder.Trimspec;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import quaero.utils.CharacterDeserializer;
import quaero.utils.CoercionMode;

public class SelectTrimObject implements ISelect {
    private ISelect field;
    @JsonDeserialize(using = CharacterDeserializer.class)
    private Character character;
    private Trimspec spec;

    public ISelect getField() {
        return field;
    }

    public void setField(ISelect field) {
        this.field = field;
    }

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public Trimspec getSpec() {
        return spec;
    }

    public void setSpec(Trimspec spec) {
        this.spec = spec;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Expression<?> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        if (field == null)
            return null;

        final Expression<String> exp = (Expression<String>) field.resolve(mode, entity, query, cb, entities, managedTypes);

        return character != null ? cb.trim(spec, character, exp) : cb.trim(spec, exp);
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
