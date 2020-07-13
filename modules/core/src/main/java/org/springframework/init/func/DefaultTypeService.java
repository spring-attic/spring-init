/*
 * Copyright 2020-2020 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public class DefaultTypeService implements TypeService {

	private ClassLoader classLoader;

	public DefaultTypeService(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public boolean isPresent(String name) {
		return ClassUtils.isPresent(name, classLoader);
	}

	@Override
	public Class<?> getType(String name) {
		return isPresent(name) ? ClassUtils.resolveClassName(name, classLoader) : null;
	}

}
