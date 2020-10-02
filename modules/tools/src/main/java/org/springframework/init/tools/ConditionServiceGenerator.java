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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.init.func.AnnotationMetadataConditionService;
import org.springframework.init.func.ConditionService;
import org.springframework.init.func.DefaultTypeService;
import org.springframework.init.func.FunctionalInstallerListener;
import org.springframework.init.func.ImportRegistrars;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.init.func.SimpleConditionService;
import org.springframework.init.func.TypeCondition;
import org.springframework.init.func.TypeConditionMapper;
import org.springframework.init.func.TypeConditionService;
import org.springframework.init.func.TypeService;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class ConditionServiceGenerator {

	private GenericApplicationContext main;

	public JavaFile process(Class<?> application) {
		return JavaFile.builder(ClassUtils.getPackageName(application), generate(application)).build();
	}

	public void process(Class<?> application, Set<JavaFile> files) {
		if (ClassUtils.isPresent(application.getName().replace("$", "_") + "Initializer", null)) {
			files.add(JavaFile.builder(ClassUtils.getPackageName(application), generate(application)).build());
		}
	}

	private TypeSpec generate(Class<?> application) {
		Class<?> initializerClass = ClassUtils.resolveClassName(application.getName().replace("$", "_") + "Initializer",
				null);
		String aop = System.setProperty("spring.aop.auto", "false");
		@SuppressWarnings("unchecked")
		ApplicationContextInitializer<GenericApplicationContext> initializer = (ApplicationContextInitializer<GenericApplicationContext>) InfrastructureUtils
				.getOrCreate(getMain(), initializerClass);
		initializer.initialize(getMain());
		ImportRegistrars imports = InfrastructureUtils.getBean(main.getBeanFactory(), ImportRegistrars.class);
		imports.processDeferred(main);
		if (aop == null) {
			System.clearProperty("spring.aop.auto");
		}
		else {
			System.setProperty("spring.aop.auto", aop);
		}
		TypeSpec.Builder builder = TypeSpec
				.classBuilder(ClassName.get(ClassUtils.getPackageName(application), "GeneratedConditionService"));
		SimpleConditionService conditions = InfrastructureUtils.getBean(main.getBeanFactory(),
				SimpleConditionService.class);
		builder.addField(typeMatcher());
		builder.addField(methodMatcher());
		builder.addField(mapperMatcher());
		if (!conditions.getTypeMatches().isEmpty()) {
			builder.addStaticBlock(typeMatchers(conditions.getTypeMatches()));
		}
		if (!conditions.getMethodMatches().isEmpty()) {
			builder.addStaticBlock(methodMatchers(conditions.getMethodMatches()));
		}
		List<String> mappers = SpringFactoriesLoader.loadFactoryNames(TypeConditionMapper.class, null);
		if (!mappers.isEmpty()) {
			builder.addStaticBlock(mapperMatchers(mappers));
		}
		builder.superclass(TypeConditionService.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addMethod(MethodSpec.constructorBuilder().addParameter(GenericApplicationContext.class, "context")
				.addModifiers(Modifier.PUBLIC)
				.addStatement("super(context, new $T(new $T(context), TYPES, METHODS), MAPPERS)", //
						SimpleConditionService.class, AnnotationMetadataConditionService.class)
				.build());
		return builder.build();
	}

	private FieldSpec methodMatcher() {
		FieldSpec.Builder builder = FieldSpec.builder(new ParameterizedTypeReference<Map<String, Boolean>>() {
		}.getType(), "TYPES", Modifier.PRIVATE, Modifier.STATIC);
		builder.initializer("new $T<>()", HashMap.class);
		return builder.build();
	}

	private FieldSpec mapperMatcher() {
		FieldSpec.Builder builder = FieldSpec.builder(new ParameterizedTypeReference<Map<String, TypeCondition>>() {
		}.getType(), "MAPPERS", Modifier.PRIVATE, Modifier.STATIC);
		builder.initializer("new $T<>()", HashMap.class);
		return builder.build();
	}

	private FieldSpec typeMatcher() {
		FieldSpec.Builder builder = FieldSpec
				.builder(new ParameterizedTypeReference<Map<String, Map<String, Boolean>>>() {
				}.getType(), "METHODS", Modifier.PRIVATE, Modifier.STATIC);
		builder.initializer("new $T<>()", HashMap.class);
		return builder.build();
	}

	private CodeBlock methodMatchers(Map<String, Map<String, Boolean>> matches) {
		Builder code = CodeBlock.builder();
		for (String type : matches.keySet()) {
			code.addStatement("METHODS.put($S, new $T<>())", type, HashMap.class);
			Map<String, Boolean> set = matches.get(type);
			for (String returned : set.keySet()) {
				code.addStatement("METHODS.get($S).put($S, $L)", type, returned, set.get(returned));
			}
		}
		return code.build();
	}

	private CodeBlock typeMatchers(Map<String, Boolean> matches) {
		Builder code = CodeBlock.builder();
		for (String type : matches.keySet()) {
			code.addStatement("TYPES.put($S, $L)", type, matches.get(type));
		}
		return code.build();
	}

	private CodeBlock mapperMatchers(List<String> mappers) {
		Builder code = CodeBlock.builder();
		for (String mapper : mappers) {
			if (ClassUtils.isPresent(mapper, null)) {
				Class<?> type = ClassUtils.resolveClassName(mapper, null);
				code.addStatement("MAPPERS.putAll(new $T().get())", type);
			}
		}
		return code.build();
	}

	private GenericApplicationContext getMain() {
		if (this.main == null) {
			WebApplicationType webType = new SpringApplication().getWebApplicationType();
			// TODO: Read application.properties?
			GenericApplicationContext context = new GenericApplicationContext();
			this.main = webType == WebApplicationType.NONE ? new GenericApplicationContext()
					: webType == WebApplicationType.REACTIVE ? new ReactiveWebServerApplicationContext()
							: new ServletWebServerApplicationContext();
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
