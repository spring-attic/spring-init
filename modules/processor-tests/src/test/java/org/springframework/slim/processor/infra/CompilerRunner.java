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
package org.springframework.slim.processor.infra;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.function.compiler.java.CompilationMessage;
import org.springframework.cloud.function.compiler.java.CompilationOptions;
import org.springframework.cloud.function.compiler.java.CompilationResult;
import org.springframework.cloud.function.compiler.java.InputFileDescriptor;
import org.springframework.cloud.function.compiler.java.InMemoryJavaFileObject;
import org.springframework.cloud.function.compiler.java.RuntimeJavaCompiler;

/**
 * @author Andy Clement
 */
public class CompilerRunner {

	public static CompilationResult run(InputFileDescriptor fd) {
		List<InputFileDescriptor> sources = new ArrayList<>();
		sources.add(fd);
		return run(sources,Collections.emptyList(), Collections.emptyList());
	}

	public static CompilationResult run(InputFileDescriptor fd, List<File> dependencies) {
		List<InputFileDescriptor> sources = new ArrayList<>();
		sources.add(fd);
		return run(sources,Collections.emptyList(), dependencies);
	}

	public static CompilationResult run(Collection<InputFileDescriptor> sources, Collection<InputFileDescriptor> resources, List<File> dependencies) {
		RuntimeJavaCompiler compiler = new RuntimeJavaCompiler();
		CompilationOptions options = new CompilationOptions();
		boolean hasErrors = false;
		System.out.println("Starting compiler...");
		CompilationResult result = compiler.compile(sources.toArray(new InputFileDescriptor[0]),resources.toArray(new InputFileDescriptor[0]), options, dependencies);
		List<CompilationMessage> compilationMessages = result.getCompilationMessages();
		for (CompilationMessage compilationMessage : compilationMessages) {
			if (compilationMessage.getKind().toString().equals("ERROR")) {
				hasErrors = true;
			}
		}
		if (hasErrors) {
			throw new IllegalStateException("Compilation failed, see errors");
		} else {
			System.out.println("Compilation completed OK");
		}
		// printCompilationSummary(result);
		return result;
	}

	@SuppressWarnings("unused")
	private static void printCompilationSummary(CompilationResult cr) {
		List<InMemoryJavaFileObject> compiledClasses = cr.getGeneratedFiles();
		if (compiledClasses == null) {
			System.out.println("NO CLASSES COMPILED");
			return;
		}
		System.out.println("Output files:");
		for (InMemoryJavaFileObject compiledClassDefinition : compiledClasses) {
			byte[] bytes = compiledClassDefinition.getBytes();
			System.out.println(compiledClassDefinition.getName() + " size:" + (bytes == null ? "NULL" : bytes.length));
		}
	}

}
