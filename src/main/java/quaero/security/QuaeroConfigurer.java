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

/**
 * Implement this interface and declare it as a Spring bean to configure which
 * JPA entities and fields are accessible through Quaero.
 *
 * <p>Any number of {@code QuaeroConfigurer} beans can coexist — all are applied
 * before the first query runs. The merge rules are:
 * <ul>
 *   <li><b>Entity access:</b> a single {@code denyAll()} from any configurer
 *       permanently blocks the entity.</li>
 *   <li><b>Field access:</b> the union of allowed-field lists across all
 *       configurers is used; if any configurer calls {@code allowAll()} for an
 *       entity, it contributes no field restriction.</li>
 * </ul>
 *
 * <p>If <em>no</em> {@code QuaeroConfigurer} bean is present in the context,
 * security validation is skipped entirely (backwards-compatible behaviour).
 *
 * <h3>Example — centralised configuration</h3>
 * <pre>{@code
 * @Configuration
 * public class MyQuaeroConfig implements QuaeroConfigurer {
 *
 *     @Override
 *     public void configure(QuaeroRegistry registry) {
 *         registry
 *             .entity("Sale")
 *                 .allow("id", "finalPrice", "status")
 *             .and()
 *             .entity("User")
 *                 .denyAll();
 *     }
 * }
 * }</pre>
 *
 * <h3>Example — permissive mode (internal / demo environments)</h3>
 * <pre>{@code
 * @Configuration
 * public class MyQuaeroConfig implements QuaeroConfigurer {
 *
 *     @Override
 *     public void configure(QuaeroRegistry registry) {
 *         registry.allowAll();
 *     }
 * }
 * }</pre>
 */
public interface QuaeroConfigurer {

    /**
     * Called once at startup. Use the {@code registry} to declare which entities
     * and fields are accessible.
     */
    void configure(QuaeroRegistry registry);
}
