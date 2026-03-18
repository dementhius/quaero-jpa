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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Order;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import quaero.query.QueryOrderObject;
import quaero.utils.CoercionMode;

/**
 * Translates a list of {@link QueryOrderObject} into JPA {@link Order} expressions.
 *
 * <p>Inject this bean when you need dynamic ORDER BY clauses inside your own JPA
 * criteria queries, without using the full {@link QueryExecutor} pipeline.
 *
 * <pre>{@code
 * @Autowired QuaeroOrderBuilder orderBuilder;
 *
 * List<Order> orders = orderBuilder.build(queryOrders, root, q, cb);
 * q.orderBy(orders);
 * }</pre>
 *
 * <p>When the query has multiple roots (e.g. Cartesian joins), use the
 * multi-root overload: the builder tries each root in turn until the order
 * field is resolved.
 */
@Component
public class QuaeroOrderBuilder {

    private final Logger logger = Logger.getLogger(QuaeroOrderBuilder.class.getName());

    @Autowired
    private Map<String, EntityType<?>> entities;
    @Autowired
    private Map<String, ManagedType<?>> managedTypes;

    /**
     * Build ORDER BY expressions for the given root using
     * {@link CoercionMode#STRICT}.
     */
    public List<Order> build(final List<QueryOrderObject> orders, final From<?, ?> root,
            final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        return build(CoercionMode.STRICT, orders, Collections.singletonList(root), query, cb);
    }

    /**
     * Build ORDER BY expressions for the given root using the specified
     * {@link CoercionMode}.
     */
    public List<Order> build(final CoercionMode mode, final List<QueryOrderObject> orders,
            final From<?, ?> root, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        return build(mode, orders, Collections.singletonList(root), query, cb);
    }

    /**
     * Build ORDER BY expressions searching for each field across multiple roots.
     * Roots are tried in list order; the first successful resolution wins.
     *
     * <p>This overload is intended for queries with Cartesian joins where order
     * fields may belong to different root entities.
     */
    public List<Order> build(final CoercionMode mode, final List<QueryOrderObject> orders,
            final List<From<?, ?>> roots, final CriteriaQuery<?> query, final CriteriaBuilder cb) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Order> result = new ArrayList<>();

        for (final QueryOrderObject orderObj : orders) {
            Order resolved = null;

            for (final From<?, ?> candidate : roots) {
                try {
                    resolved = orderObj.resolve(mode, candidate, query, cb, entities, managedTypes);
                    if (resolved != null) {
                        break;
                    }
                } catch (Exception ex) {
                    logger.log(Level.FINE,
                            "Order field not found in root " + candidate.getJavaType().getSimpleName() + ", trying next", ex);
                }
            }

            if (resolved != null) {
                result.add(resolved);
            } else {
                logger.warning("Could not resolve order field in any root: " + orderObj.getField());
            }
        }

        return result;
    }
}
