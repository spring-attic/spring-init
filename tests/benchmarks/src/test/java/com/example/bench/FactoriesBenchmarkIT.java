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
package com.example.bench;

import com.example.cust.CustomizerApplication;
import com.example.factory.FactoriesApplication;
import com.example.init.InitApplication;
import com.example.mixt.MixtApplication;
import com.example.none.NoneApplication;

import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.AuxCounters.Type;
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

import org.springframework.init.bench.ProcessLauncherState;

import jmh.mbr.junit5.Microbenchmark;

@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 1, time = 1)
@Fork(value = 2, warmups = 0)
@BenchmarkMode(Mode.AverageTime)
@Microbenchmark
public class FactoriesBenchmarkIT {

	@Benchmark
	public void init(MainState state) throws Exception {
		state.run();
	}

	@State(Scope.Thread)
	@AuxCounters(Type.EVENTS)
	public static class MainState extends ProcessLauncherState {

		public static enum Sample {
			none, fact, demo, cust; // , mixt;
		}

		@Param
		Sample sample = Sample.demo;

		public MainState() {
			super(InitApplication.class, "target", "--server.port=0");
			addArgs("-Dspring.functional.enabled=false");
			setProfiles("slim");
		}

		@Override
		public int getClasses() {
			return super.getClasses();
		}

		@Override
		public int getBeans() {
			return super.getBeans();
		}

		@TearDown(Level.Invocation)
		public void stop() throws Exception {
			super.after();
		}

		@Setup(Level.Trial)
		public void start() throws Exception {
			if (sample == Sample.fact) {
				setMainClass(FactoriesApplication.class.getName());
			}
			else if (sample == Sample.none) {
				setMainClass(NoneApplication.class.getName());
			}
			else if (sample == Sample.cust) {
				setMainClass(CustomizerApplication.class.getName());
			}
			else if (sample != Sample.demo) {
				setMainClass(MixtApplication.class.getName());
			}
			super.before();
		}
	}

}
