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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fluent registry used inside {@link QuaeroConfigurer#configure(QuaeroRegistry)}
 * to declare which JPA entities and fields are accessible through Quaero.
 *
 * <p>Each call to {@link #entity(String)} returns an {@link EntitySpec} that
 * lets you allow specific fields or deny the entity entirely. Chain multiple
 * entity declarations with {@link EntitySpec#and()}.
 *
 * <pre>{@code
 * registry
 *     .entity("Sale")
 *         .allow("id", "finalPrice", "status")
 *     .and()
 *     .entity("User")
 *         .denyAll();
 * }</pre>
 */
public class QuaeroRegistry {

    private boolean globalPermissive = false;
    private final Map<String, EntityConfig> entityConfigs = new LinkedHashMap<>();

    // ─── Global ────────────────────────────────────────────────────────────────

    /**
     * Allow all entities and all fields with no restrictions.
     * Useful for internal tools and demo environments.
     *
     * <p>Note: an explicit {@code denyAll()} from another configurer always wins.
     */
    public QuaeroRegistry allowAll() {
        this.globalPermissive = true;
        return this;
    }

    // ─── Entity-level entry point ──────────────────────────────────────────────

    /**
     * Begin configuring access for the JPA entity with the given simple name
     * (e.g. {@code "Sale"}).
     */
    public EntitySpec entity(final String name) {
        return new EntitySpec(name);
    }

    // ─── Package-visible accessors (used by QuaeroSecurityValidator) ──────────

    boolean isGlobalPermissive() {
        return globalPermissive;
    }

    Map<String, EntityConfig> getEntityConfigs() {
        return Collections.unmodifiableMap(entityConfigs);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EntitySpec — fluent per-entity configuration
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Fluent handle returned by {@link QuaeroRegistry#entity(String)} to configure
     * access for a specific entity.
     */
    public class EntitySpec {

        private final String name;

        EntitySpec(final String name) {
            this.name = name;
            entityConfigs.putIfAbsent(name, new EntityConfig());
        }

        /**
         * Allow access to the specified top-level fields of this entity.
         * Nested paths (e.g. {@code "vehicle.brand.name"}) are controlled by their
         * first segment ({@code "vehicle"}).
         *
         * <p>Calling {@code allow()} with no arguments is equivalent to
         * {@link #allowAll()}.
         */
        public EntitySpec allow(final String... fields) {
            final EntityConfig config = entityConfigs.get(name);
            if (fields == null || fields.length == 0) {
                config.allFields = true;
            } else {
                config.allowedFields.addAll(Arrays.asList(fields));
            }
            return this;
        }

        /**
         * Allow access to this entity with no field restrictions.
         */
        public EntitySpec allowAll() {
            entityConfigs.get(name).allFields = true;
            return this;
        }

        /**
         * Deny all access to this entity. Takes precedence over any other
         * configurer that allows it.
         */
        public EntitySpec denyAll() {
            entityConfigs.get(name).denied = true;
            return this;
        }

        /** Return to the parent registry to configure another entity. */
        public QuaeroRegistry and() {
            return QuaeroRegistry.this;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EntityConfig — internal data holder
    // ═══════════════════════════════════════════════════════════════════════════

    static class EntityConfig {
        boolean denied = false;
        /** True when the entity is explicitly allowed with no field restrictions. */
        boolean allFields = false;
        Set<String> allowedFields = new LinkedHashSet<>();
    }
}
