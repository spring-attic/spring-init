package org.springframework.boot.actuate.autoconfigure.web.servlet;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.SimpleEndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.servlet.function.RouterFunction;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Endpoint.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class FunctionalServletActuatorEndpointAutoConfiguration {

	private static Log logger = LogFactory.getLog(FunctionalServletActuatorEndpointAutoConfiguration.class);

	@Bean
	public RouterFunction<?> actuatorEndpoints(WebEndpointProperties endpoints, HealthEndpoint health,
			InfoEndpoint info) {
		String path = endpoints.getBasePath();
		logger.info("Exposing 2 endpoint(s) beneath base path '" + path + "'");
		EndpointLinksResolver resolver = new SimpleEndpointLinksResolver(endpoints);
		return route().GET(path,
				request -> ok().body(
						Collections.singletonMap("_links", resolver.resolveLinks(request.uri().toString())),
						new ParameterizedTypeReference<Map<String, Object>>() {
						})) //
				.GET(path + "/health", request -> ok().body(health.health())) //
				.GET(path + "/health/{path}", request -> ok().body(health.healthForPath(request.pathVariable("path")))) //
				.GET(path + "/info",
						request -> ok().body(info.info(), new ParameterizedTypeReference<Map<String, Object>>() {
						})) //
				.build();
	}

}
