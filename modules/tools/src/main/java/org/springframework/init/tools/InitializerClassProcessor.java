package org.springframework.init.tools;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.squareup.javapoet.JavaFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;

public class InitializerClassProcessor {

	private static Log logger = LogFactory.getLog(InitializerClassProcessor.class);

	private InitializerSpecs specs;

	private InfrastructureProviderSpecs infras;

	private ElementUtils utils;

	private Imports imports;

	private Components components;

	public InitializerClassProcessor() {
		this.utils = new ElementUtils();
		this.imports = new Imports(this.utils);
		this.components = new Components(this.utils);
		this.specs = new InitializerSpecs(this.utils, this.imports, this.components);
		this.infras = new InfrastructureProviderSpecs(this.utils);
	}

	private Set<Class<?>> collectTypes(Class<?> type, Predicate<Class<?>> typeSelectionCondition) {
		Set<Class<?>> types = new LinkedHashSet<>();
		collectTypes(type, types, typeSelectionCondition);
		return types;
	}

	private void collectTypes(Class<?> type, Set<Class<?>> types, Predicate<Class<?>> typeSelectionCondition) {
		if (typeSelectionCondition.test(type)) {
			components.addComponent(type);
			types.add(type);
			if (utils.hasAnnotation(type, SpringClassNames.COMPONENT_SCAN.toString())) {
				List<Class<?>> bases = new ArrayList<>(utils.getTypesFromAnnotation(type,
						SpringClassNames.COMPONENT_SCAN.toString(), "basePackageClasses"));
				// TODO: Support for base packages as strings, support for filters
				if (bases.isEmpty()) {
					bases.add(type);
				}
				for (Class<?> base : bases) {
					collectTypes(ClassUtils.getPackageName(base), types, typeSelectionCondition
							.and(cls -> utils.hasAnnotation(cls, SpringClassNames.COMPONENT.toString())));
				}
			}
			for (Class<?> element : utils.getMemberClasses(type)) {
				if (utils.hasAnnotation(element, SpringClassNames.CONFIGURATION.toString())) {
					logger.info("Found nested @Configuration in " + element);
					imports.addNested(type, element);
				}
				collectTypes(element, types, typeSelectionCondition);
			}
		}
	}

	private void collectTypes(String packageName, Set<Class<?>> types, Predicate<Class<?>> typeSelectionCondition) {
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Set<Class<?>> seen = new HashSet<>();
		seen.addAll(types);
		try {
			String packagePath = "/" + packageName.replace(".", "/") + "/";
			Resource[] resources = resolver.getResources("classpath*:" + packagePath + "**/*.class");
			for (Resource resource : resources) {
				String path = resource.getURL().toString().replace(".class", "");
				String name = ClassUtils.convertResourcePathToClassName(path.substring(path.indexOf(packagePath) + 1));
				if (ClassUtils.isPresent(name, null)) {
					Class<?> type = ClassUtils.resolveClassName(name, null);
					if (!seen.contains(type)) {
						if (utils.hasAnnotation(type, SpringClassNames.COMPONENT.toString())) {
							collectTypes(type, types, typeSelectionCondition);
						}
					}
				}
			}
		}
		catch (IOException e) {
			throw new IllegalStateException("Cannot locate resources in package: " + packageName, e);
		}
	}

	public Set<JavaFile> process(String packageName) {
		Set<JavaFile> result = new HashSet<>();
		Set<Class<?>> types = new LinkedHashSet<>();
		collectTypes(packageName, types,
				te -> !Modifier.isAbstract(te.getModifiers()) && !Modifier.isStatic(te.getModifiers()));
		for (Class<?> type : types) {
			result.addAll(process(type));
		}
		return result;
	}

	public Set<JavaFile> process(Class<?> application) {
		Set<JavaFile> result = new HashSet<>();
		Set<Class<?>> types = collectTypes(application, te -> //
		!Modifier.isAbstract(te.getModifiers()) && //
				!Modifier.isStatic(te.getModifiers()) && //
				utils.hasAnnotation(te, SpringClassNames.COMPONENT.toString()));
		for (Class<?> type : types) {
			if (utils.hasAnnotation(type, SpringClassNames.CONFIGURATION.toString())) {
				logger.info("Found @Configuration in " + type);
				specs.addInitializer(type);
			}
		}
		// Hoover up any imports that didn't already get turned into initializers
		for (Class<?> importer : imports.getImports().keySet()) {
			for (Class<?> imported : imports.getImports(importer)) {
				String root = utils.getPackage(importer);
				// Only if they are in the same package (a reasonable proxy for "in
				// this source module")
				if (utils.getPackage(imported).equals(root) && !utils.isImporter(imported)) {
					specs.addInitializer(imported);
				}
			}
		}
		// Work out what these modules include
		Map<Class<?>, InitializerSpec> initializers = new HashMap<>();
		for (int count = 1; count > 0;) {
			int total = initializers.size();
			for (InitializerSpec initializer : specs.getInitializers()) {
				try {
					initializer.getInitializer();
					initializers.put(initializer.getConfigurationType(), initializer);
				}
				catch (Throwable e) {
					// TODO: this is a sign that something is missing from the classpath,
					// and it
					// might blow up at runtime.
					logger.info("Skipping: " + initializer.getClassName(), e);
				}
			}
			count = initializers.size() - total;
		}
		for (Class<?> type : types) {
			if (utils.hasAnnotation(type, SpringClassNames.SPRING_BOOT_CONFIGURATION.toString())) {
				logger.info("Found @SpringBootConfiguration in " + type);
				if (InitializerApplication.closedWorld) {
					logger.info("Adding ConditionService for " + type);
					new ConditionServiceGenerator().process(type, result);
				}
				infras.addProvider(type);
			}
		}
		for (InitializerSpec initializer : initializers.values()) {
			logger.info("Writing Initializer " + initializer.getPackage() + "." + initializer.getInitializer().name);
			result.add(JavaFile.builder(initializer.getPackage(), initializer.getInitializer()).build());
		}
		for (InfrastructureProviderSpec provider : infras.getProviders()) {
			logger.info("Writing InfrastructureProvider " + provider.getPackage() + "." + provider.getProvider().name);
			result.add(JavaFile.builder(provider.getPackage(), provider.getProvider()).build());
		}
		return result;
	}

}
