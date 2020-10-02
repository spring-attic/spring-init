/*
 * Copyright 2019-2019 the original author or authors.
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
package org.springframework.init.library;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.availability.ProbesCondition;
import org.springframework.boot.actuate.autoconfigure.web.reactive.ActuatorReactiveEndpointsCondition;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ActuatorServletEndpointsCondition;
import org.springframework.boot.autoconfigure.context.MessageSourceCondition;
import org.springframework.init.func.TypeCondition;
import org.springframework.init.func.TypeConditionMapper;

/**
 * @author Dave Syer
 *
 */
public class LibraryTypeConditionMapper implements TypeConditionMapper {

	private static Map<String, TypeCondition> CONDITIONS = new HashMap<>();

	static {
		CONDITIONS.put("org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration",
				new MessageSourceCondition());
		CONDITIONS.put(
				"org.springframework.boot.actuate.autoconfigure.availability.AvailabilityProbesAutoConfiguration",
				new ProbesCondition());
		CONDITIONS.put(
				"org.springframework.boot.actuate.autoconfigure.web.reactive.FunctionalReactiveActuatorEndpointAutoConfiguration",
				new ActuatorReactiveEndpointsCondition());
		CONDITIONS.put(
				"org.springframework.boot.actuate.autoconfigure.web.servlet.FunctionalServletActuatorEndpointAutoConfiguration",
				new ActuatorServletEndpointsCondition());
	}

	@Override
	public Map<String, TypeCondition> get() {
		return CONDITIONS;
	}

}
