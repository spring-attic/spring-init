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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 *
 */
public class SimpleConditionService implements ConditionService {

	private Map<Class<?>, Boolean> typeMatches = new HashMap<>();

	private Map<Class<?>, Set<Class<?>>> methodMatches = new HashMap<>();

	private ConditionService fallback;

	public SimpleConditionService(ConditionService service) {
		this(service, Collections.emptyMap(), Collections.emptyMap());
	}

	public SimpleConditionService() {
		this(null, Collections.emptyMap(), Collections.emptyMap());
	}

	public SimpleConditionService(Map<Class<?>, Boolean> typeMatches, Map<Class<?>, Set<Class<?>>> methodMatches) {
		this(null, typeMatches, methodMatches);
	}

	SimpleConditionService(@Nullable ConditionService service, Map<Class<?>, Boolean> typeMatches,
			Map<Class<?>, Set<Class<?>>> methodMatches) {
		this.fallback = service;
		this.typeMatches.putAll(typeMatches);
		this.methodMatches.putAll(methodMatches);
	}

	@Override
	public boolean matches(Class<?> type, ConfigurationPhase phase) {
		if (typeMatches.containsKey(type)) {
			return typeMatches.get(type);
		}
		boolean matches = fallback == null ? false : fallback.matches(type, phase);
		match(type, matches);
		return matches;
	}

	@Override
	public boolean matches(Class<?> type) {
		return matches(type, (ConfigurationPhase) null);
	}

	@Override
	public boolean matches(Class<?> factory, Class<?> type) {
		if (methodMatches.containsKey(factory)) {
			return methodMatches.get(factory).contains(type);
		}
		boolean matches = fallback == null ? false : fallback.matches(factory, type);
		match(factory, type, matches);
		return matches;
	}

	@Override
	public boolean includes(Class<?> type) {
		return fallback == null ? true : fallback.includes(type);
	}

	public SimpleConditionService match(Class<?> type, boolean matches) {
		typeMatches.put(type, matches);
		return this;
	}

	public SimpleConditionService match(Class<?> factory, Class<?> type, boolean matches) {
		if (matches) {
			methodMatches.computeIfAbsent(type, key -> new HashSet<>()).add(type);
		} else {
			methodMatches.computeIfAbsent(type, key -> new HashSet<>());
		}
		return this;
	}

	public Map<Class<?>, Boolean> getTypeMatches() {
		return typeMatches;
	}

	public Map<Class<?>, Set<Class<?>>> getMethodMatches() {
		return methodMatches;
	}

}
