package org.springframework.init.func;

import org.springframework.core.env.Environment;

public interface PropertiesBinder<T> {

	T bind(T bean, Environment environment);

}
