/*
 * Copyright 2016-2017 the original author or authors.
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

package com.example.bench;

import com.example.bench.ActrBenchmarkIT.MainState;
import com.example.bench.ActrBenchmarkIT.MainState.Sample;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
public class ActrStateTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void noactr() throws Exception {
		// System.setProperty("bench.args",
		// "-agentlib:jdwp=transport=dt_socket,server=y,address=8000");
		MainState state = new MainState();
		state.setSample(Sample.noactr);
		state.start();
		state.run();
		state.stop();
		output.flush();
		assertThat(output.toString()).contains("Benchmark app started");
		assertThat(output.toString()).doesNotContain("/actuator");
	}

	@Test
	public void vanilla() throws Exception {
		// System.setProperty("bench.args",
		// "-agentlib:jdwp=transport=dt_socket,server=y,address=8000");
		MainState state = new MainState();
		state.setSample(Sample.mixed);
		state.start();
		state.run();
		state.stop();
		output.flush();
		assertThat(output.toString()).contains("Benchmark app started");
		assertThat(output.toString()).contains("/actuator");
	}

	@Test
	public void none() throws Exception {
		// System.setProperty("bench.args", "-verbose:class");
		MainState state = new MainState();
		state.setSample(Sample.none);
		state.start();
		state.run();
		state.stop();
		output.flush();
		assertThat(output.toString()).contains("Benchmark app started");
		assertThat(output.toString()).contains("0 endpoint(s)");
	}

}
