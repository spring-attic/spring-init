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
import java.nio.file.Files;

/**
 * Representing a file that is an input to compilation - either a source or a
 * resource file.
 * 
 * @author Andy Clement
 */
public class InputFileDescriptor {

	private String name;
	private String classname;
	private String content;

	public InputFileDescriptor(File file, String name, String classname) {
		try {
			content = new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load " + file, e);
		}
		this.name = name;
		this.classname = classname;
	}

	public InputFileDescriptor(String classname, String content) {
		this.content = content;
		this.name = classname;
		this.classname = classname;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof InputFileDescriptor) && ((InputFileDescriptor) obj).getClassName().equals(classname);
	}

	public String getClassName() {
		return classname;
	}

	public String getContent() {
		return content;
	}

	public String toString() {
		return "Source[" + classname + "]";
	}

	public String getPackageName() {
		int idx = classname.lastIndexOf(".");
		if (idx == -1) {
			return "";
		} else {
			return classname.substring(0, idx);
		}
	}

	public String getName() {
		return this.name;
	}

}
