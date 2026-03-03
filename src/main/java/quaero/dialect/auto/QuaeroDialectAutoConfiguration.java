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
package quaero.dialect.auto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

import javax.persistence.EntityManagerFactory;

@Configuration
@ConditionalOnClass(EntityManagerFactory.class)
@Conditional(QuaeroDialectAutoConfiguration.NoExplicitDialect.class)
public class QuaeroDialectAutoConfiguration {

	@Bean
	public HibernatePropertiesCustomizer quaeroDialectResolverCustomizer() {
		return hibernateProperties -> {
			final String resolverClass = QuaeroDialectResolver.class.getName();
			final String existing = (String) hibernateProperties.get("hibernate.dialect_resolvers");

			final String merged = (existing != null && !existing.trim().isEmpty()) ? existing + "," + resolverClass
					: resolverClass;

			hibernateProperties.put("hibernate.dialect_resolvers", merged);
		};
	}

	static class NoExplicitDialect implements Condition {
		@Override
		public boolean matches(final ConditionContext ctx, final AnnotatedTypeMetadata metadata) {
			final String dialect = ctx.getEnvironment().getProperty("spring.jpa.properties.hibernate.dialect");
			return dialect == null || dialect.trim().isEmpty();
		}
	}
}
