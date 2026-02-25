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

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import quaero.utils.CoercionMode;

@JsonTypeInfo(use = NAME, include = PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = SelectValueObject.class, name = "SelectValue"),
        @JsonSubTypes.Type(value = SelectSimpleObject.class, name = "SelectSimple"),
        @JsonSubTypes.Type(value = SelectCoalesceObject.class, name = "SelectCoalesce"),
        @JsonSubTypes.Type(value = SelectConcatObject.class, name = "SelectConcat"),
        @JsonSubTypes.Type(value = SelectConditionalObject.class, name = "SelectConditional"),
        @JsonSubTypes.Type(value = SelectArithmeticObject.class, name = "SelectArithmetic"),
        @JsonSubTypes.Type(value = SelectNumericOperationObject.class, name = "SelectNumericOperation"),
        @JsonSubTypes.Type(value = SelectSubstringObject.class, name = "SelectSubstring"),
        @JsonSubTypes.Type(value = SelectFunctionObject.class, name = "SelectFunction"),
        @JsonSubTypes.Type(value = SelectInnerSubselectObject.class, name = "SelectInnerSubselect"),
        @JsonSubTypes.Type(value = SelectTrimObject.class, name = "SelectTrim") })
public interface ISelect {
    
    /**
     * Resolves this select node into a JPA {@link Expression}.
     * Called after all joins have been defined via {@link #defineJoin}.
     */
    Expression<?> resolve(final CoercionMode mode, final From<?, ?> entity, CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes);

    /**
     * Defines any JPA joins required by this select node and accumulates them
     * into the shared {@code joinMaps}.
     *
     * <p>Default implementation is a no-op that safely returns the received map
     * unchanged. Implementations that need joins (e.g. {@link SelectSimpleObject})
     * override this method.
     *
     * <p><b>Contract:</b> always return {@code joinMaps} (mutated or not) so
     * callers can safely chain calls without losing previously defined joins.
     */
    default Map<String, Join<?, ?>> defineJoin(final Root<?> entity, final CriteriaQuery<?> query, final Metamodel metamodel, final Map<String, Join<?, ?>> joinMaps) {
        return joinMaps; // default: no joins, pass through
    }
}
