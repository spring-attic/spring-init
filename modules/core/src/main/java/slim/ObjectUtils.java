/*
 * Copyright 2018 the original author or authors.
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

package slim;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
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
	public static <T> ObjectProvider<Map<String, T>> map(ListableBeanFactory beans, Class<T> type) {
		return new ObjectProvider<Map<String,T>>() {

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
	public static <T> ObjectProvider<T[]> array(ListableBeanFactory beans, Class<T> type) {
		return new ObjectProvider<T[]>() {

			@SuppressWarnings("unchecked")
			@Override
			public T[] getObject() throws BeansException {
				return (T[]) beans.getBeanProvider(type).orderedStream().collect(Collectors.toList()).toArray();
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
}
