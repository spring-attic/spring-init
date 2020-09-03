package org.springframework.init.func;

import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;

public class BeanRegistrar {
	
	public static <T> BeanDefinition generic(ParameterizedTypeReference<T> type, Supplier<T> supplier, BeanDefinitionCustomizer ...customizers) {
		RootBeanDefinition bean = new RootBeanDefinition();
		bean.setInstanceSupplier(supplier);
		bean.setTargetType(ResolvableType.forType(type));
		for (BeanDefinitionCustomizer customizer : customizers) {
			customizer.customize(bean);
		}
		return bean;
	}

}
