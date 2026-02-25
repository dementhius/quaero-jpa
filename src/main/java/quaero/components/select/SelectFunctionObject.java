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
 * Calls a native SQL function via cb.function().
 *
 * <p>The {@code returnType} field allows the caller to declare the expected
 * Java return type of the function. Defaults to {@code Object.class} for
 * unknown functions. Well-known string functions (to_char, substring) default
 * to {@code String.class} when no returnType is provided.
 */
public class SelectFunctionObject implements ISelect {

    private List<ISelect> params;
    private String function;
    /**
     * Optional: fully-qualified class name of the expected return type.
     * e.g. "java.lang.String", "java.lang.Integer".
     * Defaults to Object.class if null or unresolvable.
     */
    private String returnType;

    public List<ISelect> getParams() {
        return params;
    }

    public void setParams(final List<ISelect> params) {
        this.params = params;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(final String function) {
        this.function = function;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(final String returnType) {
        this.returnType = returnType;
    }

    @Override
    public Expression<?> resolve(final CoercionMode mode,final From<?, ?> entity, final CriteriaQuery<?> query, final CriteriaBuilder cb, final Map<String, EntityType<?>> entities,
            final Map<String, ManagedType<?>> managedTypes) {
        if (params == null || params.isEmpty() || function == null || function.isEmpty()) {
            return null;
        }

        final Expression<?>[] args = params.stream().map(p -> p.resolve(mode, entity, query, cb, entities, managedTypes)).toArray(Expression[]::new);

        return cb.function(function, resolveReturnType(), args);
    }

    /**
     * Resolves the Java return type for the function.
     * Priority: explicit returnType field > well-known function defaults > Object.class
     */
    private Class<?> resolveReturnType() {
        if (returnType != null && !returnType.isEmpty()) {
            try {
                return Class.forName(returnType);
            } catch (ClassNotFoundException ignored) {
                // fall through to defaults
            }
        }
        // Well-known string functions
        if (function != null) {
            switch (function.toLowerCase()) {
                case "to_char":
                case "substring":
                case "upper":
                case "lower":
                case "trim":
                case "replace":
                    return String.class;
            }
        }
        return Object.class;
    }

    @Override
    public Map<String, Join<?, ?>> defineJoin(final Root<?> entity, final CriteriaQuery<?> query, final Metamodel metamodel,
            final Map<String, Join<?, ?>> joinMaps) {
        if (params != null) {
            for (final ISelect param : params) {
                param.defineJoin(entity, query, metamodel, joinMaps);
            }
        }
        return joinMaps;
    }

}
