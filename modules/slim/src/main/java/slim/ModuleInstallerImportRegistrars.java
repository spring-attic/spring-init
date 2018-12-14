/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package slim;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class ModuleInstallerImportRegistrars
		implements BeanDefinitionRegistryPostProcessor, ImportRegistrars {

	private Set<Imported> registrars = new LinkedHashSet<>();

	private GenericApplicationContext context;

	public ModuleInstallerImportRegistrars(GenericApplicationContext context) {
		this.context = context;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
	}

	@Override
	public void add(Class<?> importer, Class<?> registrar) {
		this.registrars.add(new Imported(importer, registrar));
	}

	@Override
	public void add(Class<?> importer, String typeName) {
		if (typeName.endsWith(".xml")) {
			this.registrars.add(new Imported(importer, typeName, context));
		}
		else {
			if (isAutoConfiguration(importer, typeName) && !context.getEnvironment()
					.getProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY,
							Boolean.class, true)) {
				return;
			}
			this.registrars
					.add(new Imported(importer, typeName, context.getClassLoader()));
		}
	}

	private boolean isAutoConfiguration(Class<?> importer, String typeName) {
		// TODO: maybe work out a better way to detect auto configs
		return typeName.endsWith("AutoConfigurationImportSelector")
				|| importer.getName().endsWith("AutoConfiguration")
				|| typeName.endsWith("AutoConfiguration");
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
			throws BeansException {
		Set<Imported> seen = new LinkedHashSet<>();
		Set<Imported> added = findAdded(seen, registry);
		while (!added.isEmpty()) {
			for (Imported imported : added) {
				if (!registrars.contains(imported)) {
					Class<?> type = imported.getType();
					if (type != null && ImportBeanDefinitionRegistrar.class
							.isAssignableFrom(type)) {
						importRegistrar(registry, imported);
					}
				}
			}
			added = findAdded(seen, registry);
		}
	}

	private Set<Imported> findAdded(Set<Imported> seen, BeanDefinitionRegistry registry) {
		Set<Imported> added = new LinkedHashSet<>();
		Set<Imported> start = ordered(registrars);
		Set<ApplicationContextInitializer<GenericApplicationContext>> initializers = new LinkedHashSet<>();
		ConditionService conditions = context.getBean(ConditionService.class);
		for (Imported imported : registrars) {
			if (seen.contains(imported)) {
				continue;
			}
			seen.add(imported);
			Class<?> type = imported.getType();
			if (type != null) {
				if (ImportSelector.class.isAssignableFrom(type)) {
					ImportSelector registrar = (ImportSelector) context
							.getAutowireCapableBeanFactory().createBean(type);
					String[] selected = registrar.selectImports(
							new StandardAnnotationMetadata(imported.getSource()));
					for (String select : selected) {
						if (ClassUtils.isPresent(select, context.getClassLoader())) {
							Class<?> clazz = ClassUtils.resolveClassName(select,
									context.getClassLoader());
							if (conditions.matches(clazz,
									ConfigurationPhase.PARSE_CONFIGURATION)) {
								if (AnnotatedElementUtils.isAnnotated(clazz,
										Configuration.class)) {
									// recurse?
									if (ClassUtils.isPresent(select + "Initializer",
											context.getClassLoader())) {
										@SuppressWarnings("unchecked")
										ApplicationContextInitializer<GenericApplicationContext> initializer = BeanUtils
												.instantiateClass(
														ClassUtils.resolveClassName(
																select + "Initializer",
																context.getClassLoader()),
														ApplicationContextInitializer.class);
										initializers.add(initializer);
									}
								}
								else if (ImportBeanDefinitionRegistrar.class
										.isAssignableFrom(clazz)) {
									added.add(new Imported(imported.getSource(), clazz));
								}
								else {
									context.registerBean(clazz);
								}
							}
						}
					}
				}
				else if (ImportBeanDefinitionRegistrar.class.isAssignableFrom(type)) {
					importRegistrar(registry, imported);
				}
				else {
					try {
						if (type.getAnnotation(Configuration.class) != null) {
							// recurse?
							if (ClassUtils.isPresent(type.getName() + "Initializer",
									context.getClassLoader())) {
								@SuppressWarnings("unchecked")
								ApplicationContextInitializer<GenericApplicationContext> initializer = BeanUtils
										.instantiateClass(
												ClassUtils.resolveClassName(
														type.getName() + "Initializer",
														context.getClassLoader()),
												ApplicationContextInitializer.class);
								initializers.add(initializer);
							}
						}
						else {
							context.registerBean(type);
						}
					}
					catch (ArrayStoreException e) {
						// ignore
					}
				}
			}
			else if (imported.getResources() != null) {
				initializers.add(new XmlInitializer(imported.getResources()));
			}
		}
		for (ApplicationContextInitializer<GenericApplicationContext> initializer : initializers) {
			initializer.initialize(context);
		}
		for (Imported imported : registrars) {
			if (!start.contains(imported)) {
				added.add(imported);
			}
		}
		return added;
	}

	private Set<Imported> ordered(Set<Imported> registrars) {
		Set<Imported> result = new LinkedHashSet<>();
		for (Imported imported : registrars) {
			if (imported.getType() != null && imported.getType().getName()
					.startsWith(AutoConfigurationPackages.class.getName())) {
				result.add(imported);
			}
		}
		result.addAll(registrars);
		return result;
	}

	public void importRegistrar(BeanDefinitionRegistry registry, Imported imported) {
		Class<?> type = imported.getType();
		Object bean = context.getAutowireCapableBeanFactory().createBean(type);
		ImportBeanDefinitionRegistrar registrar = (ImportBeanDefinitionRegistrar) bean;
		registrar.registerBeanDefinitions(
				new StandardAnnotationMetadata(imported.getSource()), registry);
	}

	private static class Imported {
		private Class<?> source;
		private String typeName;
		private Class<?> type;
		private Resource[] resources;

		public Imported(Class<?> source, Class<?> type) {
			this.source = source;
			this.type = type;
			this.typeName = type.getName();
		}

		private Class<?> resolve(ClassLoader classLoader, String typeName) {
			if (ClassUtils.isPresent(typeName, classLoader)) {
				Class<?> clazz = ClassUtils.resolveClassName(typeName, classLoader);
				return clazz;
			}
			return null;
		}

		public Imported(Class<?> source, String location, ClassLoader classLoader) {
			this.source = source;
			this.type = resolve(classLoader, location);
			this.typeName = type == null ? location : type.getName();
		}

		public Imported(Class<?> importer, String location, ApplicationContext loader) {
			this.source = importer;
			try {
				this.resources = loader.getResources(location);
			}
			catch (IOException e) {
			}
		}

		public Resource[] getResources() {
			return resources;
		}

		public Class<?> getSource() {
			return this.source;
		}

		public Class<?> getType() {
			return this.type;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((this.source == null) ? 0 : this.source.getName().hashCode());
			result = prime * result
					+ ((this.typeName == null) ? 0 : this.typeName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Imported other = (Imported) obj;
			if (this.source == null) {
				if (other.source != null)
					return false;
			}
			else if (!this.source.equals(other.source))
				return false;
			if (this.typeName == null) {
				if (other.typeName != null)
					return false;
			}
			else if (!this.typeName.equals(other.typeName))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Imported [source=" + this.source.getName()

					+ ", type=" + this.typeName + "]";
		}

	}

	static class XmlInitializer
			implements ApplicationContextInitializer<GenericApplicationContext> {

		private final Resource[] resources;

		public XmlInitializer(Resource[] resources) {
			this.resources = resources;
		}

		@Override
		public void initialize(GenericApplicationContext context) {
			XmlBeanDefinitionReader xml = new XmlBeanDefinitionReader(context);
			for (Resource resource : resources) {
				xml.loadBeanDefinitions(resource);
			}
		}

	}
}
