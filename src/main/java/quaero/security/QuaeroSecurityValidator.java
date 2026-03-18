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
package quaero.security;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.EntityType;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import quaero.components.filter.FilterArrayObject;
import quaero.components.filter.FilterSimpleObject;
import quaero.components.filter.IFilter;
import quaero.components.select.ISelect;
import quaero.components.select.SelectSimpleObject;
import quaero.query.Query;
import quaero.query.QueryJoinObject;
import quaero.query.QueryMultiJoinObject;
import quaero.query.QueryOrderObject;
import quaero.query.QuerySelectObject;

/**
 * Validates every {@link Query} against the rules declared in all
 * {@link QuaeroConfigurer} beans before execution.
 *
 * <p><b>Activation:</b> validation is only active when at least one
 * {@link QuaeroConfigurer} bean is present in the Spring context. If none is
 * found, all queries pass through unchanged (backwards-compatible behaviour).
 *
 * <p><b>What is validated:</b>
 * <ul>
 *   <li>Root entity ({@code tableName}).</li>
 *   <li>All joined entities ({@code dynamicJoins}, {@code dynamicJoinsMultiple}).</li>
 *   <li>Top-level fields referenced in selects, filters and orders of the root
 *       entity. For a path like {@code "vehicle.brand.name"}, the validated
 *       segment is {@code "vehicle"}.</li>
 * </ul>
 *
 * <p><b>Field validation is not applied to join-level selects</b> in this
 * version; entity-level access control covers those cases.
 */
@Component
public class QuaeroSecurityValidator implements InitializingBean {

    @Autowired(required = false)
    private List<QuaeroConfigurer> configurers;

    @Autowired(required = false)
    private Map<String, EntityType<?>> entities;

    /** False when neither configurers nor {@link QuaeroExposed} annotations are present. */
    private boolean active = false;

    /**
     * True when any configurer called {@code registry.allowAll()}.
     * Still subject to explicit {@code denyAll()} on individual entities.
     */
    private boolean globalPermissive = false;

    private final Map<String, MergedEntityPolicy> policies = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        if (configurers != null && !configurers.isEmpty()) {
            active = true;
            for (final QuaeroConfigurer configurer : configurers) {
                final QuaeroRegistry registry = new QuaeroRegistry();
                configurer.configure(registry);
                merge(registry);
            }
        }

        if (entities != null && !entities.isEmpty()) {
            final Map<String, Class<?>> classMap = new LinkedHashMap<>();
            for (final Map.Entry<String, EntityType<?>> e : entities.entrySet()) {
                if (e.getValue().getJavaType() != null) {
                    classMap.put(e.getKey(), e.getValue().getJavaType());
                }
            }
            scanAnnotations(classMap);
        }
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    /**
     * Validate the given query. Throws {@link QuaeroAccessDeniedException} if any
     * entity or field is not permitted by the active configuration.
     *
     * <p>A no-op when no {@link QuaeroConfigurer} beans are registered.
     */
    public void validate(final Query query) {
        if (!active) {
            return;
        }

        // Root entity
        checkEntity(query.getTableName());

        // Joined entities
        if (query.getDynamicJoins() != null) {
            for (final QueryJoinObject join : query.getDynamicJoins()) {
                checkEntity(join.getJoinTableName());
            }
        }
        if (query.getDynamicJoinsMultiple() != null) {
            for (final QueryMultiJoinObject join : query.getDynamicJoinsMultiple()) {
                checkEntity(join.getJoinTableName());
            }
        }

        // Fields in selects
        if (query.getSelects() != null) {
            for (final QuerySelectObject select : query.getSelects()) {
                checkField(query.getTableName(), select.getField());
            }
        }

        // Fields in filter (recursive)
        checkFilterFields(query.getTableName(), query.getFilter());

        // Fields in orders
        if (query.getOrders() != null) {
            for (final QueryOrderObject order : query.getOrders()) {
                checkField(query.getTableName(), order.getField());
            }
        }
    }

    // ─── Validation helpers ────────────────────────────────────────────────────

    private void checkEntity(final String entityName) {
        if (entityName == null) {
            return;
        }
        if (globalPermissive && !isExplicitlyDenied(entityName)) {
            return;
        }
        final MergedEntityPolicy policy = policies.get(entityName);
        if (policy == null || policy.denied) {
            throw new QuaeroAccessDeniedException("Access denied to entity: '" + entityName + "'");
        }
    }

    private void checkField(final String entityName, final ISelect select) {
        if (select == null || entityName == null) {
            return;
        }
        if (!(select instanceof SelectSimpleObject)) {
            // Complex expressions (functions, arithmetic, etc.) are not field-validated.
            return;
        }
        final String path = ((SelectSimpleObject) select).getField();
        if (path == null || path.isEmpty()) {
            return;
        }

        // Only the first path segment is the direct attribute of the root entity.
        final int dot = path.indexOf('.');
        final String topLevel = dot < 0 ? path : path.substring(0, dot);

        if (globalPermissive && !isExplicitlyDenied(entityName)) {
            return;
        }

        final MergedEntityPolicy policy = policies.get(entityName);
        if (policy == null || policy.denied) {
            // Entity-level already blocked by checkEntity(); nothing more to do here.
            return;
        }
        if (policy.allFields) {
            return;
        }
        if (!policy.allowedFields.isEmpty() && !policy.allowedFields.contains(topLevel)) {
            throw new QuaeroAccessDeniedException(
                    "Access denied to field '" + topLevel + "' on entity '" + entityName + "'");
        }
    }

    private void checkFilterFields(final String entityName, final IFilter filter) {
        if (filter == null) {
            return;
        }
        if (filter instanceof FilterSimpleObject) {
            checkField(entityName, ((FilterSimpleObject) filter).getField());
        } else if (filter instanceof FilterArrayObject) {
            final IFilter[] children = ((FilterArrayObject) filter).getFilters();
            if (children != null) {
                for (final IFilter child : children) {
                    checkFilterFields(entityName, child);
                }
            }
        }
        // FilterQueryObject: skipped — subquery validation is out of scope for now.
    }

    private boolean isExplicitlyDenied(final String entityName) {
        final MergedEntityPolicy policy = policies.get(entityName);
        return policy != null && policy.denied;
    }

    // ─── Annotation scanning ───────────────────────────────────────────────────

    /**
     * Scans {@code entityClasses} for {@link QuaeroExposed} and merges the
     * resulting policies into the active rule set.
     *
     * <p>Package-visible so unit tests can call this directly without wiring a
     * full JPA context.
     */
    void scanAnnotations(final Map<String, Class<?>> entityClasses) {
        for (final Map.Entry<String, Class<?>> entry : entityClasses.entrySet()) {
            final Class<?> javaType = entry.getValue();
            if (!javaType.isAnnotationPresent(QuaeroExposed.class)) {
                continue;
            }
            active = true;
            final QuaeroRegistry.EntityConfig config = new QuaeroRegistry.EntityConfig();
            final Set<String> exposedFields = collectExposedFields(javaType);
            if (exposedFields.isEmpty()) {
                config.allFields = true;
            } else {
                config.allowedFields.addAll(exposedFields);
            }
            final MergedEntityPolicy existing = policies.get(entry.getKey());
            if (existing == null) {
                policies.put(entry.getKey(), new MergedEntityPolicy(config));
            } else {
                existing.merge(config);
            }
        }
    }

    private Set<String> collectExposedFields(final Class<?> clazz) {
        final Set<String> result = new LinkedHashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (final java.lang.reflect.Field f : current.getDeclaredFields()) {
                if (f.isAnnotationPresent(QuaeroExposed.class)) {
                    result.add(f.getName());
                }
            }
            current = current.getSuperclass();
        }
        return result;
    }

    // ─── Registry merge ────────────────────────────────────────────────────────

    private void merge(final QuaeroRegistry registry) {
        if (registry.isGlobalPermissive()) {
            globalPermissive = true;
        }
        for (final Map.Entry<String, QuaeroRegistry.EntityConfig> entry : registry.getEntityConfigs().entrySet()) {
            final String entity = entry.getKey();
            final QuaeroRegistry.EntityConfig config = entry.getValue();
            final MergedEntityPolicy existing = policies.get(entity);
            if (existing == null) {
                policies.put(entity, new MergedEntityPolicy(config));
            } else {
                existing.merge(config);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Internal merged policy
    // ═══════════════════════════════════════════════════════════════════════════

    private static class MergedEntityPolicy {

        boolean denied;
        /**
         * True only when ALL configurers that mention this entity have set
         * {@code allFields=true}. A single configurer with an explicit field list
         * narrows the access.
         */
        boolean allFields;
        Set<String> allowedFields;

        MergedEntityPolicy(final QuaeroRegistry.EntityConfig config) {
            this.denied = config.denied;
            this.allFields = config.allFields;
            this.allowedFields = new LinkedHashSet<>(config.allowedFields);
        }

        void merge(final QuaeroRegistry.EntityConfig config) {
            // Any deny wins
            this.denied |= config.denied;
            // All must agree that there are no field restrictions
            this.allFields &= config.allFields;
            // Union of explicit allow-lists
            this.allowedFields.addAll(config.allowedFields);
        }
    }
}
