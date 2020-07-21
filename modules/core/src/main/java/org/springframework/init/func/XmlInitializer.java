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
package org.springframework.init.func;

import java.io.IOException;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

public class XmlInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

	private final Resource[] resources;

	private final String pattern;

	public XmlInitializer(Resource[] resources) {
		this.resources = resources;
		this.pattern = null;
	}

	public XmlInitializer(String pattern) {
		this.resources = null;
		this.pattern = pattern;
	}

	@Override
	public void initialize(GenericApplicationContext context) {
		XmlBeanDefinitionReader xml = new XmlBeanDefinitionReader(context);
		if (resources != null) {
			for (Resource resource : resources) {
				xml.loadBeanDefinitions(resource);
			}
		} else {
			Resource[] resources;
			try {
				resources = context.getResources(pattern);
				for (Resource resource : resources) {
					xml.loadBeanDefinitions(resource);
				}
			} catch (IOException e) {
				throw new IllegalStateException("Cannot load resources: " + pattern, e);
			}
		}
	}

}