/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.samples.petclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.init.SpringInitApplication;
import org.springframework.init.config.JpaDataConfigurations;
import org.springframework.init.config.WebMvcConfigurations;

/**
 * PetClinic Spring Boot Application.
 *
 * @author Dave Syer
 *
 */
@SpringInitApplication({ MessageSourceAutoConfiguration.class,
		JpaDataConfigurations.class, WebMvcConfigurations.class,
		ThymeleafAutoConfiguration.class })
@EntityScan
public class PetClinicApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetClinicApplication.class, args);
	}

}

@Configuration
@Profile("cache")
@ImportAutoConfiguration(CacheAutoConfiguration.class)
class ApplicationCacheConfiguration {
}

@Configuration
@Profile("tx")
@ImportAutoConfiguration(TransactionAutoConfiguration.class)
class ApplicationTxConfiguration {
}

@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
@Configuration
@ImportAutoConfiguration({ EndpointAutoConfiguration.class,
		HealthIndicatorAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
		InfoEndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
		ServletManagementContextAutoConfiguration.class,
		ManagementContextAutoConfiguration.class })
class ApplicationActuatorConfiguration {
}
