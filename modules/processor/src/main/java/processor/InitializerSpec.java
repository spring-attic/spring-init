/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

/**
 * @author Dave Syer
 *
 */
public class InitializerSpec implements Comparable<InitializerSpec> {

	private TypeSpec initializer;
	private String pkg;
	private TypeElement configurationType;
	private ElementUtils utils;
	private ClassName className;
	private Imports imports;
	private Resources resources = new Resources();
	private Components components;

	public InitializerSpec(ElementUtils utils, TypeElement type, Imports imports,
			Components components) {
		this.utils = utils;
		this.components = components;
		this.className = toInitializerNameFromConfigurationName(type);
		this.pkg = ClassName.get(type).packageName();
		type = imports.getImports().containsKey(type)
				&& type.getKind() == ElementKind.ANNOTATION_TYPE
						? imports.getImports().get(type).iterator().next()
						: type;
		this.configurationType = type;
		this.imports = imports;
		for (TypeElement imported : utils.getTypesFromAnnotation(type,
				SpringClassNames.IMPORT.toString(), "value")) {
			imports.addImport(type, imported);
		}
		for (String imported : utils.getStringsFromAnnotation(type,
				SpringClassNames.IMPORT_RESOURCE.toString(), "value")) {
			resources.addResource(type, imported);
		}
	}

	public TypeElement getConfigurationType() {
		return configurationType;
	}

	public void setConfigurationType(TypeElement configurationType) {
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

	private TypeSpec createInitializer(TypeElement type) {
		Builder builder = TypeSpec.classBuilder(getClassName());
		builder.addSuperinterface(SpringClassNames.INITIALIZER_TYPE);
		builder.addModifiers(Modifier.PUBLIC);
		builder.addMethod(createInitializer());
		return builder.build();
	}

	public static ClassName toInitializerNameFromConfigurationName(TypeElement type) {
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
		List<? extends AnnotationMirror> annotationMirrors = configurationType
				.getAnnotationMirrors();
		for (AnnotationMirror am : annotationMirrors) {
			// Looking up something like @EnableBar
			TypeElement element = (TypeElement) am.getAnnotationType().asElement();
			addImportInvokers(builder, element);
		}
	}

	private void addImportInvokers(MethodSpec.Builder builder, TypeElement element) {
		Set<TypeElement> registrarInitializers = imports.getImports().get(element);
		if (registrarInitializers != null) {
			for (TypeElement imported : registrarInitializers) {
				if (utils.isImporter(imported)) {
					builder.addStatement(
							"context.getBeanFactory().getBean($T.class).add($T.class, \"$L\")",
							SpringClassNames.IMPORT_REGISTRARS, configurationType,
							imported.getQualifiedName());
				}
				else if (utils.getPackage(imported).equals(pkg)
						|| components.getAll().contains(imported)) {
					builder.addStatement("new $T().initialize(context)", InitializerSpec
							.toInitializerNameFromConfigurationName(imported));
				}
				else {
					builder.addStatement(
							"context.getBeanFactory().getBean($T.class).add($T.class, \"$L\")",
							SpringClassNames.IMPORT_REGISTRARS, configurationType,
							imported.getQualifiedName());
				}
			}
		}
	}

	private void addBeanMethods(MethodSpec.Builder builder, TypeElement type) {
		boolean conditional = utils.hasAnnotation(type,
				SpringClassNames.CONDITIONAL.toString());
		if (conditional) {
			builder.addStatement(
					"$T conditions = context.getBeanFactory().getBean($T.class)",
					SpringClassNames.CONDITION_SERVICE,
					SpringClassNames.CONDITION_SERVICE);
			builder.beginControlFlow("if (conditions.matches($T.class))", type);
		}
		builder.beginControlFlow(
				"if (context.getBeanFactory().getBeanNamesForType($T.class).length==0)",
				type);
		addResources(builder);
		addRegistrarInvokers(builder);
		addScannedComponents(builder);
		addNewBeanForConfig(builder, type);
		boolean conditionsAvailable = conditional;
		for (ExecutableElement method : getBeanMethods(type)) {
			conditionsAvailable |= createBeanMethod(builder, method, type,
					conditionsAvailable);
		}
		builder.endControlFlow();
		if (conditional) {
			builder.endControlFlow();
		}
	}

	private void addResources(MethodSpec.Builder builder) {
		Set<String> locations = resources.getResources().get(configurationType);
		if (locations != null) {
			for (String location : locations) {
				builder.addStatement(
						"context.getBeanFactory().getBean($T.class).add($T.class, \"$L\")",
						SpringClassNames.IMPORT_REGISTRARS, configurationType, location);
			}
		}
	}

	private void addScannedComponents(MethodSpec.Builder builder) {
		Set<TypeElement> set = components.getComponents().get(configurationType);
		if (set != null) {
			for (TypeElement imported : set) {
				if (!imported.equals(configurationType)) {
					if (utils.hasAnnotation(imported,
							SpringClassNames.CONFIGURATION.toString())) {
						builder.addStatement("new $T().initialize(context)",
								InitializerSpec.toInitializerNameFromConfigurationName(
										imported));
					}
					else {
						if (imported.getModifiers().contains(Modifier.PUBLIC)) {
							ExecutableElement constructor = getConstructor(imported);
							Parameters params = autowireParamsForMethod(constructor);
							builder.addStatement(
									"context.registerBean($T.class, () -> new $T("
											+ params.format + "))",
									ArrayUtils.merge(imported, imported, params.args));
						}
						else {
							builder.addStatement(
									"context.registerBean($T.resolveClassName(\"$L\", context.getClassLoader()))",
									SpringClassNames.CLASS_UTILS,
									imported.getQualifiedName());

						}
					}
				}
			}
		}
	}

	private void addNewBeanForConfig(MethodSpec.Builder builder, TypeElement type) {
		ExecutableElement constructor = getConstructor(type);
		Parameters params = autowireParamsForMethod(constructor);
		builder.addStatement(
				"context.registerBean($T.class, () -> new $T(" + params.format + "))",
				ArrayUtils.merge(type, type, params.args));
	}

	private boolean createBeanMethod(MethodSpec.Builder builder,
			ExecutableElement beanMethod, TypeElement type, boolean conditionsAvailable) {
		// TODO will need to handle bean methods in private configs
		try {
			TypeMirror returnType = utils.getReturnType(beanMethod);

			Element returnTypeElement = utils.asElement(returnType);
			boolean conditional = utils.hasAnnotation(beanMethod,
					SpringClassNames.CONDITIONAL.toString());
			if (conditional) {
				if (!conditionsAvailable) {
					builder.addStatement(
							"$T conditions = context.getBeanFactory().getBean($T.class)",
							SpringClassNames.CONDITION_SERVICE,
							SpringClassNames.CONDITION_SERVICE);
				}
			}

			if (returnTypeElement.getModifiers().contains(Modifier.PRIVATE)) {

				if (conditional) {
					builder.beginControlFlow(
							"if (conditions.matches($T.class, $T.resolveClassName(\"$L\", context.getClassLoader())))",
							type, SpringClassNames.CLASS_UTILS,
							utils.erasure(returnType));
				}
				utils.printMessage(Kind.WARNING,
						"Generating source for bean method, type involved is private: "
								+ beanMethod.getEnclosingElement() + "." + beanMethod);
				builder.addStatement(
						"context.registerBean($T.resolveClassName(\"$L\", context.getClassLoader()))",
						SpringClassNames.CLASS_UTILS,
						((TypeElement) returnTypeElement).getQualifiedName());

			}
			else {

				if (conditional) {
					builder.beginControlFlow(
							"if (conditions.matches($T.class, $T.class))", type,
							utils.erasure(returnType));
				}
				Parameters params = autowireParamsForMethod(beanMethod);

				builder.addStatement(
						"context.registerBean(" + "\"" + beanMethod.getSimpleName()
								+ "\", $T.class, "
								+ supplier(type, beanMethod, params.format)
								+ customizer(type, beanMethod, params) + ")",
						ArrayUtils.merge(utils.erasure(returnType), type, params.args));
			}

			if (conditional) {
				builder.endControlFlow();
			}

			return conditional;
		}
		catch (Throwable t) {
			throw new RuntimeException("Problem performing createBeanMethod for method "
					+ type.toString() + "." + beanMethod.toString(), t);
		}
	}

	private String customizer(TypeElement type, ExecutableElement beanMethod,
			Parameters params) {
		StringBuilder builder = new StringBuilder(", ");
		boolean hasInit = false;
		StringBuilder body = new StringBuilder();
		if (utils.hasAnnotation(beanMethod, SpringClassNames.BEAN.toString())) {
			String methodName = utils.getStringFromAnnotation(beanMethod,
					SpringClassNames.BEAN.toString(), "initMethod");
			if (methodName != null && methodName.length() > 0) {
				body.append("def.setInitMethodName(\"" + methodName + "\")");
			}
			methodName = utils.getStringFromAnnotation(beanMethod,
					SpringClassNames.BEAN.toString(), "destroyMethod");
			if (methodName != null && methodName.length() > 0) {
				hasInit = body.length() > 0;
				if (hasInit && body.indexOf("{") != 0) {
					body.insert(0, "{");
					body.append("; ");
				}
				body.append("def.setDestroyMethodName(\"" + methodName + "\")");
			}
		}
		if (utils.hasAnnotation(beanMethod,
				SpringClassNames.CONFIGURATION_PROPERTIES.toString())
				|| utils.implementsInterface(
						(TypeElement) utils.asElement(beanMethod.getReturnType()),
						SpringClassNames.FACTORY_BEAN)) {
			String methodName = beanMethod.getSimpleName().toString();
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

	private Parameters autowireParamsForMethod(ExecutableElement method) {
		List<Parameter> parameterTypes = getParameters(method, this::parameterAccessor)
				.collect(Collectors.toList());

		String format = parameterTypes.stream().map(param -> param.format)
				.collect(Collectors.joining(","));
		Object[] args = parameterTypes.stream().flatMap(param -> param.types.stream())
				.collect(Collectors.toList()).toArray();

		Parameters params = new Parameters();
		params.format = format;
		params.args = args;
		return params;
	}

	private String supplier(TypeElement owner, ExecutableElement beanMethod,
			String parameterVariables) {
		boolean exception = utils.throwsCheckedException(beanMethod);
		String code = "context.getBean($T.class)." + beanMethod.getSimpleName() + "("
				+ parameterVariables + ")";
		if (exception) {
			return "() -> { try { return " + code
					+ "; } catch (Exception e) { throw new IllegalStateException(e); } }";
		}
		return "() -> " + code;
	}

	private Parameter parameterAccessor(VariableElement param) {
		Parameter result = new Parameter();
		TypeMirror paramType = param.asType();
		String paramTypename = utils.getParameterType(param);
		TypeElement typeElement = (TypeElement) utils.asElement(paramType);
		if (utils.implementsInterface(typeElement, SpringClassNames.OBJECT_PROVIDER)) {
			if (paramType instanceof DeclaredType) {
				DeclaredType declaredType = (DeclaredType) paramType;
				List<? extends TypeMirror> args = declaredType.getTypeArguments();
				if (!args.isEmpty()) {
					TypeMirror type = args.iterator().next();
					TypeName value = TypeName.get(utils.erasure(type));
					if (value.toString().equals(Map.class.getName())) {
						result.format = "$T.map(context, $T.class)";
						result.types.add(SpringClassNames.OBJECT_UTILS);
						Iterator<? extends TypeMirror> iterator = ((DeclaredType) type)
								.getTypeArguments().iterator();
						iterator.next();
						type = iterator.next();
						value = TypeName.get(utils.erasure(type));
						result.types.add(value);
					}
					else if (type instanceof DeclaredType
							&& !((DeclaredType) type).getTypeArguments().isEmpty()) {
						result.format = "context.getBeanProvider($T.forClassWithGenerics($T.class, "
								+ ((DeclaredType) type).getTypeArguments().stream()
										.map(thing -> "$T.class")
										.collect(Collectors.joining(", "))
								+ "))";
						result.types.add(SpringClassNames.RESOLVABLE_TYPE);
						result.types.add(value);
						((DeclaredType) type).getTypeArguments().forEach(t -> {
							TypeName v = TypeName.get(utils.erasure(t));
							// The target type itself is generic. So far we only support
							// one
							// level of generic parameters. Further levels could be
							// supported
							// by adding calls to ResolvableType
							if ("?".equals(v.toString())) {
								result.types.add(TypeName.OBJECT);
							}
							else {
								result.types.add(v);
							}
						});
					}
					else if (type instanceof ArrayType) {
						result.format = "$T.array(context, $T.class)";
						result.types.add(SpringClassNames.OBJECT_UTILS);
						value = TypeName.get(((ArrayType) type).getComponentType());
						result.types.add(value);
					}
					else {
						result.format = "context.getBeanProvider($T.class)";
						result.types.add(value);
					}
				}
			}
		}
		else if (utils.implementsInterface(typeElement,
				SpringClassNames.APPLICATION_CONTEXT)
				|| paramTypename.equals(
						SpringClassNames.CONFIGURABLE_APPLICATION_CONTEXT.toString())) {
			if (utils.implementsInterface(typeElement,
					SpringClassNames.WEB_APPLICATION_CONTEXT)) {
				result.format = "($T)context";
				result.types.add(ClassName.get(typeElement));
			}
			else {
				result.format = "context";
			}
		}
		else if (utils.implementsInterface(typeElement, SpringClassNames.BEAN_FACTORY)) {
			result.format = "context.getBeanFactory()";
		}
		else if (utils.implementsInterface(typeElement, ClassName.get(Optional.class))) {
			result.format = "context.getBeanProvider($T.class)";
			if (paramType instanceof DeclaredType) {
				DeclaredType declaredType = (DeclaredType) paramType;
				List<? extends TypeMirror> args = declaredType.getTypeArguments();
				if (!args.isEmpty()) {
					TypeMirror type = args.iterator().next();
					TypeName value = TypeName.get(utils.erasure(type));
					if (type instanceof DeclaredType
							&& !((DeclaredType) type).getTypeArguments().isEmpty()) {
						// The target type itself is generic. So far we only support one
						// level of generic parameters. Further levels could be supported
						// by adding calls to ResolvableType
						result.format = "context.getBeanProvider($T.forClassWithGenerics($T.class, $T.class))";
						result.types.add(SpringClassNames.RESOLVABLE_TYPE);
						if ("?".equals(value.toString())) {
							result.types.add(TypeName.OBJECT);
						}
						else {
							result.types.add(value);
						}
						type = ((DeclaredType) type).getTypeArguments().iterator().next();
					}
					else if (type instanceof ArrayType) {
						// TODO: something special with an array of generic types?
					}
					result.types.add(value);
				}
				result.format = "$T.ofNullable(" + result.format + ".getIfAvailable())";
				result.types.add(0, ClassName.get(Optional.class));
			}
		}
		else if (paramType instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) paramType;
			// Really?
			result.format = "context.getBeanProvider($T.class).stream().collect($T.toList()).toArray(new $T[0])";
			result.types.add(TypeName.get(utils.erasure(arrayType.getComponentType())));
			result.types.add(TypeName.get(Collectors.class));
			result.types.add(TypeName.get(utils.erasure(arrayType.getComponentType())));

		}
		else if (paramType instanceof DeclaredType
				&& (utils.implementsInterface(typeElement, ClassName.get(List.class))
						|| utils.implementsInterface(typeElement,
								ClassName.get(Collection.class)))) {
			DeclaredType declaredType = (DeclaredType) paramType;
			List<? extends TypeMirror> args = declaredType.getTypeArguments();
			// TODO: make this work with more general collection elements types
			if (!args.isEmpty()) {
				TypeMirror type = args.iterator().next();
				TypeName value = TypeName.get(utils.erasure(type));
				result.format = "context.getBeanProvider($T.class).stream().collect($T.toList())";
				if (type instanceof DeclaredType
						&& !((DeclaredType) type).getTypeArguments().isEmpty()) {
					result.format = "$T.generic(context.getBeanProvider($T.forClassWithGenerics($T.class, $T.class)).stream().collect($T.toList()))";
					result.types.add(SpringClassNames.OBJECT_UTILS);
					result.types.add(SpringClassNames.RESOLVABLE_TYPE);
					result.types.add(value);
					type = ((DeclaredType) type).getTypeArguments().iterator().next();
					value = TypeName.get(utils.erasure(type));
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
		else if (utils.implementsInterface(typeElement, ClassName.get(Map.class))
				&& paramType instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) paramType;
			List<? extends TypeMirror> args = declaredType.getTypeArguments();
			// TODO: make this work with more general collection elements types
			if (args.size() > 1) {
				TypeMirror type = args.get(1);
				TypeName value = TypeName.get(utils.erasure(type));
				result.format = "context.getBeansOfType($T.class)";
				result.types.add(value);
			}
		}
		else {
			String qualifier = utils.getQualifier(param);
			if (qualifier != null) {
				result.format = "$T.qualifiedBeanOfType(context, $T.class, \"" + qualifier
						+ "\")";
				result.types.add(SpringClassNames.BEAN_FACTORY_ANNOTATION_UTILS);
				result.types.add(TypeName.get(utils.erasure(param)));
			}
			else {
				result.format = "context.getBean($T.class)";
				result.types.add(TypeName.get(utils.erasure(param)));
			}
		}
		return result;
	}

	private <T> Stream<T> getParameters(ExecutableElement method,
			Function<VariableElement, T> mapper) {
		return method.getParameters().stream().map(mapper);
	}

	private List<ExecutableElement> getBeanMethods(TypeElement type) {
		Set<Name> seen = new HashSet<>();
		List<ExecutableElement> beanMethods = new ArrayList<>();
		while (type != null) {
			for (ExecutableElement candidate : ElementFilter
					.methodsIn(type.getEnclosedElements())) {
				if (isBeanMethod(candidate) && seen.add(candidate.getSimpleName())) {
					beanMethods.add(candidate);
				}
			}
			type = utils.getSuperType(type);
		}
		return beanMethods;
	}

	private ExecutableElement getConstructor(TypeElement type) {
		Set<Name> seen = new HashSet<>();
		List<ExecutableElement> methods = new ArrayList<>();
		for (ExecutableElement candidate : ElementFilter
				.constructorsIn(type.getEnclosedElements())) {
			if (seen.add(candidate.getSimpleName())) {
				methods.add(candidate);
			}
		}
		// TODO: pick one that is explicitly autowired?
		if (methods.isEmpty()) {
			System.err.println("Wah: " + type);
		}
		return methods.get(0);
	}

	private boolean isBeanMethod(ExecutableElement element) {
		Set<Modifier> modifiers = element.getModifiers();
		if (!isAnnotated(element, SpringClassNames.BEAN)) {
			return false;
		}
		if (modifiers.contains(Modifier.PRIVATE)) {
			return false;
		}
		return true;
	}

	private boolean isAnnotated(Element element, ClassName type) {
		for (AnnotationMirror candidate : element.getAnnotationMirrors()) {
			if (type.equals(ClassName.get(candidate.getAnnotationType()))) {
				return true;
			}
		}
		return false;
	}

	static class Parameter {
		private String format;
		private List<TypeName> types = new ArrayList<>();
	}

	static class Parameters {
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
