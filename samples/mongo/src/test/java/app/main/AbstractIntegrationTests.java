/*
 * Copyright 2020-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.main;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MongoDBContainer;

import app.main.AbstractIntegrationTests.Initializer;

/**
 * @author Dave Syer
 *
 */
@ContextConfiguration(initializers = Initializer.class)
public abstract class AbstractIntegrationTests {

	static final MongoDBContainer mongoDBContainer = new MongoDBContainer();

	protected static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			mongoDBContainer.start();
			TestPropertyValues
					.of("spring.data.mongodb.host=" + mongoDBContainer.getHost(),
							"spring.data.mongodb.port=" + mongoDBContainer.getFirstMappedPort())
					.applyTo(configurableApplicationContext.getEnvironment());
		}
	}

}