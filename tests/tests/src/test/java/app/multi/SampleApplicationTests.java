/*
 * Copyright 2018 the original author or authors.
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

package app.multi;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

import lib.multi.Bar;
import lib.multi.Foo;

/**
 * @author Dave Syer
 *
 */
@SpringBootTest(properties = "spring.functional.enabled=false")
@RunWith(SpringRunner.class)
public class SampleApplicationTests {

	@Autowired
	private Foo foo;

	@Autowired(required = false)
	private Bar bar;

	@Autowired
	private ApplicationContext context;

	@Test
	public void test() {
		assertThat(foo).isNotNull();
		assertThat(bar).isNotNull();
		assertThat(context.getBeanNamesForType(CommandLineRunner.class)).hasSize(1);
	}

}
