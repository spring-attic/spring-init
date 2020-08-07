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

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.init.func.AnnotationMetadataConditionService;
import org.springframework.init.func.DefaultTypeService;
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
public class TypeServiceGenerator {

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
		TypeSpec.Builder builder = TypeSpec
				.classBuilder(ClassName.get(ClassUtils.getPackageName(application), "GeneratedTypeService"));
		builder.addField(typeMatcher());
		builder.addStaticBlock(typeMatchers(Collections.singletonMap(initializerClass.getName(), initializerClass)));
		builder.superclass(DefaultTypeService.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
				.addStatement("super(TYPES)", AnnotationMetadataConditionService.class).build());
		return builder.build();
	}

	private FieldSpec typeMatcher() {
		FieldSpec.Builder builder = FieldSpec.builder(new ParameterizedTypeReference<Map<String, Class<?>>>() {
		}.getType(), "TYPES", Modifier.PRIVATE, Modifier.STATIC);
		builder.initializer("new $T<>()", HashMap.class);
		return builder.build();
	}

	private CodeBlock typeMatchers(Map<String, Class<?>> matches) {
		Builder code = CodeBlock.builder();
		for (String type : matches.keySet()) {
			code.addStatement("TYPES.put($S, $T.class)", type, matches.get(type));
		}
		return code.build();
	}
}
