package org.springframework.boot.actuate.autoconfigure.web.reactive;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.SimpleEndpointLinksResolver;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions.Builder;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Endpoint.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@AutoConfigureAfter(ManagementContextAutoConfiguration.class)
public class FunctionalReactiveActuatorEndpointAutoConfiguration {

	private static Log logger = LogFactory
			.getLog(FunctionalReactiveActuatorEndpointAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean(name = "webEndpointReactiveHandlerMapping")
	public RouterFunction<?> actuatorEndpoints(WebEndpointProperties endpoints,
			HealthEndpoint health, ObjectProvider<InfoEndpoint> info) {
		String path = endpoints.getBasePath();
		logger.info("Exposing 2 endpoint(s) beneath base path '" + path + "'");
		EndpointLinksResolver resolver = new SimpleEndpointLinksResolver(endpoints);
		Builder builder = route().GET(path,
				request -> ok().body(
						Mono.just(Collections.singletonMap("_links",
								resolver.resolveLinks(request.uri().toString()))),
						new ParameterizedTypeReference<Map<String, Object>>() {
						})) //
				.GET(path + "/health",
						request -> ok().body(Mono.just(health.health()),
								HealthComponent.class)) //
				.GET(path + "/health/{path}", request -> ok().body(
						Mono.just(health.healthForPath(request.pathVariable("path"))),
						HealthComponent.class));
		info.ifAvailable(endpoint -> builder.GET(path + "/info",
				request -> ok().body(Mono.just(endpoint.info()),
						new ParameterizedTypeReference<Map<String, Object>>() {
						}))); //
		return builder.build();
	}

}
