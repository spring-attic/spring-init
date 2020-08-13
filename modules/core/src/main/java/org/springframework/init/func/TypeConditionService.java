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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Dave Syer
 *
 */
public class TypeConditionService implements ConditionService {

	private static Log logger = LogFactory.getLog(TypeConditionService.class);

	private Map<String, TypeCondition> typeMatches = new HashMap<>();

	private ConditionService fallback;

	private GenericApplicationContext context;

	public TypeConditionService(GenericApplicationContext context, ConditionService service) {
		this(context, service, Collections.emptyMap());
	}

	public TypeConditionService(GenericApplicationContext context) {
		this(context, null, Collections.emptyMap());
	}

	public TypeConditionService(GenericApplicationContext context, ConditionService service,
			Map<String, TypeCondition> typeMatches) {
		this.context = context;
		this.fallback = service;
		this.typeMatches.putAll(typeMatches);
	}

	public void setFallback(ConditionService fallback) {
		this.fallback = fallback;
	}

	@Override
	public boolean matches(Class<?> type, ConfigurationPhase phase) {
		if (logger.isDebugEnabled()) {
			logger.debug(type.getName() + ": " + typeMatches.get(type.getName()));
		}
		if (typeMatches.containsKey(type.getName())) {
			return typeMatches.get(type.getName()).matches(context);
		}
		ConditionService fallback = getFallback();
		boolean matches = fallback == null ? false : fallback.matches(type, phase);
		return matches;
	}

	@Override
	public boolean matches(Class<?> type) {
		return matches(type, (ConfigurationPhase) null);
	}

	@Override
	public boolean matches(Class<?> factory, Class<?> type) {
		if (typeMatches.containsKey(factory.getName())) {
			return typeMatches.get(factory.getName()).matches(type.getName(), context);
		}
		ConditionService fallback = getFallback();
		boolean matches = fallback == null ? false : fallback.matches(factory, type);
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

}
