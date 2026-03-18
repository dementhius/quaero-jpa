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
package quaero;

import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import quaero.components.filter.IFilter;
import quaero.utils.CoercionMode;

/**
 * Translates a Quaero {@link IFilter} tree into a JPA {@link Predicate}.
 *
 * <p>Inject this bean when you need dynamic WHERE clauses inside your own JPA
 * criteria queries, without using the full {@link QueryExecutor} pipeline.
 *
 * <pre>{@code
 * @Autowired QuaeroPredicateBuilder predicateBuilder;
 *
 * CriteriaBuilder cb = em.getCriteriaBuilder();
 * CriteriaQuery<Sale> q = cb.createQuery(Sale.class);
 * Root<Sale> root = q.from(Sale.class);
 *
 * IFilter filter = FilterArrayObject.and(
 *     FilterSimpleObject.equal("status", "ACTIVE"),
 *     FilterSimpleObject.greaterThan("finalPrice", 10_000)
 * );
 *
 * Predicate where = predicateBuilder.build(filter, root, q, cb);
 * q.where(where);
 * }</pre>
 */
@Component
public class QuaeroPredicateBuilder {

    @Autowired
    private Map<String, EntityType<?>> entities;
    @Autowired
    private Map<String, ManagedType<?>> managedTypes;

    /**
     * Build a JPA {@link Predicate} from the given filter using
     * {@link CoercionMode#STRICT}.
     *
     * @return the resolved predicate, or {@code null} if {@code filter} is null.
     */
    public Predicate build(final IFilter filter, final From<?, ?> from,
            final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        return build(CoercionMode.STRICT, filter, from, query, cb);
    }

    /**
     * Build a JPA {@link Predicate} from the given filter using the specified
     * {@link CoercionMode}.
     *
     * @return the resolved predicate, or {@code null} if {@code filter} is null.
     */
    public Predicate build(final CoercionMode mode, final IFilter filter,
            final From<?, ?> from, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        if (filter == null) {
            return null;
        }
        return filter.resolve(mode, from, query, cb, entities, managedTypes);
    }
}
