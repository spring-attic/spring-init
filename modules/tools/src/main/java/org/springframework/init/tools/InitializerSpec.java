/*
 * Copyright 2016-2017 the original author or authors.
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

/**
 * @author Dave Syer
 *
 */
public class InitializerSpec implements Comparable<InitializerSpec> {

	private static Log logger = LogFactory.getLog(InitializerSpec.class);

	private TypeSpec initializer;

	private String pkg;

	private Class<?> configurationType;

	private ElementUtils utils;

	private ClassName className;

	private Imports imports;

	private Resources resources = new Resources();

	private Components components;

	private boolean hasEnabled = false;

	private InitializerSpecs specs;

	public InitializerSpec(InitializerSpecs specs, ElementUtils utils, Class<?> type, Imports imports,
			Components components) {
		this.specs = specs;
		this.utils = utils;
		this.components = components;
		this.className = toInitializerNameFromConfigurationName(type);
		this.pkg = ClassName.get(type).packageName();
		type = imports.getImports().containsKey(type) && type.isAnnotation()
				? imports.getImports().get(type).iterator().next()
				: type;
		this.configurationType = type;
		this.imports = imports;
		for (Class<?> imported : utils.getTypesFromAnnotation(type, SpringClassNames.IMPORT.toString(), "value")) {
			imports.addImport(type, imported);
		}
		for (String imported : utils.getStringsFromAnnotation(type, SpringClassNames.IMPORT_RESOURCE.toString(),
				"value")) {
			resources.addResource(type, imported);
		}
	}

	public Class<?> getConfigurationType() {
		return configurationType;
	}

	public void setConfigurationType(Class<?> configurationType) {
		this.configurationType = configurationType;
	}

	public TypeSpec getInitializer() {
		if (initializer == null) {
			this.initializer = createInitializer(configurationType);
		}
		return initializer;
	}

	public void setInitializer(TypeSpec module) {
		this.initializer = module;
	}

	public String getPackage() {
		return pkg;
	}

	public void setPackage(String pkg) {
		this.pkg = pkg;
	}

	private TypeSpec createInitializer(Class<?> type) {
		Builder builder = TypeSpec.classBuilder(getClassName());
		builder.addSuperinterface(SpringClassNames.INITIALIZER_TYPE);
		builder.addModifiers(Modifier.PUBLIC);
		this.hasEnabled = maybeAddEnabled(builder);
		builder.addMethod(createInitializer());
		return builder.build();
	}

	private boolean maybeAddEnabled(Builder builder) {
		boolean conditional = utils.hasAnnotation(this.configurationType,
				SpringClassNames.CONDITIONAL_ON_CLASS.toString());
		if (!conditional) {
			return false;
		}
		Annotation anno = utils.getAnnotation(this.configurationType, SpringClassNames.CONDITIONAL_ON_CLASS.toString());
		Map<String, Object> values = AnnotationUtils.getAnnotationAttributes(anno);
		List<String> types = new ArrayList<>();
		for (Entry<String, Object> entry : values.entrySet()) {
			if (entry.getKey().toString().equals("value")) {
				Class<?>[] value = (Class<?>[]) entry.getValue();
				for (Class<?> type : value) {
					types.add(type.getName());
				}
			} else if (entry.getKey().toString().equals("name")) {
				String[] value = (String[]) entry.getValue();
				for (String type : value) {
					types.add(type);
				}
			}
			// TODO: enumerate class names as well
		}
		if (types.isEmpty()) {
			return false;
		}
		builder.addField(TypeName.BOOLEAN, "enabled", Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL);
		com.squareup.javapoet.CodeBlock.Builder code = CodeBlock.builder();
		code.add("enabled =\n");
		for (int i = 0; i < types.size(); i++) {
			String type = types.get(i);
			code.add("$T.isPresent($S, null)", SpringClassNames.CLASS_UTILS, type);
			if (i < types.size() - 1) {
				code.add(" &&\n");
			} else {
				code.add(";\n");
			}
		}
		builder.addStaticBlock(code.build());
		return true;
	}

	public static ClassName toInitializerNameFromConfigurationName(Class<?> type) {
		return toInitializerNameFromConfigurationName(ClassName.get(type));
	}

	public static ClassName toInitializerNameFromConfigurationName(ClassName type) {
		String name = type.simpleName();
		if (type.enclosingClassName() != null) {
			name = type.enclosingClassName().simpleName() + "_" + name;
		}
		return ClassName.get(type.packageName(), name + "Initializer");
	}

	private MethodSpec createInitializer() {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("initialize");
		builder.addAnnotation(Override.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addParameter(SpringClassNames.GENERIC_APPLICATION_CONTEXT, "context");
		addBeanMethods(builder, configurationType);
		return builder.build();
	}

	private void addRegistrarInvokers(MethodSpec.Builder builder) {
		addImportInvokers(builder, configurationType);
		Annotation[] annotationMirrors = configurationType.getAnnotations();
		for (Annotation am : annotationMirrors) {
			// Looking up something like @EnableBar
			Class<?> element = am.annotationType();
			addImportInvokers(builder, element);
		}
	}

	private void addImportInvokers(MethodSpec.Builder builder, Class<?> element) {
		Set<Class<?>> registrarInitializers = imports.getImports().get(element);
		if (registrarInitializers != null) {
			for (Class<?> imported : registrarInitializers) {
				if (utils.isConfigurationProperties(imported)) {
					List<Class<?>> types = utils.getTypesFromAnnotation(configurationType,
							SpringClassNames.ENABLE_CONFIGURATION_PROPERTIES.reflectionName(), "value");
					// TODO: Can we limit this to once only? Is it worth it (would eventually fail
					// e.g. in test slices)
					builder.addStatement("$T.register(context)",
							SpringClassNames.CONFIGURATION_PROPERTIES_BINDING_POST_PROCESSOR);
					for (Class<?> type : types) {
						builder.beginControlFlow(
								"if (context.getBeanFactory().getBeanNamesForType($T.class).length==0)", type);
						builder.addStatement("context.registerBean($T.class, () -> new $T())", type, type);
						builder.endControlFlow();
					}
				} else if (utils.isImportWithNoMetadata(imported)) {
					builder.addStatement(
							"$T.invokeAwareMethods(new $T(), context.getEnvironment(), context, context).registerBeanDefinitions(null, context)",
							SpringClassNames.INFRASTRUCTURE_UTILS, imported);
				} else if (utils.isImportSelector(imported)) {
					builder.addStatement("$T.getBean(context.getBeanFactory(), $T.class).add($T.class, \"$L\")",
							SpringClassNames.INFRASTRUCTURE_UTILS, SpringClassNames.IMPORT_REGISTRARS,
							configurationType, imported.getCanonicalName());
				} else if (utils.isAutoConfigurationPackages(imported)) {
					// TODO: extract base packages from configurationType
					builder.addStatement("$T.register(context, $S)", SpringClassNames.AUTOCONFIGURATION_PACKAGES, pkg);
				} else if (utils.isImportBeanDefinitionRegistrar(imported)) {
					boolean accessible = false;
					try {
						accessible = imported.getName().startsWith(pkg)
								|| java.lang.reflect.Modifier.isPublic(imported.getConstructor().getModifiers());
					} catch (Exception e) {
						// ignore
					}
					builder.beginControlFlow("try");
					// TODO: Have another look at the BeanNameGenerator if
					// https://jira.spring.io/browse/DATACMNS-1770 is fixed
					if (accessible) {
						builder.addStatement(
								"$T.invokeAwareMethods(new $T(), context.getEnvironment(), context, context).registerBeanDefinitions($T.getBean(context.getBeanFactory(), $T.class).getMetadataReader($S).getAnnotationMetadata(), context, $T.getBean(context.getBeanFactory(), $T.class))",
								SpringClassNames.INFRASTRUCTURE_UTILS, imported, SpringClassNames.INFRASTRUCTURE_UTILS,
								SpringClassNames.METADATA_READER_FACTORY, configurationType.getName(),
								SpringClassNames.INFRASTRUCTURE_UTILS, SpringClassNames.BEAN_NAME_GENERATOR);
					} else {
						builder.addStatement(
								"(($T)$T.getOrCreate(context, $S)).registerBeanDefinitions($T.getBean(context.getBeanFactory(), $T.class).getMetadataReader($S).getAnnotationMetadata(), context, $T.getBean(context.getBeanFactory(), $T.class))",
								SpringClassNames.IMPORT_BEAN_DEFINITION_REGISTRAR,
								SpringClassNames.INFRASTRUCTURE_UTILS, imported.getName().replace("$", "."),
								SpringClassNames.INFRASTRUCTURE_UTILS, SpringClassNames.METADATA_READER_FACTORY,
								configurationType.getName(), SpringClassNames.INFRASTRUCTURE_UTILS,
								SpringClassNames.BEAN_NAME_GENERATOR);
					}
					builder.nextControlFlow("catch ($T e)", IOException.class)
							.addStatement(" throw new IllegalStateException(e)").endControlFlow();
				} else if (utils.hasAnnotation(imported, SpringClassNames.CONFIGURATION.toString())) {
					ClassName initializerName = InitializerSpec.toInitializerNameFromConfigurationName(imported);
					if (!ClassUtils.isPresent(initializerName.toString(), null)) {
						// Hack an initializer together (ideally we'd have full coverage in all
						// dependencies)
						if (!imported.getName().startsWith(pkg)) {
							logger.warn("Creating initializer on the fly for: " + imported.getName());
						}
						specs.addInitializer(imported);
					}
					builder.addStatement("new $T().initialize(context)", initializerName);
				} else {
					builder.addStatement("context.registerBean($T.class)", imported);
				}
			}
		}
	}

	private void addBeanMethods(MethodSpec.Builder builder, Class<?> type) {
		boolean conditional = utils.hasAnnotation(type, SpringClassNames.CONDITIONAL.toString());
		if (this.hasEnabled) {
			builder.beginControlFlow("if ($T.enabled)", this.className);
		}
		if (conditional) {
			builder.addStatement("$T conditions = $T.getBean(context.getBeanFactory(), $T.class)",
					SpringClassNames.CONDITION_SERVICE, SpringClassNames.INFRASTRUCTURE_UTILS,
					SpringClassNames.CONDITION_SERVICE);
			builder.beginControlFlow("if (conditions.matches($T.class))", type);
		}
		builder.beginControlFlow("if (context.getBeanFactory().getBeanNamesForType($T.class).length==0)", type);
		builder.addStatement("$T types = $T.getBean(context.getBeanFactory(), $T.class)", SpringClassNames.TYPE_SERVICE,
				SpringClassNames.INFRASTRUCTURE_UTILS, SpringClassNames.TYPE_SERVICE);
		boolean conditionsAvailable = addScannedComponents(builder, conditional);
		addNewBeanForConfig(builder, type);
		for (Method method : getBeanMethods(type)) {
			conditionsAvailable |= createBeanMethod(builder, method, type, conditionsAvailable);
		}
		addResources(builder);
		addRegistrarInvokers(builder);
		builder.endControlFlow();
		if (conditional) {
			builder.endControlFlow();
		}
		if (this.hasEnabled) {
			builder.endControlFlow();
		}
	}

	private void addResources(MethodSpec.Builder builder) {
		Set<String> locations = resources.getResources().get(configurationType);
		if (locations != null) {
			for (String location : locations) {
				builder.addStatement("new $T($S).initialize(context)", SpringClassNames.XML_INITIALIZER, location);
			}
		}
	}

	private boolean addScannedComponents(MethodSpec.Builder builder, boolean conditional) {
		Set<Class<?>> set = components.getComponents().get(configurationType);
		boolean filtered = false;
		if (!utils.getAnnotationsFromAnnotation(configurationType, SpringClassNames.COMPONENT_SCAN.toString(),
				"excludeFilters").isEmpty()) {
			filtered = true;
		}
		if (set != null) {
			for (Class<?> imported : set) {
				if (!imported.equals(configurationType)) {
					if (filtered) {
						if (!conditional) {
							builder.addStatement("$T conditions = $T.getBean(context.getBeanFactory(), $T.class)",
									SpringClassNames.CONDITION_SERVICE, SpringClassNames.INFRASTRUCTURE_UTILS,
									SpringClassNames.CONDITION_SERVICE);
							conditional = true;
						}
						includes(builder, imported);
					}
					if (utils.hasAnnotation(imported, SpringClassNames.CONFIGURATION.toString())) {
						builder.addStatement("new $T().initialize(context)",
								InitializerSpec.toInitializerNameFromConfigurationName(imported));
					} else {
						if (java.lang.reflect.Modifier.isPublic(imported.getModifiers())) {
							Constructor<?> constructor = getConstructor(imported);
							ParameterSpecs params = autowireParamsForMethod(constructor);
							builder.addStatement("context.registerBean($T.class, () -> new $T(" + params.format + "))",
									ArrayUtils.merge(imported, imported, params.args));
						} else {
							builder.addStatement("context.registerBean(types.getType($S))", imported.getName());

						}
					}
					if (filtered) {
						builder.endControlFlow();
					}
				}
			}
		}
		return conditional;
	}

	private void includes(com.squareup.javapoet.MethodSpec.Builder builder, Class<?> imported) {
		if (java.lang.reflect.Modifier.isPublic(imported.getModifiers())) {
			builder.beginControlFlow("if (conditions.includes($T.class))", imported);
		} else {
			builder.beginControlFlow("if (conditions.includes(types.getType($S)))", imported.getName());
		}
	}

	private void addNewBeanForConfig(MethodSpec.Builder builder, Class<?> type) {
		Constructor<?> constructor = getConstructor(type);
		ParameterSpecs params = autowireParamsForMethod(constructor);
		builder.addStatement("context.registerBean($T.class, () -> new $T(" + params.format + "))",
				ArrayUtils.merge(type, type, params.args));
	}

	private boolean createBeanMethod(MethodSpec.Builder builder, Method beanMethod, Class<?> type,
			boolean conditionsAvailable) {
		// TODO will need to handle bean methods in private configs
		try {
			Class<?> returnTypeElement = utils.getReturnType(beanMethod);
			boolean conditional = utils.hasAnnotation(beanMethod, SpringClassNames.CONDITIONAL.toString());
			if (conditional) {
				if (!conditionsAvailable) {
					builder.addStatement("$T conditions = $T.getBean(context.getBeanFactory(), $T.class)",
							SpringClassNames.CONDITION_SERVICE, SpringClassNames.INFRASTRUCTURE_UTILS,
							SpringClassNames.CONDITION_SERVICE);
				}
			}

			if (java.lang.reflect.Modifier.isPrivate(returnTypeElement.getModifiers())) {

				if (conditional) {
					builder.beginControlFlow("if (conditions.matches($T.class, types.getType($S)))", type,
							returnTypeElement.getName());
				}
				logger.info("Generating source for bean method, type involved is private: "
						+ beanMethod.getDeclaringClass() + "." + beanMethod);
				builder.addStatement("context.registerBean(types.getType($S))", returnTypeElement.getName());

			} else {

				if (conditional) {
					builder.beginControlFlow("if (conditions.matches($T.class, $T.class))", type, returnTypeElement);
				}
				ParameterSpecs params = autowireParamsForMethod(beanMethod);

				builder.addStatement("context.registerBean(" + "\"" + beanMethod.getName() + "\", $T.class, "
						+ supplier(type, beanMethod, params.format) + customizer(type, beanMethod, params) + ")",
						ArrayUtils.merge(returnTypeElement, type, params.args));
			}

			if (conditional) {
				builder.endControlFlow();
			}

			return conditional;
		} catch (Throwable t) {
			throw new RuntimeException(
					"Problem performing createBeanMethod for method " + type.toString() + "." + beanMethod.toString(),
					t);
		}
	}

	private String customizer(Class<?> type, Method beanMethod, ParameterSpecs params) {
		StringBuilder builder = new StringBuilder(", ");
		boolean hasInit = false;
		StringBuilder body = new StringBuilder();
		if (utils.hasAnnotation(beanMethod, SpringClassNames.BEAN.toString())) {
			String methodName = utils.getStringFromAnnotation(beanMethod, SpringClassNames.BEAN.toString(),
					"initMethod");
			if (methodName != null && methodName.length() > 0) {
				body.append("def.setInitMethodName(\"" + methodName + "\")");
			}
			methodName = utils.getStringFromAnnotation(beanMethod, SpringClassNames.BEAN.toString(), "destroyMethod");
			if (methodName != null && methodName.length() > 0 && !methodName.startsWith("(")) {
				hasInit = body.length() > 0;
				if (hasInit && body.indexOf("{") != 0) {
					body.insert(0, "{");
					body.append("; ");
				}
				body.append("def.setDestroyMethodName(\"" + methodName + "\")");
			}
		}
		if (utils.hasAnnotation(beanMethod, SpringClassNames.CONFIGURATION_PROPERTIES.toString())
				|| utils.implementsInterface(beanMethod.getReturnType(), FactoryBean.class)) {
			String methodName = beanMethod.getName().toString();
			// The bean name for the @Configuration class is the class name
			String factoryName = "$T.class.getName()";
			params.addArg(type);
			body.append("{ def.setFactoryMethodName(\"" + methodName + "\"); ");
			body.append("def.setFactoryBeanName(" + factoryName + ")");
			hasInit = true;
		}
		if (body.length() > 0) {
			if (hasInit) {
				body.append(";}");
			}
			builder.append("def -> ").append(body.toString());
		}
		return builder.length() > 2 ? builder.toString() : "";
	}

	private ParameterSpecs autowireParamsForMethod(Executable method) {
		List<ParameterSpec> parameterTypes = getParameters(method, this::parameterAccessor)
				.collect(Collectors.toList());

		String format = parameterTypes.stream().map(param -> param.format).collect(Collectors.joining(","));
		Object[] args = parameterTypes.stream().flatMap(param -> param.types.stream()).collect(Collectors.toList())
				.toArray();

		ParameterSpecs params = new ParameterSpecs();
		params.format = format;
		params.args = args;
		return params;
	}

	private String supplier(Class<?> owner, Method beanMethod, String parameterVariables) {
		boolean exception = utils.throwsCheckedException(beanMethod);
		String code;
		if (!java.lang.reflect.Modifier.isStatic(beanMethod.getModifiers())) {
			code = "context.getBean($T.class)." + beanMethod.getName() + "(" + parameterVariables + ")";
		} else {
			code = "$T." + beanMethod.getName() + "(" + parameterVariables + ")";
		}
		if (exception) {
			return "() -> { try { return " + code + "; } catch (Exception e) { throw new IllegalStateException(e); } }";
		}
		return "() -> " + code;
	}

	private ParameterSpec parameterAccessor(Parameter param) {
		// TODO: Use this:
		// ResolvableType.forMethodParameter(MethodParameter.forParameter(param));
		ParameterSpec result = new ParameterSpec();
		Type paramType = param.getParameterizedType();
		Class<?> typeElement = param.getType();
		String paramTypename = utils.getParameterType(param);
		if (utils.implementsInterface(param.getType(), ObjectProvider.class)) {
			if (paramType instanceof ParameterizedType) {
				Type[] args = ((ParameterizedType) paramType).getActualTypeArguments();
				if (args.length > 0) {
					Type value = args[0];
					if (utils.erasure(value).equals(Map.class.getName())) {
						result.format = "$T.map(context, $T.class)";
						result.types.add(SpringClassNames.OBJECT_UTILS);
						Type[] iterator = ((ParameterizedType) value).getActualTypeArguments();
						value = iterator[1];
						result.types.add(TypeName.get(value));
					} else if (value instanceof ParameterizedType
							&& ((ParameterizedType) value).getActualTypeArguments().length > 0) {
						result.format = "context.getBeanProvider($T.forClassWithGenerics($T.class, "
								+ Stream.of(((ParameterizedType) value).getActualTypeArguments())
										.map(thing -> "$T.class").collect(Collectors.joining(", "))
								+ "))";
						result.types.add(SpringClassNames.RESOLVABLE_TYPE);
						result.types.add(TypeName.get(((ParameterizedType) value).getRawType()));
						Arrays.asList(((ParameterizedType) value).getActualTypeArguments()).forEach(t -> {
							if (t instanceof ParameterizedType) {
								t = ((ParameterizedType) t).getRawType();
							}
							TypeName v = TypeName.get(t);
							// The target type itself is generic. So far we only support
							// one level of generic parameters. Further levels could be
							// supported by adding calls to ResolvableType
							if ("?".equals(v.toString())) {
								result.types.add(TypeName.OBJECT);
							} else {
								result.types.add(v);
							}
						});
					} else if (value instanceof Class && ((Class<?>) value).isArray()) {
						result.format = "$T.array(context, $T.class)";
						result.types.add(SpringClassNames.OBJECT_UTILS);
						value = ((Class<?>) value).getComponentType();
						result.types.add(TypeName.get(value));
					} else {
						result.format = "context.getBeanProvider($T.class)";
						result.types.add(TypeName.get(value));
					}
				}
			}
		} else if (utils.implementsInterface(typeElement, ApplicationContext.class)
				|| utils.implementsInterface(typeElement, ResourceLoader.class)
				|| utils.implementsInterface(typeElement, ApplicationEventPublisher.class)
				|| paramTypename.equals(SpringClassNames.CONFIGURABLE_APPLICATION_CONTEXT.toString())) {
			if (utils.implementsInterface(typeElement, SpringClassNames.WEB_APPLICATION_CONTEXT)) {
				result.format = "($T)context";
				result.types.add(ClassName.get(typeElement));
			} else {
				result.format = "context";
			}
		} else if (utils.implementsInterface(typeElement, BeanFactory.class)) {
			result.format = "context.getBeanFactory()";
		} else if (utils.implementsInterface(typeElement, Optional.class)) {
			result.format = "context.getBeanProvider($T.class)";
			if (paramType instanceof ParameterizedType) {
				ParameterizedType declaredType = (ParameterizedType) paramType;
				List<Type> args = Arrays.asList(declaredType.getActualTypeArguments());
				if (!args.isEmpty()) {
					Type type = args.iterator().next();
					TypeName value = TypeName.get(type);
					if (type instanceof ParameterizedType
							&& ((ParameterizedType) type).getActualTypeArguments().length > 0) {
						// The target type itself is generic.
						result.format = "context.getBeanProvider($T.forClassWithGenerics($T.class, $T.class))";
						result.types.add(SpringClassNames.RESOLVABLE_TYPE);
						if ("?".equals(value.toString())) {
							result.types.add(TypeName.OBJECT);
						} else {
							result.types.add(value);
						}
						type = ((ParameterizedType) type).getActualTypeArguments()[0];
						if (type instanceof ParameterizedType) {
							// So far we only support one level of generic parameters. Further levels could
							// be supported by adding calls to ResolvableType
							type = ((ParameterizedType) type).getRawType();
						}
					} else if (ResolvableType.forType(type).isArray()) {
						// TODO: something special with an array of generic types?
					}
					result.types.add(value);
				}
				result.format = "$T.ofNullable(" + result.format + ".getIfAvailable())";
				result.types.add(0, ClassName.get(Optional.class));
			}
		} else if (typeElement.isArray()) {
			// Really?
			result.format = "context.getBeanProvider($T.class).stream().collect($T.toList()).toArray(new $T[0])";
			result.types.add(TypeName.get(typeElement.getComponentType()));
			result.types.add(TypeName.get(Collectors.class));
			result.types.add(TypeName.get(typeElement.getComponentType()));

		} else if (paramType instanceof ParameterizedType && (utils.implementsInterface(typeElement, List.class)
				|| utils.implementsInterface(typeElement, Collection.class))) {
			ParameterizedType declaredType = (ParameterizedType) paramType;
			List<Type> args = Arrays.asList(declaredType.getActualTypeArguments());
			// TODO: make this work with more general collection elements types
			if (!args.isEmpty()) {
				Type type = args.iterator().next();
				TypeName value = TypeName.get(type);
				result.format = "context.getBeanProvider($T.class).stream().collect($T.toList())";
				if (type instanceof ParameterizedType
						&& ((ParameterizedType) type).getActualTypeArguments().length > 0) {
					result.format = "$T.generic(context.getBeanProvider($T.forClassWithGenerics($T.class, $T.class)).stream().collect($T.toList()))";
					result.types.add(SpringClassNames.OBJECT_UTILS);
					result.types.add(SpringClassNames.RESOLVABLE_TYPE);
					result.types.add(ClassName.get(((ParameterizedType) type).getRawType()));
					type = ((ParameterizedType) type).getActualTypeArguments()[0];
					value = TypeName.get((type));
					// The target type itself is generic. So far we only support one
					// level of generic parameters. Further levels could be supported
					// by adding calls to ResolvableType
					if ("?".equals(value.toString())) {
						result.types.add(TypeName.OBJECT);
					} else {
						result.types.add(value);
					}

				} else {
					result.types.add(value);
				}
				result.types.add(TypeName.get(Collectors.class));
			}
		} else if (utils.implementsInterface(typeElement, Map.class) && paramType instanceof ParameterizedType) {
			ParameterizedType declaredType = (ParameterizedType) paramType;
			List<Type> args = Arrays.asList(declaredType.getActualTypeArguments());
			// TODO: make this work with more general collection elements types
			if (args.size() > 1) {
				Type type = args.get(1);
				TypeName value = TypeName.get(type);
				result.format = "context.getBeansOfType($T.class)";
				result.types.add(value);
			}
		} else {
			StringBuilder code = new StringBuilder();
			String qualifier = utils.getQualifier(param);
			if (paramType instanceof ParameterizedType) {
				// We don't care any more
				paramType = ((ParameterizedType) paramType).getRawType();
			}
			if (utils.isLazy(param)) {
				code.append("$T.lazy($T.class, () -> ");
				result.types.add(SpringClassNames.OBJECT_UTILS);
				result.types.add(TypeName.get(paramType));
			}
			if (qualifier != null) {
				code.append("$T.qualifiedBeanOfType(context, $T.class, \"" + qualifier + "\")");
				result.types.add(SpringClassNames.BEAN_FACTORY_ANNOTATION_UTILS);
				result.types.add(TypeName.get(paramType));
			} else {
				code.append("context.getBean($T.class)");
				result.types.add(TypeName.get(paramType));
			}
			if (utils.isLazy(param)) {
				code.append(")");
			}
			result.format = code.toString();
		}
		return result;
	}

	private <T> Stream<T> getParameters(Executable method, Function<Parameter, T> mapper) {
		return Stream.of(method.getParameters()).map(mapper);
	}

	private List<Method> getBeanMethods(Class<?> type) {
		Set<String> seen = new HashSet<>();
		List<Method> beanMethods = new ArrayList<>();
		while (type != null) {
			for (Method candidate : type.getDeclaredMethods()) {
				if (isBeanMethod(candidate) && seen.add(candidate.getName())) {
					beanMethods.add(candidate);
				}
			}
			type = utils.getSuperType(type);
		}
		return beanMethods;
	}

	private Constructor<?> getConstructor(Class<?> type) {
		Set<String> seen = new HashSet<>();
		List<Constructor<?>> methods = new ArrayList<>();
		for (Constructor<?> candidate : type.getDeclaredConstructors()) {
			if (seen.add(candidate.toString())) {
				methods.add(candidate);
			}
		}
		// TODO: pick one that is explicitly autowired?
		if (methods.isEmpty()) {
			System.err.println("Wah: " + type);
		}
		return methods.get(0);
	}

	private boolean isBeanMethod(Method element) {
		int modifiers = element.getModifiers();
		if (!utils.hasAnnotation(element, SpringClassNames.BEAN.toString())) {
			return false;
		}
		if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
			return false;
		}
		return true;
	}

	static class ParameterSpec {

		private String format;

		private List<TypeName> types = new ArrayList<>();

	}

	static class ParameterSpecs {

		private String format;

		private Object[] args;

		public void addArg(Object arg) {
			Object[] args = new Object[this.args.length + 1];
			System.arraycopy(this.args, 0, args, 0, this.args.length);
			args[args.length - 1] = arg;
			this.args = args;
		}

	}

	@Override
	public String toString() {
		return "InitializerSpec:" + className.toString();
	}

	public ClassName getClassName() {
		return className;
	}

	@Override
	public int compareTo(InitializerSpec o) {
		return this.className.compareTo(o.getClassName());
	}

}
