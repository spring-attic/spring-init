/*
 * Copyright 2016-2017 the original author or authors.
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

package com.example.bench;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.init.bench.CaptureSystemOutput;
import org.springframework.init.bench.CaptureSystemOutput.OutputCapture;
import org.springframework.init.bench.LauncherState;
import org.springframework.init.select.EnableSelectedAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
@CaptureSystemOutput
public class LauncherStateTests {

	private LauncherState state;

	@BeforeEach
	public void init() throws Exception {
		state = new LauncherState(PrivateApplication.class);
		state.addProperties("spring.main.web-application-type=none");
		state.start();
	}

	@AfterEach
	public void close() throws Exception {
		if (state != null) {
			state.close();
		}
	}

	@Test
	public void isolated(OutputCapture output) throws Exception {
		state.isolated();
		assertThat(output.toString()).contains("Benchmark app started");
	}

	@Test
	public void shared(OutputCapture output) throws Exception {
		// System.setProperty("bench.args", "-verbose:class");
		state.shared();
		assertThat(output.toString()).contains("Benchmark app started");
	}

	@SpringBootConfiguration
	@EnableSelectedAutoConfiguration
	public static class PrivateApplication {

		public static void main(String[] args) throws Exception {
			SpringApplication.run(PrivateApplication.class, args);
		}

	}
}
