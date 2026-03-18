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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import quaero.components.filter.FilterArrayObject;
import quaero.components.filter.FilterSimpleObject;
import quaero.components.select.SelectSimpleObject;
import quaero.query.Query;
import quaero.query.QueryOrderObject;
import quaero.query.QuerySelectObject;

import quaero.security.QuaeroExposed;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link QuaeroSecurityValidator} — no Spring context required.
 *
 * <p>The validator's {@code configurers} field is injected via reflection so
 * we can exercise every branch without wiring a full Spring context.
 */
public class QuaeroSecurityValidatorTest {

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Build a ready-to-use validator from one or more configurers. */
    private QuaeroSecurityValidator validatorWith(final QuaeroConfigurer... configurers)
            throws Exception {
        final QuaeroSecurityValidator v = new QuaeroSecurityValidator();
        final Field f = QuaeroSecurityValidator.class.getDeclaredField("configurers");
        f.setAccessible(true);
        f.set(v, Arrays.asList(configurers));
        v.afterPropertiesSet();
        return v;
    }

    /** Build a validator with no configurers (inactive). */
    private QuaeroSecurityValidator inactiveValidator() throws Exception {
        final QuaeroSecurityValidator v = new QuaeroSecurityValidator();
        v.afterPropertiesSet(); // configurers = null → inactive
        return v;
    }

    /** Minimal query with only a tableName. */
    private Query queryFor(final String tableName) {
        final Query q = new Query();
        q.setTableName(tableName);
        return q;
    }

    /** Add a simple field select to a query. */
    private Query withSelect(final Query q, final String fieldPath) {
        final SelectSimpleObject simple = new SelectSimpleObject();
        simple.setField(fieldPath);
        final QuerySelectObject sel = new QuerySelectObject();
        sel.setField(simple);
        q.setSelects(Collections.singletonList(sel));
        return q;
    }

    /** Add a simple order to a query. */
    private Query withOrder(final Query q, final String fieldPath) {
        final SelectSimpleObject simple = new SelectSimpleObject();
        simple.setField(fieldPath);
        final QueryOrderObject order = new QueryOrderObject();
        order.setField(simple);
        q.setOrders(Collections.singletonList(order));
        return q;
    }

    // ─── No-configurer (inactive) ─────────────────────────────────────────────

    @Test
    void noConfigurers_nullList_validationSkipped() throws Exception {
        final QuaeroSecurityValidator v = inactiveValidator();
        // Any entity — should never throw
        assertDoesNotThrow(() -> v.validate(queryFor("AnythingGoes")));
    }

    @Test
    void noConfigurers_emptyList_validationSkipped() throws Exception {
        final QuaeroSecurityValidator v = new QuaeroSecurityValidator();
        final Field f = QuaeroSecurityValidator.class.getDeclaredField("configurers");
        f.setAccessible(true);
        f.set(v, Collections.emptyList());
        v.afterPropertiesSet();
        assertDoesNotThrow(() -> v.validate(queryFor("Sale")));
    }

    // ─── allowAll (global permissive) ────────────────────────────────────────

    @Test
    void allowAll_anyEntityPasses() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.allowAll());
        assertDoesNotThrow(() -> v.validate(queryFor("Sale")));
        assertDoesNotThrow(() -> v.validate(queryFor("UnknownEntity")));
    }

    @Test
    void allowAll_withExplicitDeny_throws() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.allowAll(),
                r -> r.entity("Forbidden").denyAll());
        // "Forbidden" explicitly denied even though global permissive
        assertThrows(QuaeroAccessDeniedException.class,
                () -> v.validate(queryFor("Forbidden")));
        // other entities still pass
        assertDoesNotThrow(() -> v.validate(queryFor("Sale")));
    }

    // ─── Entity-level access ─────────────────────────────────────────────────

    @Test
    void entityDenied_throws() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Secret").denyAll());
        assertThrows(QuaeroAccessDeniedException.class,
                () -> v.validate(queryFor("Secret")));
    }

    @Test
    void entityNotInPolicy_throws() throws Exception {
        // Only "Sale" is configured; "Invoice" is unknown → denied
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Sale").allowAll());
        assertThrows(QuaeroAccessDeniedException.class,
                () -> v.validate(queryFor("Invoice")));
    }

    @Test
    void entityAllowed_passes() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Sale").allowAll());
        assertDoesNotThrow(() -> v.validate(queryFor("Sale")));
    }

    // ─── Field-level access ───────────────────────────────────────────────────

    @Test
    void allFields_selectNotValidated() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Sale").allowAll());
        final Query q = withSelect(queryFor("Sale"), "anything");
        assertDoesNotThrow(() -> v.validate(q));
    }

    @Test
    void fieldAllowed_passes() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Sale").allow("id", "finalPrice"));
        final Query q = withSelect(queryFor("Sale"), "finalPrice");
        assertDoesNotThrow(() -> v.validate(q));
    }

    @Test
    void fieldForbidden_throws() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Sale").allow("id"));
        final Query q = withSelect(queryFor("Sale"), "secret");
        assertThrows(QuaeroAccessDeniedException.class, () -> v.validate(q));
    }

    @Test
    void nestedPath_validatesFirstSegmentOnly() throws Exception {
        // "vehicle.brand.name" → top-level segment is "vehicle"
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Sale").allow("vehicle"));
        final Query q = withSelect(queryFor("Sale"), "vehicle.brand.name");
        assertDoesNotThrow(() -> v.validate(q)); // "vehicle" is allowed
    }

    @Test
    void nestedPath_firstSegmentForbidden_throws() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Sale").allow("id"));
        final Query q = withSelect(queryFor("Sale"), "secret.nested.field");
        assertThrows(QuaeroAccessDeniedException.class, () -> v.validate(q));
    }

    // ─── Filter & order field validation ─────────────────────────────────────

    @Test
    void filterField_forbidden_throws() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Sale").allow("id"));
        final Query q = queryFor("Sale");
        q.setFilter(FilterSimpleObject.equal("secret", "x"));
        assertThrows(QuaeroAccessDeniedException.class, () -> v.validate(q));
    }

    @Test
    void filterArray_recursivelyValidated() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Sale").allow("id"));
        final Query q = queryFor("Sale");
        q.setFilter(FilterArrayObject.and(
                FilterSimpleObject.equal("id", 1),
                FilterSimpleObject.equal("secret", "x")));
        assertThrows(QuaeroAccessDeniedException.class, () -> v.validate(q));
    }

    @Test
    void orderField_forbidden_throws() throws Exception {
        final QuaeroSecurityValidator v = validatorWith(r -> r.entity("Sale").allow("id"));
        final Query q = withOrder(queryFor("Sale"), "secret");
        assertThrows(QuaeroAccessDeniedException.class, () -> v.validate(q));
    }

    // ─── Multiple configurers ─────────────────────────────────────────────────

    @Test
    void multipleConfigurers_denyWins() throws Exception {
        // Configurer A allows "Sale"; Configurer B denies it — deny wins
        final QuaeroSecurityValidator v = validatorWith(
                r -> r.entity("Sale").allowAll(),
                r -> r.entity("Sale").denyAll());
        assertThrows(QuaeroAccessDeniedException.class,
                () -> v.validate(queryFor("Sale")));
    }

    @Test
    void multipleConfigurers_fieldUnion() throws Exception {
        // Configurer A allows "id"; Configurer B allows "name" → union = both allowed
        final QuaeroSecurityValidator v = validatorWith(
                r -> r.entity("Sale").allow("id"),
                r -> r.entity("Sale").allow("name"));
        assertDoesNotThrow(() -> v.validate(withSelect(queryFor("Sale"), "id")));
        assertDoesNotThrow(() -> v.validate(withSelect(queryFor("Sale"), "name")));
        assertThrows(QuaeroAccessDeniedException.class,
                () -> v.validate(withSelect(queryFor("Sale"), "secret")));
    }

    // ─── @QuaeroExposed annotation support ───────────────────────────────────

    // Inner entity stubs used only for annotation scanning tests

    @QuaeroExposed
    private static class ExposedAllFields {
        // No field-level @QuaeroExposed → all fields are accessible
        @SuppressWarnings("unused") private String id;
        @SuppressWarnings("unused") private String name;
        @SuppressWarnings("unused") private String secret;
    }

    @QuaeroExposed
    private static class ExposedSelectFields {
        @QuaeroExposed private String id;
        @QuaeroExposed private String name;
        @SuppressWarnings("unused") private String secret; // NOT annotated
    }

    private static class NotExposed {
        @SuppressWarnings("unused") private String field;
    }

    /** Validator built only from annotation scanning (no configurers). */
    private QuaeroSecurityValidator validatorWithAnnotations(final Map<String, Class<?>> classMap)
            throws Exception {
        final QuaeroSecurityValidator v = new QuaeroSecurityValidator();
        v.afterPropertiesSet(); // no configurers → active stays false
        v.scanAnnotations(classMap);
        return v;
    }

    @Test
    void annotated_entityExposed_noFieldAnnotations_allFieldsPass() throws Exception {
        final Map<String, Class<?>> map = new LinkedHashMap<>();
        map.put("ExposedAllFields", ExposedAllFields.class);
        final QuaeroSecurityValidator v = validatorWithAnnotations(map);
        assertDoesNotThrow(() -> v.validate(withSelect(queryFor("ExposedAllFields"), "id")));
        assertDoesNotThrow(() -> v.validate(withSelect(queryFor("ExposedAllFields"), "anything")));
    }

    @Test
    void annotated_entityExposed_withFieldAnnotations_annotatedFieldsPass() throws Exception {
        final Map<String, Class<?>> map = new LinkedHashMap<>();
        map.put("ExposedSelectFields", ExposedSelectFields.class);
        final QuaeroSecurityValidator v = validatorWithAnnotations(map);
        assertDoesNotThrow(() -> v.validate(withSelect(queryFor("ExposedSelectFields"), "id")));
        assertDoesNotThrow(() -> v.validate(withSelect(queryFor("ExposedSelectFields"), "name")));
    }

    @Test
    void annotated_entityExposed_withFieldAnnotations_unannotatedFieldThrows() throws Exception {
        final Map<String, Class<?>> map = new LinkedHashMap<>();
        map.put("ExposedSelectFields", ExposedSelectFields.class);
        final QuaeroSecurityValidator v = validatorWithAnnotations(map);
        assertThrows(QuaeroAccessDeniedException.class,
                () -> v.validate(withSelect(queryFor("ExposedSelectFields"), "secret")));
    }

    @Test
    void annotated_entityWithoutAnnotation_whenScanningActive_throws() throws Exception {
        // ExposedAllFields activates scanning; NotExposed has no @QuaeroExposed → denied
        final Map<String, Class<?>> map = new LinkedHashMap<>();
        map.put("ExposedAllFields", ExposedAllFields.class);
        map.put("NotExposed", NotExposed.class);
        final QuaeroSecurityValidator v = validatorWithAnnotations(map);
        assertThrows(QuaeroAccessDeniedException.class,
                () -> v.validate(queryFor("NotExposed")));
    }

    @Test
    void annotated_noAnnotatedEntities_validationStaysInactive() throws Exception {
        // No @QuaeroExposed anywhere → active stays false → pass-through
        final Map<String, Class<?>> map = new LinkedHashMap<>();
        map.put("NotExposed", NotExposed.class);
        final QuaeroSecurityValidator v = validatorWithAnnotations(map);
        assertDoesNotThrow(() -> v.validate(queryFor("NotExposed")));
        assertDoesNotThrow(() -> v.validate(queryFor("AnythingElse")));
    }

    @Test
    void annotationAndConfigurer_configurerRestrictsAnnotatedFields() throws Exception {
        // @QuaeroExposed at class level → allFields = true
        // Configurer restricts to only "id" → most restrictive wins
        final Map<String, Class<?>> map = new LinkedHashMap<>();
        map.put("ExposedAllFields", ExposedAllFields.class);

        final QuaeroSecurityValidator v = new QuaeroSecurityValidator();
        final Field f = QuaeroSecurityValidator.class.getDeclaredField("configurers");
        f.setAccessible(true);
        f.set(v, Arrays.asList((QuaeroConfigurer) r -> r.entity("ExposedAllFields").allow("id")));
        v.afterPropertiesSet();       // processes configurer
        v.scanAnnotations(map);       // merges annotation policy

        assertDoesNotThrow(() -> v.validate(withSelect(queryFor("ExposedAllFields"), "id")));
        assertThrows(QuaeroAccessDeniedException.class,
                () -> v.validate(withSelect(queryFor("ExposedAllFields"), "name")));
    }
}
