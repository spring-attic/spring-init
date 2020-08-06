package org.springframework.init.func;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.env.Environment;

public class PropertiesBinders {

	private Map<Class<?>, PropertiesBinder<?>> delegates = new HashMap<>();

	public void register(Class<?> type, PropertiesBinder<?> binder) {
		delegates.put(type, binder);
	}

	public boolean isBindable(Class<?> type) {
		return delegates.containsKey(type);
	}

	public <T> T bind(T bean, Environment environment) {
		@SuppressWarnings("unchecked")
		PropertiesBinder<T> binder = (PropertiesBinder<T>) delegates.get(bean.getClass());
		if (binder != null) {
			return binder.bind((T)bean, environment);
		}
		return bean;
	}

}
