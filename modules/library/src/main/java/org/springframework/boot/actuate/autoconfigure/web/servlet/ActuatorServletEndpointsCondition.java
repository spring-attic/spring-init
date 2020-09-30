/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.init.func.TypeCondition;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.function.RouterFunction;

public class ActuatorServletEndpointsCondition implements TypeCondition {

	@Override
	public boolean matches(ConditionContext context) {
		return isServletWebApplication(context).isMatch();
	}

	@Override
	public boolean matches(String resultType, ConditionContext context) {
		if (RouterFunction.class.getName().equals(resultType)) {
			return !context.getBeanFactory().containsBean("webEndpointServletHandlerMapping");
		}
		return TypeCondition.super.matches(resultType, context);
	}

	private ConditionOutcome isServletWebApplication(ConditionContext context) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("");
		if (context.getBeanFactory() != null) {
			String[] scopes = context.getBeanFactory().getRegisteredScopeNames();
			if (ObjectUtils.containsElement(scopes, "session")) {
				return ConditionOutcome.match(message.foundExactly("'session' scope"));
			}
		}
		if (context.getEnvironment() instanceof ConfigurableWebEnvironment) {
			return ConditionOutcome.match(message.foundExactly("ConfigurableWebEnvironment"));
		}
		if (context.getResourceLoader() instanceof WebApplicationContext) {
			return ConditionOutcome.match(message.foundExactly("WebApplicationContext"));
		}
		return ConditionOutcome.noMatch(message.because("not a servlet web application"));
	}

}