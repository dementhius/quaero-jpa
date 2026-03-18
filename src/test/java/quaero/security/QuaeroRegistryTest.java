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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QuaeroRegistry} — no Spring context required.
 */
public class QuaeroRegistryTest {

    @Test
    void allowAll_setsGlobalPermissive() {
        final QuaeroRegistry registry = new QuaeroRegistry();
        registry.allowAll();
        assertTrue(registry.isGlobalPermissive());
    }

    @Test
    void newRegistry_notGlobalPermissive() {
        assertFalse(new QuaeroRegistry().isGlobalPermissive());
    }

    @Test
    void entity_allowSpecificFields_storesAllowedFields() {
        final QuaeroRegistry registry = new QuaeroRegistry();
        registry.entity("Sale").allow("id", "finalPrice");

        final QuaeroRegistry.EntityConfig cfg = registry.getEntityConfigs().get("Sale");
        assertNotNull(cfg);
        assertFalse(cfg.denied);
        assertFalse(cfg.allFields);
        assertTrue(cfg.allowedFields.contains("id"));
        assertTrue(cfg.allowedFields.contains("finalPrice"));
        assertEquals(2, cfg.allowedFields.size());
    }

    @Test
    void entity_allowAll_setsAllFieldsFlag() {
        final QuaeroRegistry registry = new QuaeroRegistry();
        registry.entity("Sale").allowAll();

        final QuaeroRegistry.EntityConfig cfg = registry.getEntityConfigs().get("Sale");
        assertTrue(cfg.allFields);
        assertFalse(cfg.denied);
    }

    @Test
    void entity_denyAll_setsDeniedFlag() {
        final QuaeroRegistry registry = new QuaeroRegistry();
        registry.entity("User").denyAll();

        final QuaeroRegistry.EntityConfig cfg = registry.getEntityConfigs().get("User");
        assertTrue(cfg.denied);
    }

    @Test
    void allow_noArgs_equalsAllFields() {
        final QuaeroRegistry registry = new QuaeroRegistry();
        registry.entity("Sale").allow(); // empty varargs ≡ allowAll

        assertTrue(registry.getEntityConfigs().get("Sale").allFields);
    }

    @Test
    void chaining_and_multipleEntities() {
        final QuaeroRegistry registry = new QuaeroRegistry();
        registry.entity("Sale").allow("id").and()
                .entity("User").denyAll();

        assertEquals(2, registry.getEntityConfigs().size());
        assertFalse(registry.getEntityConfigs().get("Sale").denied);
        assertTrue(registry.getEntityConfigs().get("User").denied);
    }

    @Test
    void getEntityConfigs_isUnmodifiable() {
        final QuaeroRegistry registry = new QuaeroRegistry();
        assertThrows(UnsupportedOperationException.class,
                () -> registry.getEntityConfigs().put("X", new QuaeroRegistry.EntityConfig()));
    }

    @Test
    void entity_calledTwice_sameConfig_accumulates() {
        final QuaeroRegistry registry = new QuaeroRegistry();
        registry.entity("Sale").allow("id");
        registry.entity("Sale").allow("finalPrice"); // second call on same entity

        final QuaeroRegistry.EntityConfig cfg = registry.getEntityConfigs().get("Sale");
        assertTrue(cfg.allowedFields.contains("id"));
        assertTrue(cfg.allowedFields.contains("finalPrice"));
        assertEquals(1, registry.getEntityConfigs().size()); // still one entry
    }
}
