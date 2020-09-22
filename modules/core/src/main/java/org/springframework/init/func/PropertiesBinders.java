package org.springframework.init.func;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.core.OrderComparator;
import org.springframework.core.env.Environment;

public class PropertiesBinders {

	private Map<Class<?>, SortedSet<PropertiesBinder<?>>> delegates = new HashMap<>();

	public void register(Class<?> type, PropertiesBinder<?> binder) {
		delegates.computeIfAbsent(type, key -> new TreeSet<>(OrderComparator.INSTANCE)).add(binder);
	}

	public boolean isBindable(Class<?> type) {
		return delegates.containsKey(type);
	}

	public <T> T bind(T bean, Environment environment) {
		if (delegates.containsKey(bean.getClass())) {
			for (PropertiesBinder<?> binder : delegates.get(bean.getClass())) {
				@SuppressWarnings("unchecked")
				PropertiesBinder<T> instance = (PropertiesBinder<T>) binder;
				bean = instance.bind(bean, environment);
			}
		}
		return bean;
	}

}
