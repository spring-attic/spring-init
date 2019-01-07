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

package org.springframework.samples.petclinic.bench;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.init.bench.CaptureSystemOutput;
import org.springframework.init.bench.CaptureSystemOutput.OutputCapture;
import org.springframework.samples.petclinic.bench.CacheBenchmarkIT.MainState;
import org.springframework.samples.petclinic.bench.CacheBenchmarkIT.MainState.Sample;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
@CaptureSystemOutput
public class CacheLauncherStateTests {

	@BeforeAll
	public static void init() {
		System.setProperty("logging.level.org.springframework.init", "DEBUG");
	}

	@Test
	public void vanilla(OutputCapture output) throws Exception {
		MainState state = new MainState();
		state.setSample(Sample.empty);
		state.start();
		state.isolated();
		state.close();
		assertThat(output.toString()).contains("Benchmark app started");
		assertThat(output.toString()).doesNotContain("cacheManager");
	}

	@Test
	public void cache(OutputCapture output) throws Exception {
		MainState state = new MainState();
		state.setSample(Sample.cache);
		state.start();
		state.isolated();
		state.close();
		assertThat(output.toString()).contains("Benchmark app started");
		assertThat(output.toString()).contains("cacheManager");
		assertThat(output.toString()).contains("Ehcache");
	}

	@Test
	public void simple(OutputCapture output) throws Exception {
		MainState state = new MainState();
		state.setSample(Sample.simple);
		state.start();
		state.isolated();
		state.close();
		assertThat(output.toString()).contains("Benchmark app started");
		assertThat(output.toString()).contains("cacheManager");
		assertThat(output.toString()).doesNotContain("JCache");
	}

}
