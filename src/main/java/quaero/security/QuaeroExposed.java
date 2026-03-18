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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JPA entity class or field as accessible through Quaero queries.
 *
 * <p>Place on the <b>entity class</b> to make it queryable. Optionally, also
 * annotate individual <b>fields</b> to restrict which fields may appear in
 * selects, filters, and orders:
 *
 * <ul>
 *   <li>Entity annotated, no fields annotated → all fields are accessible.</li>
 *   <li>Entity annotated, some fields annotated → only annotated fields.</li>
 *   <li>Entity not annotated → access denied (when annotation scanning is
 *       active, i.e., at least one entity in the application carries this
 *       annotation).</li>
 * </ul>
 *
 * <p>This approach is fully compatible with {@link QuaeroConfigurer}: both
 * mechanisms can coexist. Policies from annotations and configurers are merged
 * at startup using the <em>most-restrictive-wins</em> rule — a {@code denyAll}
 * from any configurer always blocks the entity; explicit field allow-lists are
 * intersected conservatively.
 *
 * <p>Annotation scanning activates automatically as soon as at least one entity
 * in the metamodel carries {@code @QuaeroExposed}. No {@link QuaeroConfigurer}
 * bean is required when using this approach exclusively.
 *
 * <pre>{@code
 * // All fields accessible
 * @Entity
 * @QuaeroExposed
 * public class Product { ... }
 *
 * // Only 'id', 'name', and 'price' accessible
 * @Entity
 * @QuaeroExposed
 * public class Sale {
 *     @QuaeroExposed private Long id;
 *     @QuaeroExposed private String name;
 *     @QuaeroExposed private BigDecimal price;
 *     private String internalNotes; // blocked
 * }
 * }</pre>
 *
 * @see QuaeroConfigurer
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QuaeroExposed {
}
