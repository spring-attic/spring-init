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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.init.bench.LauncherState;
import org.springframework.init.select.EnableSelectedAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

import jmh.mbr.junit5.Microbenchmark;

@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 1, time = 1)
@Fork(value = 2, warmups = 0)
@BenchmarkMode(Mode.AverageTime)
@Microbenchmark
public class CacheBenchmarkIT {

	@Benchmark
	public void bench(MainState state) throws Exception {
		state.isolated();
	}

	@State(Scope.Thread)
	public static class MainState extends LauncherState {

		public static enum Sample {
			empty(EmptyApplication.class), simple(CacheApplication.class), cache(
					CacheApplication.class), jcache(
							CacheApplication.class), manual(ManualCacheApplication.class);
			private Class<?> config;

			private Sample(Class<?> config) {
				this.config = config;
			}
		}

		public static enum Config {
			functional, annotation;
		}

		@Param
		private Sample sample = Sample.simple;

		@Param
		private Config config = Config.functional;

		public MainState() {
			super(CacheApplication.class);
			addProperties("spring.main.web-application-type=none");
		}

		@TearDown(Level.Invocation)
		public void stop() throws Exception {
			super.close();
		}

		@Override
		@Setup(Level.Invocation)
		public void start() throws Exception {
			setMainClass(sample.config);
			switch (sample) {
			case simple:
			case manual:
			case cache:
				addProperties("spring.cache.cache-names=app");
				addProperties("spring.cache.type=simple");
				break;

			case jcache:
				addProperties("spring.cache.cache-names=app");
				break;

			default:
				break;
			}
			if (config == Config.annotation) {
				addProperties("spring.functional.enabled=false");
				if (sample.config == CacheApplication.class) {
					setMainClass(AnnoCacheApplication.class);
				}
				else if (sample.config == EmptyApplication.class) {
					setMainClass(AnnoEmptyApplication.class);
				}
			}
			super.start();
		}

		public void setSample(Sample sample) {
			this.sample = sample;
		}

		public void setConfig(Config config) {
			this.config = config;
		}

		@Override
		protected URL[] filterClassPath(URL[] urls) {
			List<URL> list = new ArrayList<>();
			for (URL url : urls) {
				if (sample == Sample.simple || sample == Sample.manual) {
					if (url.toString().contains("cache-api")) {
						continue;
					}
				}
				if (config == Config.annotation) {
					if (url.toString().contains("spring-init-")) {
						continue;
					}
				}
				list.add(url);
			}
			return list.toArray(new URL[0]);
		}
	}

	@EnableCaching
	@SpringBootConfiguration
	@EnableSelectedAutoConfiguration({ CacheAutoConfiguration.class,
			ConfigurationPropertiesAutoConfiguration.class })
	public static class CacheApplication {

		public static void main(String[] args) throws Exception {
			SpringApplication.run(CacheApplication.class, args);
		}

		@Bean
		public CommandLineRunner runner(ApplicationContext context) {
			return args -> {
				assertThat(context.getBean(CacheManager.class)).isNotNull();
			};
		}

	}

	@SpringBootConfiguration
	@EnableSelectedAutoConfiguration({ ConfigurationPropertiesAutoConfiguration.class })
	public static class EmptyApplication {
		public static void main(String[] args) throws Exception {
			System.setProperty("spring.main.web-application-type", "none");
			SpringApplication.run(EmptyApplication.class, args);
		}
	}

	@EnableCaching
	@SpringBootConfiguration
	@ImportAutoConfiguration({ CacheAutoConfiguration.class,
			ConfigurationPropertiesAutoConfiguration.class })
	public static class AnnoCacheApplication {

		public static void main(String[] args) throws Exception {
			SpringApplication.run(AnnoCacheApplication.class, args);
		}

		@Bean
		public CommandLineRunner runner(ApplicationContext context) {
			return args -> {
				assertThat(context.getBean(CacheManager.class)).isNotNull();
			};
		}

	}

	@EnableCaching
	@SpringBootConfiguration
	@ImportAutoConfiguration(ConfigurationPropertiesAutoConfiguration.class)
	@EnableConfigurationProperties(CacheProperties.class)
	public static class ManualCacheApplication {

		@Autowired
		private CacheProperties cacheProperties;

		public static void main(String[] args) throws Exception {
			System.setProperty("spring.main.web-application-type", "none");
			SpringApplication.run(ManualCacheApplication.class, args);
		}

		@Bean
		public CommandLineRunner runner(ApplicationContext context) {
			return args -> {
				assertThat(context.getBean(CacheManager.class)).isNotNull();
			};
		}

		@Bean
		public ConcurrentMapCacheManager cacheManager() {
			ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
			List<String> cacheNames = this.cacheProperties.getCacheNames();
			if (!cacheNames.isEmpty()) {
				cacheManager.setCacheNames(cacheNames);
			}
			return cacheManager;
		}

	}

	@SpringBootConfiguration
	@ImportAutoConfiguration(ConfigurationPropertiesAutoConfiguration.class)
	public static class AnnoEmptyApplication {
		public static void main(String[] args) throws Exception {
			System.setProperty("spring.main.web-application-type", "none");
			System.setProperty("spring.functional.enabled", "false");
			SpringApplication.run(AnnoEmptyApplication.class, args);
		}
	}
}
