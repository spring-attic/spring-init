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
package org.springframework.init.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataSource;
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
public class CustomBinderBuilder {

	public Set<MethodSpec> getBinders() {
		Set<MethodSpec> result = new HashSet<>();
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
			for (Resource resource : resolver.getResources("classpath*:application.properties")) {
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
							if (source.getType() != null && ClassUtils.isPresent(source.getType(), null)) {
								Class<?> sourceType = ClassUtils.resolveClassName(source.getType(), null);
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
			result.add(method.build());
		}
		return result;
	}

	private MethodSpec.Builder binderSpec(Class<?> bound) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(StringUtils.uncapitalize(bound.getSimpleName()));
		builder.returns(bound);
		builder.addParameter(bound, "bean");
		builder.addParameter(Environment.class, "environment");
		return builder;
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

}
