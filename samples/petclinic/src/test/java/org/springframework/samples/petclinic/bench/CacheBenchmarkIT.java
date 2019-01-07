/*
LazyInitBeanFactoryPostProcessor.java * Copyright 2016-2017 the original author or authors.
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.init.EnableSelectedAutoConfiguration;
import org.springframework.init.bench.LauncherApplication;
import org.springframework.init.bench.LauncherState;

import static org.assertj.core.api.Assertions.assertThat;

import jmh.mbr.junit5.Microbenchmark;

@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 1, time = 1)
@Fork(value = 2, warmups = 0)
@BenchmarkMode(Mode.AverageTime)
@Microbenchmark
public class CacheBenchmarkIT {

	@Benchmark
	public void launch(MainState state) throws Exception {
		state.isolated();
	}

	@State(Scope.Thread)
	public static class MainState extends LauncherState {

		public static enum Sample {
			empty(EmptyApplication.class), simple(CacheApplication.class), cache(CacheApplication.class);
			private Class<?> config;

			private Sample(Class<?> config) {
				this.config = config;
			}
		}

		@Param
		private Sample sample = Sample.simple;

		public MainState() {
			super(CacheApplication.class);
			addProperties("spring.main.web-application-type=none");
		}

		@TearDown(Level.Invocation)
		public void stop() throws Exception {
			super.close();
		}

		@Setup(Level.Invocation)
		public void start() throws Exception {
			setMainClass(sample.config);
			switch (sample) {
			case simple:
				addProperties("spring.cache.cache-names=app");
				addProperties("spring.cache.type=simple");
				break;

			case cache:
				addProperties("spring.cache.cache-names=app");
				break;

			default:
				break;
			}
			super.start();
		}

		public void setSample(Sample sample) {
			this.sample = sample;
		}
		
		@Override
		protected URL[] filterClassPath(URL[] urls) {
			if (sample==Sample.simple) {
				List<URL> list = new ArrayList<>();
				for (URL url : urls) {
					if (!url.toString().contains("cache-api")) {
						list.add(url);
					}
				}
				return list.toArray(new URL[0]);
			}
			return super.filterClassPath(urls);
		}
	}

	@EnableCaching
	@SpringBootConfiguration
	@EnableSelectedAutoConfiguration({ CacheAutoConfiguration.class,
			ConfigurationPropertiesAutoConfiguration.class })
	public static class CacheApplication extends LauncherApplication {

		public static void main(String[] args) throws Exception {
			LauncherApplication.run(CacheApplication.class, args);
		}

		@Override
		public void run() {
			super.run();
			assertThat(getContext().getBean(CacheManager.class)).isNotNull();
		}

	}

	@SpringBootConfiguration
	@EnableSelectedAutoConfiguration({ ConfigurationPropertiesAutoConfiguration.class })
	public static class EmptyApplication extends LauncherApplication {
		public static void main(String[] args) throws Exception {
			LauncherApplication.run(EmptyApplication.class, args);
		}
	}
}
