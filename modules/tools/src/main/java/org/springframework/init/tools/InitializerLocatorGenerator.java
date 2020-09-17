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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.init.func.DefaultInitializerLocator;
import org.springframework.init.func.SimpleInitializerLocator;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class InitializerLocatorGenerator {

	public JavaFile process(Class<?> application) {
		String packageName = ClassUtils.getPackageName(application);
		return JavaFile.builder(packageName, generate(packageName, Collections.singleton(application), false)).build();
	}

	public void process(Class<?> application, Set<JavaFile> files) {
		if (ClassUtils.isPresent(application.getName().replace("$", "_") + "Initializer", null)) {
			files.add(process(application));
		}
	}

	public JavaFile process(String packageName, Set<Class<?>> applications) {
		return JavaFile.builder(packageName, generate(packageName, applications, true)).build();
	}

	public void process(String packageName, Set<Class<?>> applications, Set<JavaFile> files) {
		if (applications.isEmpty()) {
			return;
		}
		files.add(process(packageName, applications));
	}

	private TypeSpec generate(String packageName, Set<Class<?>> applications, boolean simple) {
		TypeSpec.Builder builder = TypeSpec.classBuilder(ClassName.get(packageName, "GeneratedInitializerLocator"));
		builder.addField(typeMatcher());
		builder.addStaticBlock(initializers(applications));
		builder.addModifiers(Modifier.PUBLIC);
		if (simple) {
			builder.superclass(SimpleInitializerLocator.class);
			builder.addMethod(createSimple());
		}
		else {
			builder.superclass(DefaultInitializerLocator.class);
			builder.addMethod(createDefault());
		}
		return builder.build();
	}

	private MethodSpec createSimple() {
		MethodSpec.Builder builder = MethodSpec.constructorBuilder();
		builder.addModifiers(Modifier.PUBLIC);
		builder.beginControlFlow("for (String name : TYPES.keySet())");
		builder.addStatement("register(name, TYPES.get(name))");
		builder.endControlFlow();
		return builder.build();
	}

	private MethodSpec createDefault() {
		MethodSpec.Builder builder = MethodSpec.constructorBuilder();
		builder.addModifiers(Modifier.PUBLIC);
		return builder.addParameter(GenericApplicationContext.class, "context").addStatement("super(context)")
				.addStatement("register(new $T(TYPES))", SimpleInitializerLocator.class).build();
	}

	private FieldSpec typeMatcher() {
		FieldSpec.Builder builder = FieldSpec.builder(
				new ParameterizedTypeReference<Map<String, ApplicationContextInitializer<GenericApplicationContext>>>() {
				}.getType(), "TYPES", Modifier.PRIVATE, Modifier.STATIC);
		builder.initializer("new $T<>()", HashMap.class);
		return builder.build();
	}

	private CodeBlock initializers(Set<Class<?>> applications) {
		Builder code = CodeBlock.builder();
		for (Class<?> type : applications) {
			String initializer = type.getName().replace("$", "_") + "Initializer";
			if (ClassUtils.isPresent(initializer, null)) {
				code.addStatement("TYPES.put($S, new $T())", type.getName(),
						TypeName.get(ClassUtils.resolveClassName(initializer, null)));
			}
		}
		return code.build();
	}

}
