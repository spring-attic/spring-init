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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;

/**
 * @author Dave Syer
 *
 */
public class SpringClassNames {

	public static final ClassName IMPORT_SELECTOR = ClassName
			.get("org.springframework.context.annotation", "ImportSelector");

	public static final ClassName OBJECT_PROVIDER = ClassName
			.get("org.springframework.beans.factory", "ObjectProvider");

	public static final ClassName IMPORT_BEAN_DEFINITION_REGISTRAR = ClassName.get(
			"org.springframework.context.annotation", "ImportBeanDefinitionRegistrar");

	public static final ClassName STANDARD_ANNOTATION_METADATA = ClassName
			.get("org.springframework.core.type", "StandardAnnotationMetadata");

	public static final ClassName CONFIGURATION = ClassName
			.get("org.springframework.context.annotation", "Configuration");

	public static final ClassName COMPONENT = ClassName
			.get("org.springframework.stereotype", "Component");

	public static final ClassName QUALIFIER = ClassName
			.get("org.springframework.beans.factory.annotation", "Qualifier");

	public static final ClassName BEAN_FACTORY_ANNOTATION_UTILS = ClassName.get(
			"org.springframework.beans.factory.annotation", "BeanFactoryAnnotationUtils");

	public static final ClassName COMPONENT_SCAN = ClassName
			.get("org.springframework.context.annotation", "ComponentScan");

	public static final ClassName SPRING_BOOT_CONFIGURATION = ClassName
			.get("org.springframework.boot", "SpringBootConfiguration");

	public static final ClassName BEAN = ClassName
			.get("org.springframework.context.annotation", "Bean");

	public static final ClassName IMPORT = ClassName
			.get("org.springframework.context.annotation", "Import");

	public static final ClassName IMPORT_RESOURCE = ClassName
			.get("org.springframework.context.annotation", "ImportResource");

	public static final ClassName NULLABLE = ClassName.get("org.springframework.lang",
			"Nullable");

	public static final ClassName OBJECT_UTILS = ClassName.get("slim", "ObjectUtils");

	public static final ClassName CONDITION_SERVICE = ClassName.get("slim",
			"ConditionService");

	public static final ClassName IMPORT_REGISTRARS = ClassName.get("slim",
			"ImportRegistrars");

	public static final ClassName APPLICATION_CONTEXT_INITIALIZER = ClassName
			.get("org.springframework.context", "ApplicationContextInitializer");

	public static final ClassName APPLICATION_CONTEXT = ClassName
			.get("org.springframework.context", "ApplicationContext");

	public static final ClassName WEB_APPLICATION_CONTEXT = ClassName
			.get("org.springframework.web.context", "WebApplicationContext");

	public static final ClassName CONFIGURABLE_APPLICATION_CONTEXT = ClassName
			.get("org.springframework.context", "ConfigurableApplicationContext");

	public static final ClassName BEAN_FACTORY = ClassName
			.get("org.springframework.beans.factory", "BeanFactory");

	public static final ClassName LISTABLE_BEAN_FACTORY = ClassName
			.get("org.springframework.beans.factory", "ListableBeanFactory");

	public static final ClassName CONFIGURABLE_LISTABLE_BEAN_FACTORY = ClassName.get(
			"org.springframework.beans.factory.config",
			"ConfigurableListableBeanFactory");

	public static final ClassName GENERIC_APPLICATION_CONTEXT = ClassName
			.get("org.springframework.context.support", "GenericApplicationContext");

	public static final ParameterizedTypeName INITIALIZER_TYPE = ParameterizedTypeName
			.get(APPLICATION_CONTEXT_INITIALIZER, GENERIC_APPLICATION_CONTEXT);

	public static final ClassName CONDITIONAL = ClassName
			.get("org.springframework.context.annotation", "Conditional");

	public static final ClassName ENABLE_CONFIGURATION_PROPERTIES = ClassName.get(
			"org.springframework.boot.context.properties",
			"EnableConfigurationProperties");

	public static final ClassName CONFIGURATION_PROPERTIES = ClassName.get(
			"org.springframework.boot.context.properties", "ConfigurationProperties");

	public static final ClassName RESOLVABLE_TYPE = ClassName
			.get("org.springframework.core", "ResolvableType");

	public static final ClassName CLASS_UTILS = ClassName.get("org.springframework.util",
			"ClassUtils");

	public static final ClassName FACTORY_BEAN = ClassName.get("org.springframework.beans.factory",
			"FactoryBean");

}
