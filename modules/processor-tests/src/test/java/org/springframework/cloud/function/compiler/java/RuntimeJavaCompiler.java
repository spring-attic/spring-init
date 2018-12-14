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

package org.springframework.cloud.function.compiler.java;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compile Java source at runtime and load it.
 *
 * @author Andy Clement
 */
public class RuntimeJavaCompiler {

	private static Logger logger = LoggerFactory.getLogger(RuntimeJavaCompiler.class);

	private JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

	public CompilationResult compile(InputFileDescriptor[] sources, InputFileDescriptor[] resources, CompilationOptions compilationOptions, List<File> dependencies) {
		logger.debug("Compiling {} source{}",sources.length, sources.length==1?"":"s");
		DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
		MemoryBasedJavaFileManager fileManager = new MemoryBasedJavaFileManager();
		fileManager.addResolvedDependencies(dependencies);
		List<JavaFileObject> compilationUnits = new ArrayList<>();
		for (InputFileDescriptor source: sources) {
			compilationUnits.add(InMemoryJavaFileObject.getSourceJavaFileObject(source.getClassName(), source.getContent()));
		}
		List<String> options = new ArrayList<>();
		options.add("-processorpath");
		options.add(new File("../processor/target/classes/").toString());
		options.add("-source");
		options.add("1.8");
		options.add("-processor");
		options.add("processor.SlimConfigurationProcessor");
		CompilationTask task = compiler.getTask(null, fileManager , diagnosticCollector, options,  null, compilationUnits);
		boolean success = task.call();
		CompilationResult compilationResult = new CompilationResult(success);
		compilationResult.setDependencies(new ArrayList<>(fileManager.getResolvedAdditionalDependencies().values()));
		compilationResult.setInputResources(resources);
		// If successful there may be no errors but there might be info/warnings
		for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
			CompilationMessage.Kind kind = (diagnostic.getKind()==Kind.ERROR?CompilationMessage.Kind.ERROR:CompilationMessage.Kind.OTHER);
			String sourceCode =null;
			try {
				sourceCode = (String)diagnostic.getSource().getCharContent(true);
			}
			catch (IOException ioe) {
				// Unexpected, but leave sourceCode null to indicate it was not retrievable
			}
			catch (NullPointerException npe) {
				// TODO: should we skip warning diagnostics in the loop altogether?
			}
			int startPosition = (int)diagnostic.getPosition();
			if (startPosition == Diagnostic.NOPOS) {
				startPosition = (int)diagnostic.getStartPosition();
			}
			CompilationMessage compilationMessage = new CompilationMessage(kind,diagnostic.getMessage(null),sourceCode,startPosition,(int)diagnostic.getEndPosition());
			compilationResult.recordCompilationMessage(compilationMessage);
		}
		compilationResult.setGeneratedFiles(fileManager.getOutputFiles());
		return compilationResult;
	}
}
