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

package app.vsble.sub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import app.vsble.Bar;

/**
 * A component for scanning can be package private.
 * 
 * @author Dave Syer
 *
 */
@Component
class Runner implements CommandLineRunner {

	@Value("${app.value}")
	private String message;
	private Bar bar;
	
	public Runner(Bar bar) {
		this.bar = bar;
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Message: " + message);
		System.out.println("Bar: " + bar);
		System.out.println("Foo: " + bar.getFoo());
	}

}
