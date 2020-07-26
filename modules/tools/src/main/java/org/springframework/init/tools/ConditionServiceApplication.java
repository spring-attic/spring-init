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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.init.func.AnnotationMetadataConditionService;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.DefaultTypeService;
import org.springframework.init.func.FunctionalInstallerListener;
import org.springframework.init.func.ImportRegistrars;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.init.func.SimpleConditionService;
import org.springframework.init.func.TypeService;
import org.springframework.util.ClassUtils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

/**
 * @author Dave Syer
 *
 */
public class ConditionServiceApplication {

	private GenericApplicationContext main;

	public JavaFile process(Class<?> application) {
		return JavaFile.builder(ClassUtils.getPackageName(application), generate(application)).build();
	}

	private TypeSpec generate(Class<?> application) {
		Class<?> initializerClass = ClassUtils.resolveClassName(application.getName().replace("$", "_") + "Initializer",
				null);
		@SuppressWarnings("unchecked")
		ApplicationContextInitializer<GenericApplicationContext> initializer = (ApplicationContextInitializer<GenericApplicationContext>) InfrastructureUtils
				.getOrCreate(getMain(), initializerClass);
		initializer.initialize(getMain());
		ImportRegistrars imports = InfrastructureUtils.getBean(main.getBeanFactory(), ImportRegistrars.class);
		for (ApplicationContextInitializer<GenericApplicationContext> init : imports.getDeferred()) {
			init.initialize(main);
		}
		TypeSpec.Builder builder = TypeSpec
				.classBuilder(ClassName.get(ClassUtils.getPackageName(application), "GeneratedConditionService"));
		SimpleConditionService conditions = InfrastructureUtils.getBean(main.getBeanFactory(),
				SimpleConditionService.class);
		builder.addField(typeMatcher());
		builder.addField(methodMatcher());
		if (!conditions.getTypeMatches().isEmpty()) {
			builder.addStaticBlock(typeMatchers(conditions.getTypeMatches()));
		}
		if (!conditions.getMethodMatches().isEmpty()) {
			builder.addStaticBlock(methodMatchers(conditions.getMethodMatches()));
		}
		builder.superclass(SimpleConditionService.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
				.addStatement("super(TYPES, METHODS)").build());
		return builder.build();
	}

	private FieldSpec methodMatcher() {
		FieldSpec.Builder builder = FieldSpec.builder(new ParameterizedTypeReference<Map<Class<?>, Boolean>>() {
		}.getType(), "TYPES", Modifier.PRIVATE, Modifier.STATIC);
		builder.initializer("new $T<>()", HashMap.class);
		return builder.build();
	}

	private FieldSpec typeMatcher() {
		FieldSpec.Builder builder = FieldSpec.builder(new ParameterizedTypeReference<Map<Class<?>, Set<Class<?>>>>() {
		}.getType(), "METHODS", Modifier.PRIVATE, Modifier.STATIC);
		builder.initializer("new $T<>()", HashMap.class);
		return builder.build();
	}

	private CodeBlock methodMatchers(Map<Class<?>, Set<Class<?>>> matches) {
		Builder code = CodeBlock.builder();
		for (Class<?> type : matches.keySet()) {
			code.add("METHODS.put($T.class, new $T<>(", type, HashSet.class);
			Set<Class<?>> set = matches.get(type);
			if (!set.isEmpty()) {
				code.add("$T.asList(", Arrays.class);
				code.add(join("$T.class", set.size()), set.toArray());
				code.add(")");
			}
			code.add("));\n");
		}
		return code.build();
	}

	private String join(String string, int size) {
		StringBuilder builder = new StringBuilder(string);
		for (int i = 1; i < size; i++) {
			builder.append(", ").append(string);
		}
		return builder.toString();
	}

	private CodeBlock typeMatchers(Map<Class<?>, Boolean> matches) {
		Builder code = CodeBlock.builder();
		for (Class<?> type : matches.keySet()) {
			code.addStatement("TYPES.put($T.class, $L)", type, matches.get(type));
		}
		return code.build();
	}

	private GenericApplicationContext getMain() {
		if (this.main == null) {
			// TODO: Read application.properties?
			GenericApplicationContext context = new GenericApplicationContext();
			this.main = new GenericApplicationContext();
			context.refresh();
			InfrastructureUtils.install(main.getBeanFactory(), context);
			context.getBeanFactory().registerSingleton(TypeService.class.getName(),
					new DefaultTypeService(context.getClassLoader()));
			context.getBeanFactory().registerSingleton(ConditionService.class.getName(),
					new SimpleConditionService(new AnnotationMetadataConditionService(main)));
			FunctionalInstallerListener.initialize(main);
		}
		return this.main;
	}

}
