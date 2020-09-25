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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.init.func.InfrastructureProvider;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class LibraryInfrastructureProvider implements InfrastructureProvider {

	private static boolean serverPropertiesEnabled = ClassUtils.isPresent("org.springframework.boot.autoconfigure.web.ServerProperties",
			null)
			&& ClassUtils.isPresent("org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext",
					null);

	private ServerProperties serverProperties(ServerProperties bean, Environment environment) {
		bean.getServlet().setRegisterDefaultServlet(false);
		return bean;
	}

	@Override
	public Collection<? extends ApplicationContextInitializer<GenericApplicationContext>> getInitializers(
			GenericApplicationContext main) {
		if (serverPropertiesEnabled && main instanceof ServletWebServerApplicationContext) {
			return Arrays.asList(InfrastructureUtils.binder(ServerProperties.class, this::serverProperties));
		}
		return Collections.emptyList();
	}

}
