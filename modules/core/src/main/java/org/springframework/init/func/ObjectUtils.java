/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.init.func;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Dave Syer
 *
 */
public class ObjectUtils {
	@SuppressWarnings("unchecked")
	public static <T> T generic(Object thing) {
		return (T) thing;
	}

	public static <T> ObjectProvider<Map<String, T>> map(ListableBeanFactory beans,
			Class<T> type) {
		return new ObjectProvider<Map<String, T>>() {

			@Override
			public Map<String, T> getObject() throws BeansException {
				return beans.getBeansOfType(type);
			}

			@Override
			public Map<String, T> getObject(Object... args) throws BeansException {
				return getObject();
			}

			@Override
			public Map<String, T> getIfAvailable() throws BeansException {
				return getObject();
			}

			@Override
			public Map<String, T> getIfUnique() throws BeansException {
				return getObject();
			}
		};
	}

	public static <T> ObjectProvider<T[]> array(ListableBeanFactory beans,
			Class<T> type) {
		return new ObjectProvider<T[]>() {

			@SuppressWarnings("unchecked")
			private T[] prototype = (T[]) Array.newInstance(type, 0);

			@Override
			public T[] getObject() throws BeansException {
				return beans.getBeanProvider(type).orderedStream()
						.collect(Collectors.toList()).toArray(prototype);
			}

			@Override
			public T[] getObject(Object... args) throws BeansException {
				return getObject();
			}

			@Override
			public T[] getIfAvailable() throws BeansException {
				return getObject();
			}

			@Override
			public T[] getIfUnique() throws BeansException {
				return getObject();
			}
		};
	}

	public static <T> T lazy(Class<T> type, Supplier<T> supplier) {
		TargetSource ts = new TargetSource() {
			@Override
			public Class<?> getTargetClass() {
				return type;
			}

			@Override
			public boolean isStatic() {
				return false;
			}

			@Override
			public Object getTarget() {
				Object target = supplier.get();
				if (target == null) {
					Class<?> type = getTargetClass();
					if (Map.class == type) {
						return Collections.emptyMap();
					}
					else if (List.class == type) {
						return Collections.emptyList();
					}
					else if (Set.class == type || Collection.class == type) {
						return Collections.emptySet();
					}
					throw new NoSuchBeanDefinitionException(type,
							"Optional dependency not present for lazy injection point");
				}
				return target;
			}

			@Override
			public void releaseTarget(Object target) {
			}
		};
		ProxyFactory pf = new ProxyFactory();
		pf.setTargetSource(ts);
		pf.addInterface(type);
		@SuppressWarnings("unchecked")
		T proxy = (T) pf.getProxy();
		return proxy;
	}

}
