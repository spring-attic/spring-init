/*
 * Copyright 2016-2018 the original author or authors.
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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

/**
 * Holder for the results of compilation. If compilation was successful the set
 * of classes that resulted from compilation will be available. If compilation
 * was not successful the error messages should provide information about why.
 * Note that compilation may succeed and yet there will still be informational
 * or warning messages collected.
 * 
 * @author Andy Clement
 * @author Mark Fisher
 */
public class CompilationResult {

	private boolean successfulCompilation;

	private List<CompilationMessage> compilationMessages = new ArrayList<>();

	private List<InputFileDescriptor> resources = new ArrayList<>();

	private List<File> dependencies;

	private List<InMemoryJavaFileObject> generatedFiles;

	public CompilationResult(boolean successfulCompilation) {
		this.successfulCompilation = successfulCompilation;
	}

	public void setDependencies(List<File> dependencies) {
		this.dependencies = dependencies;
	}

	public List<File> getDependencies() {
		return this.dependencies;
	}

	public boolean wasSuccessful() {
		return successfulCompilation;
	}

	public void recordCompilationMessage(CompilationMessage message) {
		this.compilationMessages.add(message);
	}

	public void recordCompilationMessages(List<CompilationMessage> messages) {
		this.compilationMessages.addAll(messages);
	}

	public List<CompilationMessage> getCompilationMessages() {
		return Collections.unmodifiableList(compilationMessages);
	}

	public String toString() {
		return "Compilation Result: #generatedFiles="+generatedFiles.size()+" #messages="+compilationMessages.size();
	}

	public void setGeneratedFiles(List<InMemoryJavaFileObject> generatedFiles) {
		this.generatedFiles = generatedFiles;
	}

	public List<InMemoryJavaFileObject> getGeneratedFiles() {
		return generatedFiles;
	}

	public File dumpToTemporaryJar() {
		File f = null;
		try {
			f = File.createTempFile("compiledoutput-", ".jar");
			System.out.println("Writing generated output to " + f);
			f.delete();
			JarOutputStream jos = new JarOutputStream(new FileOutputStream(f));
			Set<String> packages = new HashSet<>();
			for (InMemoryJavaFileObject ccd : generatedFiles) {
				// spring.factories written twice, first time no content... dig into that later
				if (ccd.getBytes()== null) continue;
				String entryName = ccd.getName().startsWith("/")?ccd.getName().substring(1):ccd.getName(); // remove leading slash
				if (ccd.getKind() == Kind.CLASS) {
//					entryName = entryName.replace(".", "/");
				}
				String pkg = entryName.substring(0, entryName.lastIndexOf("/") + 1);
				packages.add(pkg);
				JarEntry ze = new JarEntry(entryName);
				jos.putNextEntry(ze);
				byte[] bs = ccd.getBytes();
				jos.write(bs, 0, bs.length);
				jos.closeEntry();
			}
			for (InputFileDescriptor resource: resources) {
				JarEntry ze = new JarEntry(resource.getName());
				jos.putNextEntry(ze);
				byte[] bs = resource.getContent().getBytes();
				jos.write(bs, 0, bs.length);
				jos.closeEntry();
			}
			for (String pkg : packages) {
				JarEntry ze = new JarEntry(pkg);
				jos.putNextEntry(ze);
				jos.closeEntry();
			}
			jos.close();
			f.deleteOnExit();
		} catch (Throwable t) {
			throw new IllegalStateException("Problem storing compilation result",t);
		}
		return f;
	}

	public void setInputResources(InputFileDescriptor[] resources) {
		this.resources = Arrays.asList(resources);
	}

	public String getGeneratedFileContents(String name) {
		for (InMemoryJavaFileObject generatedFile: generatedFiles) {
			if (generatedFile.getName().equals("/"+name)) {
				return generatedFile.getContent();
			}
		}
		return null;
	}

	public boolean containsNewFile(String name) {
		return generatedFiles.stream().anyMatch(generatedFile -> generatedFile.getName().equals("/"+name));
	}

	public void printGeneratedSources(PrintStream p) {
		System.out.println("Sources and resources generated during compilation:");
		for (InMemoryJavaFileObject ccd: generatedFiles) {
			if (ccd.getKind() == JavaFileObject.Kind.CLASS) {
				continue;
			}
			StringBuilder sb = new StringBuilder();
			sb.append("=====8<===== ").append(ccd.getName()).append(" =====8<=====");
			p.println(sb.toString());
			p.println(ccd.getContent());
			for (int i=0;i<sb.length();i++) p.print("=");
			p.println();
		}
	}

}
