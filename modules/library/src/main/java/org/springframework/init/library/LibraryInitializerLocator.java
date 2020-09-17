/*
 * Copyright 2019-2019 the original author or authors.
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
package org.springframework.init.library;

import org.springframework.boot.actuate.autoconfigure.web.reactive.FunctionalReactiveActuatorEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.reactive.FunctionalReactiveActuatorEndpointAutoConfigurationInitializer;
import org.springframework.boot.actuate.autoconfigure.web.servlet.FunctionalServletActuatorEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.FunctionalServletActuatorEndpointAutoConfigurationInitializer;
import org.springframework.init.func.SimpleInitializerLocator;

/**
 * @author Dave Syer
 *
 */
public class LibraryInitializerLocator extends SimpleInitializerLocator {

	public LibraryInitializerLocator() {
		super();
		register(FunctionalReactiveActuatorEndpointAutoConfiguration.class.getName(),
				() -> new FunctionalReactiveActuatorEndpointAutoConfigurationInitializer());
		register(FunctionalServletActuatorEndpointAutoConfiguration.class.getName(),
				() -> new FunctionalServletActuatorEndpointAutoConfigurationInitializer());
	}

}
