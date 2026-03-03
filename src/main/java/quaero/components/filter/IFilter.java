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
package quaero.components.filter;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import quaero.utils.CoercionMode;

@JsonTypeInfo(use = NAME, include = PROPERTY)
@JsonSubTypes({ 
        @JsonSubTypes.Type(value = FilterSimpleObject.class, name = "FilterSimple"),
        @JsonSubTypes.Type(value = FilterArrayObject.class, name = "FilterArray"),
        @JsonSubTypes.Type(value = FilterQueryObject.class, name = "FilterQuery") })
public interface IFilter {

    Predicate resolve(final CoercionMode mode, final From<?, ?> mainEntity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes);

}
