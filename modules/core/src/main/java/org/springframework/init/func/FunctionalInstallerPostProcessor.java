/*
 * Copyright 2018 the original author or authors.
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.init.func.ImportRegistrars.Imported;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 *
 */
public class FunctionalInstallerPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private Set<Imported> deferred = new LinkedHashSet<>();

	private MetadataReaderFactory metadataReaderFactory;

	private enum Phase {

		USER, DEFERRED;

	}

	private Phase phase = Phase.USER;

	private GenericApplicationContext context;

	private TypeService types;

	private ImportRegistrars registrar;

	public FunctionalInstallerPostProcessor(GenericApplicationContext context) {
		this.context = context;
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		this.types = InfrastructureUtils.getBean(beanFactory, TypeService.class);
		this.registrar = InfrastructureUtils.getBean(beanFactory, ImportRegistrars.class);
		this.metadataReaderFactory = InfrastructureUtils.containsBean(beanFactory, MetadataReaderFactory.class)
				? (MetadataReaderFactory) InfrastructureUtils.getBean(beanFactory, MetadataReaderFactory.class)
				: new CachingMetadataReaderFactory(context.getClassLoader());
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		if (registry instanceof SingletonBeanRegistry) {
			SingletonBeanRegistry beans = (SingletonBeanRegistry) registry;
			if (InfrastructureUtils.containsBean(beans, ConfigurationSource.class)) {
				ConfigurationSource sources = InfrastructureUtils.getBean(beans, ConfigurationSource.class);
				register(registry, sources.getInitializers());
				return;
			}
		}
		Set<Imported> seen = new LinkedHashSet<>();
		Set<Imported> added = findAdded(seen, registry);
		while (!added.isEmpty()) {
			for (Imported imported : added) {
				if (!registrar.getImports().contains(imported)) {
					Class<?> type = imported.getType();
					if (type != null && ImportBeanDefinitionRegistrar.class.isAssignableFrom(type)) {
						importRegistrar(registry, imported);
					}
				}
			}
			added = findAdded(seen, registry);
		}
		if (!deferred.isEmpty()) {
			phase = Phase.DEFERRED;
			seen.removeAll(deferred);
			added = findAdded(seen, registry);
			while (!added.isEmpty()) {
				for (Imported imported : added) {
					if (!registrar.getImports().contains(imported)) {
						Class<?> type = imported.getType();
						if (type != null && ImportBeanDefinitionRegistrar.class.isAssignableFrom(type)) {
							importRegistrar(registry, imported);
						}
					}
				}
				added = findAdded(seen, registry);
			}
		}
		for (ApplicationContextInitializer<GenericApplicationContext> initializer : registrar.getDeferred()) {
			initializer.initialize(context);
		}
	}

	private void register(BeanDefinitionRegistry registry,
			List<ApplicationContextInitializer<GenericApplicationContext>> configurations) {
		for (ApplicationContextInitializer<GenericApplicationContext> initializer : configurations) {
			initializer.initialize(context);
		}
		for (ApplicationContextInitializer<GenericApplicationContext> initializer : registrar.getDeferred()) {
			initializer.initialize(context);
		}
	}

	private Set<Imported> findAdded(Set<Imported> seen, BeanDefinitionRegistry registry) {
		Set<Imported> added = new LinkedHashSet<>();
		Set<Imported> start = prioritize(registrar.getImports());
		Map<Class<?>, ApplicationContextInitializer<GenericApplicationContext>> configs = new LinkedHashMap<>();
		Set<ApplicationContextInitializer<GenericApplicationContext>> initializers = new LinkedHashSet<>();
		ConditionService conditions = InfrastructureUtils.getBean(context.getBeanFactory(), ConditionService.class);
		for (Imported imported : registrar.getImports()) {
			if (seen.contains(imported)) {
				continue;
			}
			seen.add(imported);
			Class<?> type = imported.getType();
			if (type != null) {
				if (DeferredImportSelector.class.isAssignableFrom(type)) {
					if (phase == Phase.USER) {
						deferred.add(imported);
						continue;
					}
				}
				if (ImportSelector.class.isAssignableFrom(type)) {
					ImportSelector registrar = (ImportSelector) InfrastructureUtils.getOrCreate(context, type);
					String[] selected = selected(registrar, imported.getSource());
					for (String select : selected) {
						if (types.isPresent(select)) {
							Class<?> clazz = types.getType(select);
							if (conditions.matches(clazz, ConfigurationPhase.PARSE_CONFIGURATION)) {
								if (getMetaData(clazz).isAnnotated(Configuration.class.getName())) {
									// recurse?
									if (types.isPresent(select + "Initializer")) {
										Class<?> initializerType = types.getType(select + "Initializer");
										@SuppressWarnings("unchecked")
										ApplicationContextInitializer<GenericApplicationContext> initializer = (ApplicationContextInitializer<GenericApplicationContext>) InfrastructureUtils
												.getOrCreate(context, initializerType);
										configs.put(clazz, initializer);
									}
								} else if (ImportBeanDefinitionRegistrar.class.isAssignableFrom(clazz)) {
									added.add(new Imported(imported.getSource(), clazz));
								} else {
									context.registerBean(clazz);
								}
							}
						}
					}
				} else if (ImportBeanDefinitionRegistrar.class.isAssignableFrom(type)) {
					importRegistrar(registry, imported);
				} else {
					try {
						if (getMetaData(type).isAnnotated(Configuration.class.getName())) {
							// recurse?
							Class<?> initializerType = types.getType(type.getName() + "Initializer");
							@SuppressWarnings("unchecked")
							ApplicationContextInitializer<GenericApplicationContext> initializer = (ApplicationContextInitializer<GenericApplicationContext>) InfrastructureUtils
									.getOrCreate(context, initializerType);
							configs.put(type, initializer);
						} else {
							context.registerBean(type);
						}
					} catch (ArrayStoreException e) {
						// ignore
					}
				}
			} else if (imported.getResources() != null) {
				initializers.add(new XmlInitializer(imported.getResources()));
			}
		}
		if (phase == Phase.USER) {
			initializers.addAll(configs.values());
		} else {
			for (Class<?> config : AutoConfigurations
					.getClasses(AutoConfigurations.of(configs.keySet().toArray(new Class<?>[0])))) {
				initializers.add(configs.get(config));
			}
		}
		for (ApplicationContextInitializer<GenericApplicationContext> initializer : initializers) {
			initializer.initialize(context);
		}
		for (Imported imported : registrar.getImports()) {
			if (!start.contains(imported)) {
				added.add(imported);
			}
		}
		return added;
	}

	private String[] selected(ImportSelector registrar, Class<?> importer) {
		if (registrar instanceof DeferredImportSelector) {
			return new DeferredConfigurations(Stream.of(registrar.selectImports(getMetaData(importer)))
					.map(name -> types.getType(name)).filter(type -> type != null).collect(Collectors.toList())).list();
		}
		return registrar.selectImports(getMetaData(importer));
	}

	static class DeferredConfigurations extends AutoConfigurations {

		protected DeferredConfigurations(Collection<Class<?>> classes) {
			super(classes);
		}

		public String[] list() {
			return getClasses().stream().map(cls -> cls.getName()).collect(Collectors.toList()).toArray(new String[0]);
		}

	}

	private Set<Imported> prioritize(Set<Imported> registrars) {
		Set<Imported> result = new LinkedHashSet<>();
		for (Imported imported : registrars) {
			if (imported.getType() != null
					&& imported.getType().getName().startsWith(AutoConfigurationPackages.class.getName())) {
				result.add(imported);
			}
		}
		result.addAll(registrars);
		return result;
	}

	public void importRegistrar(BeanDefinitionRegistry registry, Imported imported) {
		Class<?> type = imported.getType();
		Object bean = InfrastructureUtils.getOrCreate(context, type);
		ImportBeanDefinitionRegistrar registrar = (ImportBeanDefinitionRegistrar) bean;
		registrar.registerBeanDefinitions(getMetaData(imported.getSource()), registry, IMPORT_BEAN_NAME_GENERATOR);
	}

	private AnnotationMetadata getMetaData(Class<?> imported) {
		try {
			return this.metadataReaderFactory.getMetadataReader(imported.getName()).getAnnotationMetadata();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot find metadata for " + imported, e);
		}
	}

	public static final AnnotationBeanNameGenerator IMPORT_BEAN_NAME_GENERATOR = new AnnotationBeanNameGenerator() {
		@Override
		protected String buildDefaultBeanName(BeanDefinition definition) {
			String beanClassName = definition.getBeanClassName();
			Assert.state(beanClassName != null, "No bean class name set");
			return beanClassName;
		}
	};

}
