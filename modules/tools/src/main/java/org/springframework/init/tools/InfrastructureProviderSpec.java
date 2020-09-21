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

import java.util.Arrays;
import java.util.Collection;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.ClassUtils;

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
		builder.addMethod(createProvider());
		return builder.build();
	}

	private MethodSpec createProvider() {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("getInitializers");
		builder.addAnnotation(Override.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addParameter(GenericApplicationContext.class, "main");
		builder.returns(
				new ParameterizedTypeReference<Collection<? extends ApplicationContextInitializer<GenericApplicationContext>>>() {
				}.getType());
		addInitializers(builder, type);
		return builder.build();
	}

	private void addInitializers(MethodSpec.Builder builder, Class<?> type) {
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
			builder.addCode(",\ninitializers", SpringClassNames.INITIALIZER_LOCATOR, conditions);
		}
		if (hasConditions) {
			builder.addCode(",\nconditions", SpringClassNames.CONDITION_SERVICE, conditions);
		}
		if (hasTypes) {
			builder.addCode(",\ntypes", SpringClassNames.TYPE_SERVICE, types);
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
