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

package app.main;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
@SpringBootTest("debug=true")
@RunWith(SpringRunner.class)
public class FunctionalApplicationTests {

	@Autowired
	private WebHandler webHandler;

	private WebTestClient client;

	@Before
	public void init() {
		client = WebTestClient.bindToWebHandler(webHandler).build();
	}

	@Test
	public void test() {
		client.get().uri("/").exchange().expectBody(String.class)
				.value(value -> assertThat(value).contains("Hello"));
	}

}
