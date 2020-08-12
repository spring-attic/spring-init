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
package org.springframework.boot.actuate.autoconfigure.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointHttpMethod;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;

/**
 * @author Dave Syer
 *
 */
public class SimpleEndpointLinksResolver extends EndpointLinksResolver {

	public SimpleEndpointLinksResolver(WebEndpointProperties endpoints) {
		super(Arrays.asList(health(endpoints.getBasePath()), info(endpoints.getBasePath())));
	}

	private static ExposableWebEndpoint health(String path) {
		return new ExposableWebEndpoint() {

			@Override
			public EndpointId getEndpointId() {
				return EndpointId.of("health");
			}

			@Override
			public boolean isEnableByDefault() {
				return true;
			}

			@Override
			public Collection<WebOperation> getOperations() {
				return Arrays.asList(new SimpleOperation("health"),
						new SimpleOperation("health-path", "health/{*path}"));
			}

			@Override
			public String getRootPath() {
				return path;
			}

		};
	}

	private static ExposableWebEndpoint info(String path) {
		return new ExposableWebEndpoint() {

			@Override
			public EndpointId getEndpointId() {
				return EndpointId.of("info");
			}

			@Override
			public boolean isEnableByDefault() {
				return true;
			}

			@Override
			public Collection<WebOperation> getOperations() {
				return Arrays.asList(new SimpleOperation("info"));
			}

			@Override
			public String getRootPath() {
				return path;
			}
		};
	}

	static class SimpleOperation implements WebOperation {

		private String id;

		private String path;

		public SimpleOperation(String id) {
			this(id, id);
		}

		public SimpleOperation(String id, String path) {
			this.id = id;
			this.path = path;
		}

		@Override
		public OperationType getType() {
			return OperationType.READ;
		}

		@Override
		public Object invoke(InvocationContext context) {
			return null;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public boolean isBlocking() {
			return true;
		}

		@Override
		public WebOperationRequestPredicate getRequestPredicate() {
			return new WebOperationRequestPredicate(path, WebEndpointHttpMethod.GET, Collections.emptyList(),
					Collections.emptyList());
		}

	}

}
