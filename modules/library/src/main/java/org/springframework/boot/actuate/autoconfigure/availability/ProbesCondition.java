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

package org.springframework.boot.actuate.autoconfigure.availability;

import org.springframework.boot.actuate.availability.LivenessStateHealthIndicator;
import org.springframework.boot.actuate.availability.ReadinessStateHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.init.func.TypeCondition;

public class ProbesCondition implements TypeCondition {

	private static final String ENABLED_PROPERTY = "management.endpoint.health.probes.enabled";

	private static final String DEPRECATED_ENABLED_PROPERTY = "management.health.probes.enabled";

	@Override
	public boolean matches(ConditionContext context) {
		ConditionOutcome outcome = getMatchOutcome(context.getEnvironment());
		return outcome.isMatch();
	}

	@Override
	public boolean matches(String resultType, ConditionContext context) {
		if (LivenessStateHealthIndicator.class.getName().equals(resultType)) {
			return context.getBeanFactory().getBeanNamesForType(LivenessStateHealthIndicator.class, true,
					false).length == 0;
		}
		if (ReadinessStateHealthIndicator.class.getName().equals(resultType)) {
			return context.getBeanFactory().getBeanNamesForType(ReadinessStateHealthIndicator.class, true,
					false).length == 0;
		}
		return TypeCondition.super.matches(resultType, context);
	}

	private ConditionOutcome getMatchOutcome(Environment environment) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("Probes availability");
		ConditionOutcome outcome = onProperty(environment, message, ENABLED_PROPERTY);
		if (outcome != null) {
			return outcome;
		}
		outcome = onProperty(environment, message, DEPRECATED_ENABLED_PROPERTY);
		if (outcome != null) {
			return outcome;
		}
		if (CloudPlatform.getActive(environment) == CloudPlatform.KUBERNETES) {
			return ConditionOutcome.match(message.because("running on Kubernetes"));
		}
		return ConditionOutcome.noMatch(message.because("not running on a supported cloud platform"));
	}

	private ConditionOutcome onProperty(Environment environment, ConditionMessage.Builder message,
			String propertyName) {
		String enabled = environment.getProperty(propertyName);
		if (enabled != null) {
			boolean match = !"false".equalsIgnoreCase(enabled);
			return new ConditionOutcome(match, message.because("'" + propertyName + "' set to '" + enabled + "'"));
		}
		return null;
	}

}