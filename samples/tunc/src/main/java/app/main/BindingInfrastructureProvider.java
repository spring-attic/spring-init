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
package app.main;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.init.func.InfrastructureProvider;
import org.springframework.init.func.InfrastructureUtils;

/**
 * @author Dave Syer
 *
 */
public class BindingInfrastructureProvider implements InfrastructureProvider {

	@Override
	public Collection<? extends ApplicationContextInitializer<GenericApplicationContext>> getInitializers(
			GenericApplicationContext main) {
		return Arrays.asList(InfrastructureUtils.binder(Foo.class, BindingInfrastructureProvider::bindFoo),
				InfrastructureUtils.binder(ServerProperties.class, BindingInfrastructureProvider::bindServer));
	}

	private static ServerProperties bindServer(ServerProperties bean, Environment environment) {
		bean.getServlet().setRegisterDefaultServlet(false);
		bean.setPort(environment.getProperty("server.port", Integer.class,
				environment.getProperty("SERVER_PORT", Integer.class, 8080)));
		return bean;
	}

	private static Foo bindFoo(Foo bean, Environment environment) {
		bean.setValue(environment.getProperty("app.value", environment.getProperty("APP_VALUE", "Hi")));
		return bean;
	}

}
