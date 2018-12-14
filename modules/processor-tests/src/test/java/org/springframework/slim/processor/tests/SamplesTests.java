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
package org.springframework.slim.processor.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import org.springframework.cloud.function.compiler.java.CompilationResult;
import org.springframework.cloud.function.compiler.java.InputFileDescriptor;
import org.springframework.slim.processor.infra.CompilerRunner;
import org.springframework.slim.processor.infra.Utils;

/**
 * Find the tests in the samples/XXX folder, pull together the related sources and compile
 * and run the tests. The tests within a sample are run together under one generated test.
 * 
 * @author Andy Clement
 */
@EnabledIfSystemProperty(named = "generatedTests", matches = "true")
public class SamplesTests {

	private static File samplesFolder = new File("../../samples");

	@TestFactory
	Stream<DynamicTest> samples() {
		return Arrays.asList(samplesFolder.listFiles()).stream()
				.filter(f -> f.isDirectory() && new File(f, "pom.xml").exists())
				.map(f -> DynamicTest.dynamicTest(f.getName(), () -> runTest(f)));
	}

	private static void runTest(File sampleFolder) {
		List<InputFileDescriptor> testFiles = Utils
				.getFiles(new File(sampleFolder, "src/test/java")).stream()
				.filter(f -> f.getClassName().endsWith("Tests"))
				.collect(Collectors.toList());
		Set<InputFileDescriptor> inputForCompiler = new HashSet<>();
		inputForCompiler.addAll(Utils.getFiles(new File(sampleFolder, "src/main/java")));
		inputForCompiler.addAll(Utils.getFiles(new File(sampleFolder, "src/test/java")));
		Set<InputFileDescriptor> resources = new HashSet<>();
		resources.addAll(Utils.getFiles(new File(sampleFolder, "src/main/resources")));
		resources.addAll(Utils.getFiles(new File(sampleFolder, "src/test/resources")));
		long stime = System.currentTimeMillis();
		List<File> junitDeps = Utils.resolveProjectDependencies(new File(".")).stream()
				.filter(d -> d.toString().contains("junit")).collect(Collectors.toList());
		System.out.println(
				"Timer: resolve deps: #" + (System.currentTimeMillis() - stime) + "ms");
		stime = System.currentTimeMillis();
		List<File> dependencies = new ArrayList<>(
				Utils.resolveProjectDependencies(sampleFolder));
		System.out.println(
				"Timer: resolve deps2: #" + (System.currentTimeMillis() - stime) + "ms");
		dependencies.addAll(junitDeps);
		System.out.println("For sample " + sampleFolder);
		System.out.println(" - compiling #" + inputForCompiler.size() + " sources");
		System.out.println(" - resources #" + resources.size() + " files");
		System.out.println(" - dependencies #" + dependencies.size());
		System.out.println(dependencies);
		stime = System.currentTimeMillis();
		CompilationResult result = CompilerRunner.run(inputForCompiler, resources,
				dependencies);
		System.out.println(
				"Timer: compiler run: #" + (System.currentTimeMillis() - stime) + "ms");
		dependencies.add(0, result.dumpToTemporaryJar());
		stime = System.currentTimeMillis();
		Utils.executeTests(result, dependencies,
				testFiles.toArray(new InputFileDescriptor[] {}));
		System.out.println(
				"Timer: running tests: #" + (System.currentTimeMillis() - stime) + "ms");
	}

}