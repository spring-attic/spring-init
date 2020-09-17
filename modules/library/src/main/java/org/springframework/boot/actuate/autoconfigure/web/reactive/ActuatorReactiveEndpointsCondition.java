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

package org.springframework.boot.actuate.autoconfigure.web.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebEnvironment;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.init.func.TypeCondition;
import org.springframework.web.reactive.function.server.RouterFunction;

public class ActuatorReactiveEndpointsCondition implements TypeCondition {

	@Override
	public boolean matches(ConditionContext context) {
		return isReactiveWebApplication(context).isMatch();
	}

	@Override
	public boolean matches(String resultType, ConditionContext context) {
		if (RouterFunction.class.getName().equals(resultType)) {
			return !context.getBeanFactory().containsBean("webEndpointReactiveHandlerMapping");
		}
		return TypeCondition.super.matches(resultType, context);
	}

	private ConditionOutcome isReactiveWebApplication(ConditionContext context) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("");
		if (context.getEnvironment() instanceof ConfigurableReactiveWebEnvironment) {
			return ConditionOutcome.match(message.foundExactly("ConfigurableReactiveWebEnvironment"));
		}
		if (context.getResourceLoader() instanceof ReactiveWebApplicationContext) {
			return ConditionOutcome.match(message.foundExactly("ReactiveWebApplicationContext"));
		}
		return ConditionOutcome.noMatch(message.because("not a reactive web application"));
	}

}