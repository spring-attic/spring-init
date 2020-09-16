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

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.init.func.DefaultInitializerLocator;
import org.springframework.init.func.SimpleInitializerLocator;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class InitializerLocatorGenerator {

	public JavaFile process(Class<?> application) {
		return JavaFile.builder(ClassUtils.getPackageName(application), generate(application)).build();
	}

	public void process(Class<?> application, Set<JavaFile> files) {
		if (ClassUtils.isPresent(application.getName().replace("$", "_") + "Initializer", null)) {
			files.add(process(application));
		}
	}

	private TypeSpec generate(Class<?> application) {
		TypeSpec.Builder builder = TypeSpec
				.classBuilder(ClassName.get(ClassUtils.getPackageName(application), "GeneratedInitializerLocator"));
		builder.addField(typeMatcher());
		builder.addStaticBlock(initializers());
		builder.superclass(DefaultInitializerLocator.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
				.addParameter(GenericApplicationContext.class, "context").addStatement("super(context)")
				.addStatement("register(new $T(TYPES))", SimpleInitializerLocator.class).build());
		return builder.build();
	}

	private FieldSpec typeMatcher() {
		FieldSpec.Builder builder = FieldSpec.builder(
				new ParameterizedTypeReference<Map<String, ApplicationContextInitializer<GenericApplicationContext>>>() {
				}.getType(), "TYPES", Modifier.PRIVATE, Modifier.STATIC);
		builder.initializer("new $T<>()", HashMap.class);
		return builder.build();
	}

	private CodeBlock initializers() {
		Builder code = CodeBlock.builder();
		for (String type : SpringFactoriesLoader.loadFactoryNames(EnableAutoConfiguration.class, null)) {
			String initializer = type.replace("$", "_") + "Initializer";
			if (ClassUtils.isPresent(initializer, null)) {
				code.addStatement("TYPES.put($S, new $T())", type,
						TypeName.get(ClassUtils.resolveClassName(initializer, null)));
			}
		}
		return code.build();
	}

}
