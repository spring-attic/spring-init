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
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class InitializerSpec implements Comparable<InitializerSpec> {

	private static final Collection<String> MAYBE_BEANS = new HashSet<>(
			Arrays.asList("org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping",
					"org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter",
					"org.springframework.web.method.support.CompositeUriComponentsContributor",
					"org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter",
					"org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping",
					"org.springframework.boot.actuate.endpoint.web.reactive.WebFluxEndpointHandlerMapping",
					"org.springframework.boot.actuate.endpoint.web.reactive.ControllerEndpointHandlerMapping",
					"org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping",
					"org.springframework.boot.actuate.endpoint.web.servlet.ControllerEndpointHandlerMapping",
					"org.springframework.boot.actuate.endpoint.web.ServletEndpointRegistrar"));

	private static MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

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

	private boolean conditional;

	private boolean enabled = true;

	private com.squareup.javapoet.CodeBlock.Builder code;

	private Set<String> flags = new HashSet<>();

	public InitializerSpec(InitializerSpecs specs, ElementUtils utils, Class<?> type, Imports imports,
			Components components) {
		this.specs = specs;
		this.utils = utils;
		this.components = components;
		this.className = toInitializerNameFromConfigurationName(type);
		this.pkg = ClassName.get(type).packageName();
		type = imports.getImports().containsKey(type) && type.isAnnotation()
				? imports.getImports().get(type).iterator().next() : type;
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

	public boolean isEnabled() {
		return enabled;
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
		builder.addMethod(createInitializer(builder));
		if (this.code != null) {
			specs.addBuildTime(getClassName().toString());
			builder.addStaticBlock(code.build());
		}
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
			}
			else if (entry.getKey().toString().equals("name")) {
				String[] value = (String[]) entry.getValue();
				for (String type : value) {
					types.add(type);
				}
			}
		}
		if (types.isEmpty()) {
			return false;
		}
		builder.addField(TypeName.BOOLEAN, "enabled", Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL);
		if (this.code == null) {
			code = CodeBlock.builder();
		}
		code.add("enabled =\n");
		for (int i = 0; i < types.size(); i++) {
			String type = types.get(i);
			if (!ClassUtils.isPresent(type, null)) {
				this.enabled = false;
			}
			code.add("$T.isPresent($S, null)", SpringClassNames.CLASS_UTILS, type);
			if (i < types.size() - 1) {
				code.add(" &&\n");
			}
			else {
				code.add(";\n");
			}
		}
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

	private MethodSpec createInitializer(Builder spec) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder("initialize");
		builder.addAnnotation(Override.class);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addParameter(SpringClassNames.GENERIC_APPLICATION_CONTEXT, "context");
		addBeanMethods(spec, builder, configurationType);
		return builder.build();
	}

	private void addRegistrarInvokers(CodeBlock.Builder builder) {
		addImportInvokers(builder, configurationType);
		Annotation[] annotationMirrors = configurationType.getAnnotations();
		for (Annotation am : annotationMirrors) {
			// Looking up something like @EnableBar
			Class<?> element = am.annotationType();
			addImportInvokers(builder, element);
		}
	}

	private void addDeferredImport(CodeBlock.Builder builder, Class<?> element, Class<?> imported) {
		if (utils.hasAnnotation(imported, SpringClassNames.CONFIGURATION.toString()) && utils.isIncluded(imported)) {
			ClassName initializerName = InitializerSpec.toInitializerNameFromConfigurationName(imported);
			if (!ClassUtils.isPresent(initializerName.toString(), null)) {
				// Hack an initializer together (ideally we'd have full coverage in all
				// dependencies)
				if (!imported.getName().startsWith(pkg)) {
					logger.warn("Creating initializer on the fly for: " + imported.getName());
				}
				specs.addInitializer(imported);
			}
			builder.addStatement("registrars.defer(new $T())", initializerName);
		}
		else {
			registerBean(builder, imported);
		}
	}

	private void addImport(CodeBlock.Builder builder, Class<?> element, Class<?> imported) {
		if (utils.isConfigurationProperties(imported)) {
			List<Class<?>> types = utils.getTypesFromAnnotation(configurationType,
					SpringClassNames.ENABLE_CONFIGURATION_PROPERTIES.reflectionName(), "value");
			// TODO: Can we limit this to once only? Is it worth it (would eventually fail
			// e.g. in test slices)
			builder.addStatement("$T.register(context)",
					SpringClassNames.CONFIGURATION_PROPERTIES_BINDING_POST_PROCESSOR);
			for (Class<?> type : types) {
				builder.beginControlFlow("if (context.getBeanFactory().getBeanNamesForType($T.class).length==0)", type);
				builder.addStatement("context.registerBean($T.class, () -> new $T())", type, type);
				builder.endControlFlow();
			}
		}
		else if (utils.isImportWithNoMetadata(imported)) {
			builder.addStatement(
					"$T.invokeAwareMethods(new $T(), context.getEnvironment(), context, context).registerBeanDefinitions(null, context)",
					SpringClassNames.INFRASTRUCTURE_UTILS, imported);
		}
		else if (utils.isImportSelector(imported)) {
			addImportSelector(builder, imported);
		}
		else if (utils.isAutoConfigurationPackages(imported)) {
			// TODO: extract base packages from configurationType
			builder.addStatement("$T.register(context, $S)", SpringClassNames.AUTOCONFIGURATION_PACKAGES, pkg);
		}
		else if (utils.isImportBeanDefinitionRegistrar(imported)) {
			boolean accessible = isAccessible(imported);
			builder.beginControlFlow("try");
			if (accessible) {
				builder.addStatement(
						"$T.invokeAwareMethods(new $T(), context.getEnvironment(), context, context).registerBeanDefinitions($T.getBean(context.getBeanFactory(), $T.class).getMetadataReader($S).getAnnotationMetadata(), context, $T.getBean(context.getBeanFactory(), $T.class))",
						SpringClassNames.INFRASTRUCTURE_UTILS, imported, SpringClassNames.INFRASTRUCTURE_UTILS,
						SpringClassNames.METADATA_READER_FACTORY, configurationType.getName(),
						SpringClassNames.INFRASTRUCTURE_UTILS, SpringClassNames.BEAN_NAME_GENERATOR);
			}
			else {
				builder.addStatement(
						"(($T)$T.getOrCreate(context, $S)).registerBeanDefinitions($T.getBean(context.getBeanFactory(), $T.class).getMetadataReader($S).getAnnotationMetadata(), context, $T.getBean(context.getBeanFactory(), $T.class))",
						SpringClassNames.IMPORT_BEAN_DEFINITION_REGISTRAR, SpringClassNames.INFRASTRUCTURE_UTILS,
						imported.getName().replace("$", "."), SpringClassNames.INFRASTRUCTURE_UTILS,
						SpringClassNames.METADATA_READER_FACTORY, configurationType.getName(),
						SpringClassNames.INFRASTRUCTURE_UTILS, SpringClassNames.BEAN_NAME_GENERATOR);
			}
			builder.nextControlFlow("catch ($T e)", IOException.class)
					.addStatement(" throw new IllegalStateException(e)").endControlFlow();
		}
		else if (utils.hasAnnotation(imported, SpringClassNames.CONFIGURATION.toString())
				&& utils.isIncluded(imported)) {
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
		}
		else {
			registerBean(builder, imported);
		}
	}

	private void addImportSelector(CodeBlock.Builder builder, Class<?> imported) {
		if (InitializerApplication.closedWorld) {

			AnnotationMetadata metadata;
			try {
				metadata = metadataReaderFactory.getMetadataReader(configurationType.getName()).getAnnotationMetadata();
			}
			catch (IOException e) {
				throw new IllegalStateException("Cannot retrieve metadata for " + imported.getName(), e);
			}
			ImportSelector selector = utils.getImportSelector(imported);
			if (utils.isDeferredImportSelector(imported)) {
				if (!isAutoConfiguration(configurationType, imported.getName())) {
					registerImport(builder, imported);
					return;
				}
				builder.beginControlFlow(
						"if (context.getEnvironment().getProperty($T.ENABLED_OVERRIDE_PROPERTY, Boolean.class, true))",
						SpringClassNames.ENABLE_AUTO_CONFIGURATION);
				builder.addStatement("$T registrars = $T.getBean(context.getBeanFactory(), $T.class)",
						SpringClassNames.IMPORT_REGISTRARS, SpringClassNames.INFRASTRUCTURE_UTILS,
						SpringClassNames.IMPORT_REGISTRARS);
				List<Class<?>> types = new ArrayList<>();
				for (String selected : selector.selectImports(metadata)) {
					if (ClassUtils.isPresent(selected, null)) {
						types.add(ClassUtils.resolveClassName(selected, null));
					}
				}
				// TODO: check it is actually autoconfigs first
				for (Class<?> type : new DeferredConfigurations(types).list()) {
					addDeferredImport(builder, imported, type);
				}
				builder.endControlFlow();
			}
			else {
				for (String selected : selector.selectImports(metadata)) {
					if (ClassUtils.isPresent(selected, null)) {
						addImport(builder, imported, ClassUtils.resolveClassName(selected, null));
					}
				}
			}

		}
		else {
			registerImport(builder, imported);
		}
	}

	private void registerImport(CodeBlock.Builder builder, Class<?> imported) {
		if (isAccessible(imported)) {
			builder.addStatement("$T.getBean(context.getBeanFactory(), $T.class).add($T.class, $T.class)",
					SpringClassNames.INFRASTRUCTURE_UTILS, SpringClassNames.IMPORT_REGISTRARS, configurationType,
					imported);
		}
		else {
			builder.addStatement("$T.getBean(context.getBeanFactory(), $T.class).add($T.class, types.getType($S))",
					SpringClassNames.INFRASTRUCTURE_UTILS, SpringClassNames.IMPORT_REGISTRARS, configurationType,
					imported.getCanonicalName());
		}
	}

	private boolean isAutoConfiguration(Class<?> importer, String typeName) {
		// TODO: maybe work out a better way to detect auto configs
		return typeName.endsWith("AutoConfigurationImportSelector")
				|| typeName.endsWith("ManagementContextConfigurationImportSelector");
	}

	private boolean isAccessible(Class<?> imported) {
		try {
			if (ClassUtils.getPackageName(imported).equals(pkg)) {
				return true;
			}
			if (!java.lang.reflect.Modifier.isPublic(imported.getModifiers())) {
				return false;
			}
			for (Constructor<?> constructor : imported.getConstructors()) {
				if (java.lang.reflect.Modifier.isPublic(constructor.getModifiers())) {
					return true;
				}
			}
		}
		catch (Exception e) {
			// ignore
		}
		return false;
	}

	private void addImportInvokers(CodeBlock.Builder builder, Class<?> element) {
		Set<Class<?>> registrarInitializers = imports.getImports().get(element);
		if (registrarInitializers != null) {
			for (Class<?> imported : registrarInitializers) {
				addImport(builder, element, imported);
			}
		}
	}

	private void addBeanMethods(Builder spec, MethodSpec.Builder builder, Class<?> type) {
		if (!utils.isIncluded(type)) {
			return;
		}
		conditional |= utils.hasAnnotation(type, SpringClassNames.CONDITIONAL.toString());
		CodeBlock.Builder code = CodeBlock.builder();
		addScannedComponents(code);
		ClassName factory;
		try {
			List<Method> beanMethods = getBeanMethods(type);
			factory = getFactoryType(spec, type, beanMethods);
			addNewBeanForConfig(code, type, factory);
			for (Method method : beanMethods) {
				createBeanMethod(spec, code, method, type, java.lang.reflect.Modifier.isStatic(method.getModifiers())
						? ClassName.get(method.getDeclaringClass()) : factory);
			}
		}
		catch (Throwable e) {
			logger.info("Cannot reflect on: " + type.getName());
			return;
		}
		if (!this.enabled) {
			// It's a no-op
			return;
		}
		if (this.hasEnabled) {
			builder.beginControlFlow("if ($T.enabled)", this.className);
		}
		addResources(spec, code);
		addRegistrarInvokers(code);
		if (conditional) {
			builder.addStatement("$T conditions = $T.getBean(context.getBeanFactory(), $T.class)",
					SpringClassNames.CONDITION_SERVICE, SpringClassNames.INFRASTRUCTURE_UTILS,
					SpringClassNames.CONDITION_SERVICE);
			builder.beginControlFlow("if (conditions.matches($T.class))", type);
		}
		builder.beginControlFlow("if (context.getBeanFactory().getBeanNamesForType($T.class).length==0)", factory);
		CodeBlock logic = code.build();
		if (logic.toString().contains("types.")) {
			builder.addStatement("$T types = $T.getBean(context.getBeanFactory(), $T.class)",
					SpringClassNames.TYPE_SERVICE, SpringClassNames.INFRASTRUCTURE_UTILS,
					SpringClassNames.TYPE_SERVICE);
		}
		builder.addCode(logic);
		builder.endControlFlow();
		if (conditional) {
			builder.endControlFlow();
		}
		if (this.hasEnabled) {
			builder.endControlFlow();
		}
	}

	private ClassName getFactoryType(Builder spec, Class<?> type, List<Method> beanMethods) {
		if (beanMethods.isEmpty() || !utils.isProxyBeanMethods(type)) {
			return ClassName.get(type);
		}
		TypeSpec subclass = subclass(type, beanMethods);
		spec.addType(subclass);
		return ClassName.get("", subclass.name);
	}

	private TypeSpec subclass(Class<?> type, List<Method> beanMethods) {
		Builder subclass = TypeSpec.classBuilder(type.getSimpleName().replace("$", "_") + "Cached").superclass(type);
		subclass.addModifiers(Modifier.STATIC);
		subclass.addField(methodCache());
		Constructor<?> constructor = getConstructor(type);
		if (constructor.getParameterTypes().length > 0) {
			subclass.addMethod(constructor(constructor));
		}
		for (Method method : beanMethods) {
			if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
				// no overrides for static methods
				continue;
			}
			if (java.lang.reflect.Modifier.isPrivate(method.getReturnType().getModifiers())) {
				// no overrides for private types
				continue;
			}
			subclass.addMethod(methodSpec(method));
		}
		return subclass.build();
	}

	private MethodSpec constructor(Constructor<?> method) {
		MethodSpec.Builder spec = MethodSpec.constructorBuilder();
		spec.addModifiers(Modifier.PUBLIC);
		ParameterSpec params = new ParameterSpec();
		for (Parameter param : method.getParameters()) {
			Type type = param.getParameterizedType();
			Type rawType = rawType(param.getType(), type);
			params.add(param.getName(), rawType);
			spec.addParameter(rawType, param.getName());
		}
		spec.addStatement("super(" + params.format() + ")");
		return spec.build();
	}

	private MethodSpec methodSpec(Method method) {
		MethodSpec.Builder spec = MethodSpec.methodBuilder(method.getName());
		spec.addAnnotation(Override.class);
		spec.addAnnotation(SpringClassNames.BEAN);
		Type returnType = method.getGenericReturnType();
		if (returnType instanceof ParameterizedType) {
			spec.addAnnotation(
					AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build());
		}
		if (returnType instanceof TypeVariable) {
			returnType = method.getReturnType();
		}
		spec.addModifiers(Modifier.PUBLIC);
		spec.returns(rawType(utils.getReturnType(method), returnType));
		ParameterSpec params = new ParameterSpec();
		for (Parameter param : method.getParameters()) {
			Type type = param.getParameterizedType();
			Type rawType = rawType(param.getType(), type);
			params.add(param.getName(), rawType);
			spec.addParameter(rawType, param.getName());
		}
		String key = key(method);
		spec.beginControlFlow("if (!METHODS.containsKey($S))", key);
		spec.addStatement("METHODS.put($S, super.$L(" + params.format() + "))", key(method), method.getName());
		spec.endControlFlow();
		spec.addStatement("return ($T) METHODS.get($S)", returnType, key(method));
		return spec.build();
	}

	private Type rawType(Class<?> rawType, Type type) {
		Type result = type;
		if (type instanceof ParameterizedType) {
			Type[] types = ((ParameterizedType) type).getActualTypeArguments();
			if (Stream.of(types).anyMatch(t -> t instanceof TypeVariable)) {
				// We don't care any more. Sigh. Spring Session.
				result = rawType;
			}
		}
		else if (type instanceof TypeVariable) {
			result = rawType;
		}
		return result;
	}

	private FieldSpec methodCache() {
		FieldSpec.Builder builder = FieldSpec.builder(new ParameterizedTypeReference<Map<String, Object>>() {
		}.getType(), "METHODS", Modifier.PRIVATE);
		builder.initializer("new $T<>()", ConcurrentHashMap.class);
		return builder.build();
	}

	private void addResources(Builder type, CodeBlock.Builder builder) {
		Set<String> locations = resources.getResources().get(configurationType);
		if (locations != null) {
			if (this.code == null) {
				this.code = CodeBlock.builder();
			}
			this.code.addStatement("xmlEnabled = !$S.equals($T.getProperty($S, $S))", "true", System.class,
					"spring.xml.ignore", "false");
			type.addField(TypeName.BOOLEAN, "xmlEnabled", Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL);
			builder.beginControlFlow("if (xmlEnabled)");
			for (String location : locations) {
				builder.addStatement("new $T($S).initialize(context)", SpringClassNames.XML_INITIALIZER, location);
			}
			builder.endControlFlow();
		}
	}

	private void addScannedComponents(CodeBlock.Builder builder) {
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
						conditional = true;
						includes(builder, imported);
					}
					if (utils.hasAnnotation(imported, SpringClassNames.CONFIGURATION.toString())
							&& utils.isIncluded(imported)) {
						builder.addStatement("new $T().initialize(context)",
								InitializerSpec.toInitializerNameFromConfigurationName(imported));
					}
					else {
						registerBean(builder, imported);
					}
					if (filtered) {
						builder.endControlFlow();
					}
				}
			}
		}
	}

	private void registerBean(CodeBlock.Builder builder, Class<?> imported) {
		if (utils.isIncluded(imported)) {
			boolean conditional = false;
			if (utils.hasAnnotation(imported, SpringClassNames.CONDITIONAL.toString())) {
				conditional = true;
			}
			if (isAccessible(imported)) {
				if (conditional) {
					builder.beginControlFlow("if (conditions.matches($T.class))", imported);
				}
				Constructor<?> constructor = getConstructor(imported);
				ParameterSpecs params = autowireParamsForMethod(constructor);
				builder.addStatement("context.registerBean($T.class, () -> new $T(" + params.format + "))",
						ArrayUtils.merge(imported, imported, params.args));
			}
			else {
				if (conditional) {
					builder.beginControlFlow("if (conditions.matches(types.getType($S)))", imported.getName());
				}
				builder.addStatement("context.registerBean(types.getType($S))", imported.getName());
			}
			if (conditional) {
				builder.endControlFlow();
			}
			this.conditional |= conditional;
		}
	}

	private void includes(CodeBlock.Builder builder, Class<?> imported) {
		if (isAccessible(imported)) {
			builder.beginControlFlow("if (conditions.includes($T.class))", imported);
		}
		else {
			builder.beginControlFlow("if (conditions.includes(types.getType($S)))", imported.getName());
		}
	}

	private void addNewBeanForConfig(CodeBlock.Builder code, Class<?> base, ClassName type) {
		Constructor<?> constructor = getConstructor(base);
		ParameterSpecs params = autowireParamsForMethod(constructor);
		code.addStatement("context.registerBean($T.class, () -> new $T(" + params.format + "))",
				ArrayUtils.merge(type, type, params.args));
	}

	private void createBeanMethod(Builder spec, CodeBlock.Builder builder, Method beanMethod, Class<?> type,
			ClassName factory) {
		// TODO will need to handle bean methods in private configs
		try {
			Class<?> returnTypeElement = utils.getReturnType(beanMethod);
			boolean maybe = MAYBE_BEANS.contains(beanMethod.getReturnType().getName());
			boolean conditional = utils.hasAnnotation(beanMethod, SpringClassNames.CONDITIONAL.toString());

			if (maybe) {
				addStaticFlag(spec, "requestMappingEnabled", "spring.native.remove-request-mapping-support");
				builder.beginControlFlow("if (requestMappingEnabled)");
			}
			if (conditional) {
				builder.beginControlFlow("if (conditions.matches($T.class, $T.class))", type, returnTypeElement);
			}
			ParameterSpecs params = autowireParamsForMethod(beanMethod);

			String beanName = utils.getBeanName(beanMethod);
			ResolvableType resolvable = ResolvableType.forMethodReturnType(beanMethod);
			if (resolvable.hasGenerics()) {
				builder.addStatement("context.registerBeanDefinition($S, $T.generic(new $T<$T>() {}, "
						+ supplier(beanMethod, params.format) + customizer(type, factory, beanMethod, params) + "))",
						ArrayUtils.merge(params.args, beanName, SpringClassNames.BEAN_FACTORY_UTILS,
								SpringClassNames.PARAMETERIZED_TYPE_REFERENCE, resolvable.getType(), factory));
			}
			else {
				builder.addStatement(
						"context.registerBean($S, $T.class, " + supplier(beanMethod, params.format)
								+ customizer(type, factory, beanMethod, params) + ")",
						ArrayUtils.merge(params.args, beanName, returnTypeElement, factory));
			}

			if (conditional) {
				builder.endControlFlow();
			}
			if (maybe) {
				builder.endControlFlow();
			}

			this.conditional |= conditional;
		}
		catch (Throwable t) {
			throw new RuntimeException(
					"Problem performing createBeanMethod for method " + type.toString() + "." + beanMethod.toString(),
					t);
		}
	}

	private void addStaticFlag(Builder spec, String flagName, String propertyName) {
		if (this.code == null) {
			this.code = CodeBlock.builder();
		}
		if (this.flags.contains(flagName)) {
			return;
		}
		this.flags.add(flagName);
		spec.addField(TypeName.BOOLEAN, flagName, Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL);
		this.code.addStatement(flagName + " = $S.equals($T.getProperty($S, $S))", "false", System.class, propertyName,
				"false");
	}

	private String customizer(Class<?> type, ClassName factory, Method beanMethod, ParameterSpecs params) {
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
			params.addArg(factory);
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

	private String supplier(Method beanMethod, String parameterVariables) {
		boolean exception = utils.throwsCheckedException(beanMethod);
		String code;
		if (!java.lang.reflect.Modifier.isStatic(beanMethod.getModifiers())) {
			code = "context.getBean($T.class)." + beanMethod.getName() + "(" + parameterVariables + ")";
		}
		else {
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
						result.types.add(SpringClassNames.BEAN_FACTORY_UTILS);
						Type[] iterator = ((ParameterizedType) value).getActualTypeArguments();
						value = iterator[1];
						result.types.add(TypeName.get(value));
					}
					else if (value instanceof ParameterizedType
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
							}
							else {
								result.types.add(v);
							}
						});
					}
					else if (value instanceof Class && ((Class<?>) value).isArray()) {
						result.format = "$T.array(context, $T.class)";
						result.types.add(SpringClassNames.BEAN_FACTORY_UTILS);
						value = ((Class<?>) value).getComponentType();
						result.types.add(TypeName.get(value));
					}
					else {
						result.format = "context.getBeanProvider($T.class)";
						result.types.add(TypeName.get(value));
					}
				}
			}
		}
		else if (utils.implementsInterface(typeElement, ApplicationContext.class)
				|| utils.implementsInterface(typeElement, ResourceLoader.class)
				|| utils.implementsInterface(typeElement, ApplicationEventPublisher.class)
				|| paramTypename.equals(SpringClassNames.CONFIGURABLE_APPLICATION_CONTEXT.toString())) {
			if (utils.implementsInterface(typeElement, SpringClassNames.WEB_APPLICATION_CONTEXT)) {
				result.format = "($T)context";
				result.types.add(ClassName.get(typeElement));
			}
			else {
				result.format = "context";
			}
		}
		else if (utils.implementsInterface(typeElement, BeanFactory.class)) {
			result.format = "context.getBeanFactory()";
		}
		else if (utils.implementsInterface(typeElement, Optional.class)) {
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
						}
						else {
							result.types.add(value);
						}
						type = ((ParameterizedType) type).getActualTypeArguments()[0];
						if (type instanceof ParameterizedType) {
							// So far we only support one level of generic parameters.
							// Further levels could
							// be supported by adding calls to ResolvableType
							type = ((ParameterizedType) type).getRawType();
						}
					}
					else if (ResolvableType.forType(type).isArray()) {
						// TODO: something special with an array of generic types?
					}
					result.types.add(value);
				}
				result.format = "$T.ofNullable(" + result.format + ".getIfAvailable())";
				result.types.add(0, ClassName.get(Optional.class));
			}
		}
		else if (typeElement.isArray()) {
			// Really?
			result.format = "context.getBeanProvider($T.class).stream().collect($T.toList()).toArray(new $T[0])";
			result.types.add(TypeName.get(typeElement.getComponentType()));
			result.types.add(TypeName.get(Collectors.class));
			result.types.add(TypeName.get(typeElement.getComponentType()));

		}
		else if (paramType instanceof ParameterizedType && (utils.implementsInterface(typeElement, List.class)
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
					result.types.add(SpringClassNames.BEAN_FACTORY_UTILS);
					result.types.add(SpringClassNames.RESOLVABLE_TYPE);
					result.types.add(ClassName.get(((ParameterizedType) type).getRawType()));
					type = ((ParameterizedType) type).getActualTypeArguments()[0];
					value = TypeName.get((type));
					// The target type itself is generic. So far we only support one
					// level of generic parameters. Further levels could be supported
					// by adding calls to ResolvableType
					if ("?".equals(value.toString())) {
						result.types.add(TypeName.OBJECT);
					}
					else {
						result.types.add(value);
					}

				}
				else {
					result.types.add(value);
				}
				result.types.add(TypeName.get(Collectors.class));
			}
		}
		else if (utils.implementsInterface(typeElement, Map.class) && paramType instanceof ParameterizedType) {
			ParameterizedType declaredType = (ParameterizedType) paramType;
			List<Type> args = Arrays.asList(declaredType.getActualTypeArguments());
			// TODO: make this work with more general collection elements types
			if (args.size() > 1) {
				Type type = args.get(1);
				TypeName value = TypeName.get(type);
				result.format = "context.getBeansOfType($T.class)";
				result.types.add(value);
			}
		}
		else {
			StringBuilder code = new StringBuilder();
			String qualifier = utils.getQualifier(param);
			Type rawType = paramType;
			ResolvableType resolvable = null;
			if (paramType instanceof ParameterizedType) {
				rawType = ((ParameterizedType) paramType).getRawType();
				Type[] types = ((ParameterizedType) paramType).getActualTypeArguments();
				if (Stream.of(types).noneMatch(type -> type instanceof TypeVariable)) {
					resolvable = ResolvableType.forType(paramType);
				}
				else {
					// We don't care any more. Sigh. Spring Session.
				}
			}
			if (utils.isLazy(param)) {
				code.append("$T.lazy($T.class, () -> ");
				result.types.add(SpringClassNames.BEAN_FACTORY_UTILS);
				result.types.add(TypeName.get(rawType));
			}
			if (qualifier != null) {
				if (resolvable == null) {
					code.append("$T.qualifiedBeanOfType(context, $T.class, \"" + qualifier + "\")");
					result.types.add(SpringClassNames.BEAN_FACTORY_ANNOTATION_UTILS);
					result.types.add(TypeName.get(rawType));
				}
				else {
					code.append("$T.available(context, $T.forType(new $T<$T>(){}), \"" + qualifier + "\")");
					result.types.add(SpringClassNames.BEAN_FACTORY_UTILS);
					result.types.add(SpringClassNames.RESOLVABLE_TYPE);
					result.types.add(SpringClassNames.PARAMETERIZED_TYPE_REFERENCE);
					result.types.add(TypeName.get(paramType));
				}
			}
			else {
				if (resolvable == null) {
					code.append("context.getBean($T.class)");
					result.types.add(TypeName.get(rawType));
				}
				else {
					code.append("$T.available(context, $T.forType(new $T<$T>(){}))");
					result.types.add(SpringClassNames.BEAN_FACTORY_UTILS);
					result.types.add(SpringClassNames.RESOLVABLE_TYPE);
					result.types.add(SpringClassNames.PARAMETERIZED_TYPE_REFERENCE);
					result.types.add(TypeName.get(paramType));
				}
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
				if (isBeanMethod(candidate) && !candidate.isBridge() && seen.add(key(candidate))) {
					beanMethods.add(candidate);
				}
			}
			// Ensure we include all inherited methods
			type = utils.getSuperType(type);
		}
		return beanMethods;
	}

	private String key(Method method) {
		return method.getName() + "(" + Arrays.asList(method.getParameterTypes()).stream().map(type -> type.getName())
				.collect(Collectors.joining(",")) + ")";
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

		private StringBuilder builder = new StringBuilder();

		private List<TypeName> types = new ArrayList<>();

		public void add(String added, Type... types) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(added);
			for (Type type : types) {
				this.types.add(TypeName.get(type));
			}
		}

		public String format() {
			return builder.toString();
		}

		public Object[] prepend(Object... objects) {
			Object[] result = new Object[types.size() + objects.length];
			for (int i = 0; i < objects.length; i++) {
				result[i] = objects[i];
			}
			for (int i = 0; i < types.size(); i++) {
				result[objects.length + i] = types.get(i);
			}
			return result;
		}

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

	static class DeferredConfigurations extends AutoConfigurations {

		protected DeferredConfigurations(Collection<Class<?>> classes) {
			super(classes);
		}

		public Class<?>[] list() {
			return getClasses().toArray(new Class<?>[0]);
		}

	}

}
