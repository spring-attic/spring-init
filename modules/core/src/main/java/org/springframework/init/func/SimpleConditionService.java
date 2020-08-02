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
import java.util.Map;

import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 *
 */
public class SimpleConditionService implements ConditionService {

	private Map<String, Boolean> typeMatches = new HashMap<>();

	private Map<String, Map<String, Boolean>> methodMatches = new HashMap<>();

	private ConditionService fallback;

	public SimpleConditionService(ConditionService service) {
		this(service, Collections.emptyMap(), Collections.emptyMap());
	}

	public SimpleConditionService() {
		this(null, Collections.emptyMap(), Collections.emptyMap());
	}

	public SimpleConditionService(Map<String, Boolean> typeMatches, Map<String, Map<String, Boolean>> methodMatches) {
		this(null, typeMatches, methodMatches);
	}

	public SimpleConditionService(@Nullable ConditionService service, Map<String, Boolean> typeMatches,
			Map<String, Map<String, Boolean>> methodMatches) {
		this.fallback = service;
		this.typeMatches.putAll(typeMatches);
		this.methodMatches.putAll(methodMatches);
	}

	public void setFallback(ConditionService fallback) {
		this.fallback = fallback;
	}

	@Override
	public boolean matches(Class<?> type, ConfigurationPhase phase) {
		if (typeMatches.containsKey(type.getName())) {
			return typeMatches.get(type.getName());
		}
		ConditionService fallback = getFallback();
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
		if (methodMatches.containsKey(factory.getName())
				&& methodMatches.get(factory.getName()).containsKey(type.getName())) {
			return methodMatches.get(factory.getName()).get(type.getName());
		}
		ConditionService fallback = getFallback();
		boolean matches = fallback == null ? false : fallback.matches(factory, type);
		match(factory.getName(), type.getName(), matches);
		return matches;
	}

	private ConditionService getFallback() {
		return this.fallback;
	}

	@Override
	public boolean includes(Class<?> type) {
		ConditionService fallback = getFallback();
		return fallback == null ? true : fallback.includes(type);
	}

	public SimpleConditionService match(Class<?> type, boolean matches) {
		typeMatches.put(type.getName(), matches);
		return this;
	}

	public SimpleConditionService match(String factory, String type, boolean matches) {
		if (matches) {
			methodMatches.computeIfAbsent(factory, key -> new HashMap<>()).put(type, true);
		} else {
			methodMatches.computeIfAbsent(factory, key -> new HashMap<>()).put(type, false);
		}
		return this;
	}

	public Map<String, Boolean> getTypeMatches() {
		return typeMatches;
	}

	public Map<String, Map<String, Boolean>> getMethodMatches() {
		return methodMatches;
	}

}
