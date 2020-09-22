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
package org.springframework.init.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 *
 */
public class InfrastructureProviderSpec {

	private Class<?> type;

	private TypeSpec provider;

	public InfrastructureProviderSpec(Class<?> type) {
		this.type = type;
	}

	public TypeSpec getProvider() {
		if (provider == null) {
			this.provider = createProviderSpec();
		}
		return provider;
	}

	private TypeSpec createProviderSpec() {
		Builder builder = TypeSpec.classBuilder(getClassName());
		builder.addSuperinterface(SpringClassNames.INFRASTRUCTURE_PROVIDER);
		builder.addModifiers(Modifier.PUBLIC);
		Set<Class<?>> binders = getBinders(builder);
		builder.addMethod(createProvider(binders));
		return builder.build();
	}

	private Set<Class<?>> getBinders(Builder builder) {
		Set<Class<?>> result = new HashSet<>();
		if (System.getProperty("spring.init.custom-binders", "false").equals("false")) {
			return result;
		}
		Map<Class<?>, MethodSpec.Builder> methods = new HashMap<>();
		Properties props = new Properties();
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		ConfigurationMetadataRepositoryJsonBuilder jsonBuilder = ConfigurationMetadataRepositoryJsonBuilder.create();
		try {
			for (Resource resource : resolver.getResources("file:./src/main/resources/application.properties")) {
				if (resource.exists()) {
					PropertiesLoaderUtils.fillProperties(props, resource);
				}
			}
			for (Resource resource : resolver.getResources("classpath*:/META-INF/spring-configuration-metadata.json")) {
				jsonBuilder.withJsonResource(resource.getInputStream());
			}
		}
		catch (IOException e) {
			throw new IllegalStateException("Cannot resolve resources", e);
		}
		ConfigurationMetadataRepository json = jsonBuilder.build();
		Map<String, ConfigurationMetadataGroup> groups = json.getAllGroups();
		for (Object key : props.keySet()) {
			String name = (String) key;
			for (String group : groups.keySet()) {
				if (StringUtils.hasText(group) && name.startsWith(group)) {
					ConfigurationMetadataGroup meta = groups.get(group);
					ConfigurationMetadataProperty property = meta.getProperties().get(name);
					if (property != null && ClassUtils.isPresent(property.getType(), null)) {
						for (ConfigurationMetadataSource source : meta.getSources().values()) {
							if (source.getType() != null && ClassUtils.isPresent(source.getType(), null)
									&& AnnotatedElementUtils.hasAnnotation(
											ClassUtils.resolveClassName(source.getType(), null),
											ConfigurationProperties.class)) {
								Class<?> sourceType = ClassUtils.resolveClassName(source.getType(), null);
								result.add(sourceType);
								MethodSpec.Builder spec = methods.computeIfAbsent(sourceType,
										propType -> binderSpec(propType));
								spec.addStatement(
										setterSpec(ClassUtils.resolveClassName(property.getType(), null), meta, name));
							}
						}
					}
				}
			}
		}
		for (Class<?> propType : methods.keySet()) {
			MethodSpec.Builder method = methods.get(propType);
			method.addStatement("return bean");
			builder.addMethod(method.build());
		}
		return result;
	}

	private CodeBlock setterSpec(Class<?> type, ConfigurationMetadataGroup group, String name) {
		CodeBlock.Builder builder = CodeBlock.builder();
		builder.add("bean");
		String prop = name.substring(group.getId().length() + 1);
		String[] subs = StringUtils.delimitedListToStringArray(prop, ".");
		for (int i = 0; i < subs.length - 1; i++) {
			String sub = camelCase(subs[i]);
			builder.add(".get" + StringUtils.capitalize(sub) + "()");
		}
		String leaf = camelCase(subs[subs.length - 1]);
		builder.add(".set" + StringUtils.capitalize(leaf) + "(environment.getProperty($S, $T.class))", name, type);
		return builder.build();
	}

	private String camelCase(String sub) {
		return Arrays.asList(StringUtils.delimitedListToStringArray(sub, "-")).stream()
				.map(value -> StringUtils.capitalize(value)).collect(Collectors.joining());
	}

	private MethodSpec.Builder binderSpec(Class<?> bound) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(StringUtils.uncapitalize(bound.getSimpleName()));
		builder.returns(bound);
		builder.addParameter(bound, "bean");
		builder.addParameter(Environment.class, "environment");
		if (bound.getName().equals("org.springframework.boot.autoconfigure.web.ServerProperties")) {
			builder.addStatement("bean.getServlet().setRegisterDefaultServlet(false)");
		}
		return builder;
	}

	private MethodSpec createProvider(Set<Class<?>> binders) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("getInitializers");
		builder.addAnnotation(Override.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addParameter(GenericApplicationContext.class, "main");
		builder.returns(
				new ParameterizedTypeReference<Collection<? extends ApplicationContextInitializer<GenericApplicationContext>>>() {
				}.getType());
		addInitializers(builder, type, binders);
		return builder.build();
	}

	private void addInitializers(MethodSpec.Builder builder, Class<?> type, Set<Class<?>> binders) {
		ClassName conditions = getConditionServiceName(type);
		ClassName types = getTypeServiceName(type);
		ClassName initializers = getInitializerLocatorName(type);
		boolean hasConditions = false;
		boolean hasInitialiers = false;
		boolean hasTypes = false;
		if (ClassUtils.isPresent(initializers.toString(), null)) {
			builder.addStatement("$T initializers = context -> context.registerBean($T.class, () -> new $T(main))",
					new ParameterizedTypeReference<ApplicationContextInitializer<GenericApplicationContext>>() {
					}.getType(), SpringClassNames.INITIALIZER_LOCATOR, initializers);
			hasInitialiers = true;
		}
		if (ClassUtils.isPresent(conditions.toString(), null)) {
			builder.addStatement("$T conditions = context -> context.registerBean($T.class, () -> new $T(main))",
					new ParameterizedTypeReference<ApplicationContextInitializer<GenericApplicationContext>>() {
					}.getType(), SpringClassNames.CONDITION_SERVICE, conditions);
			hasConditions = true;
		}
		if (ClassUtils.isPresent(types.toString(), null)) {
			builder.addStatement("$T types = context -> context.registerBean($T.class, () -> new $T())",
					new ParameterizedTypeReference<ApplicationContextInitializer<GenericApplicationContext>>() {
					}.getType(), SpringClassNames.TYPE_SERVICE, types);
			hasTypes = true;
		}
		builder.addCode("return $T.asList(", Arrays.class);
		builder.addCode("context -> context.registerBean($T.class, () -> new $T())", getInitializerName(type),
				getInitializerName(type));
		if (hasInitialiers) {
			builder.addCode(",\ninitializers");
		}
		if (hasConditions) {
			builder.addCode(",\nconditions");
		}
		if (hasTypes) {
			builder.addCode(",\ntypes");
		}
		for (Class<?> props : binders) {
			builder.addCode(",\n$T.binder($T.class, this::$L)", SpringClassNames.INFRASTRUCTURE_UTILS, props,
					StringUtils.uncapitalize(props.getSimpleName()));
		}
		builder.addCode(");\n");
	}

	private ClassName getInitializerName(Class<?> type) {
		return ClassName.get(ClassUtils.getPackageName(type), type.getSimpleName().replace("$", "_") + "Initializer");
	}

	private ClassName getInitializerLocatorName(Class<?> type) {
		return ClassName.get(ClassUtils.getPackageName(type), "GeneratedInitializerLocator");
	}

	private ClassName getConditionServiceName(Class<?> type) {
		return ClassName.get(ClassUtils.getPackageName(type), "GeneratedConditionService");
	}

	private ClassName getTypeServiceName(Class<?> type) {
		return ClassName.get(ClassUtils.getPackageName(type), "GeneratedTypeService");
	}

	private ClassName getClassName() {
		return ClassName.get(ClassUtils.getPackageName(type),
				type.getSimpleName().replace("$", "_") + "InfrastructureProvider");
	}

}
