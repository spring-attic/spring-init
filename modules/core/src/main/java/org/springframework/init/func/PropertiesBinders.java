package org.springframework.init.func;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

public class PropertiesBinders {

	private Map<Class<?>, List<PropertiesBinder<?>>> delegates = new HashMap<>();

	public void register(Class<?> type, PropertiesBinder<?> binder) {
		List<PropertiesBinder<?>> result = delegates.computeIfAbsent(type, key -> new ArrayList<>());
		if (!(binder instanceof Ordered)) {
			result.add(binder);
		}
		else {
			int index = Collections.binarySearch(result, binder, OrderComparator.INSTANCE);
			if (index < 0) {
				index = -index - 1;
			}
			result.add(index, binder);
		}
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
