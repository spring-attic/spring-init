/*
 * Copyright 2020-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.init.maven;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.springframework.boot.loader.tools.MainClassFinder;
import org.springframework.init.tools.InitializerApplication;
import org.springframework.util.ClassUtils;

import com.squareup.javapoet.ClassName;

@Mojo(name = "test-generate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresProject = true, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
public class GenerateTestsMojo extends AbstractMojo {

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.SpringBootConfiguration";

	private static final String SPRING_INIT_APPLICATION_CLASS_NAME = "org.springframework.init.tools.InitializerApplication";

	/**
	 * The Maven project.
	 * 
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * Skip the execution.
	 * 
	 * @since 1.3.2
	 */
	@Parameter(property = "spring-init.generate.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * Set the closed world flag - configuration can not be changed with classpath
	 * changes or environment properties.
	 * 
	 * @since 1.3.2
	 */
	@Parameter(property = "spring-init.generate.closed-world", defaultValue = "false")
	private boolean closedWorld;

	/**
	 * The name of the main class. If not specified the first compiled class found
	 * that contains a 'main' method will be used.
	 * 
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-init.generate.main-class")
	private String mainClass;

	/**
	 * The name of the main class. If not specified the first compiled class found
	 * that contains a 'main' method will be used.
	 * 
	 * @since 1.0.0
	 */
	@Parameter(property = "spring-init.generate.base-package")
	private String basePackage;

	/**
	 * Directory containing the classes and resource files that should be scanned.
	 * 
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true)
	private File classesDirectory;

	/**
	 * Directory containing the generated sources.
	 * 
	 * @since 1.0.0
	 */
	@Parameter(defaultValue = "${project.build.directory}/generated-test-sources/init", required = true)
	private File outputDirectory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skip) {
			getLog().debug("skipping run as per configuration.");
			return;
		}
		generate(getStart());
		project.addTestCompileSourceRoot(outputDirectory.getAbsolutePath());
	}

	private void generate(String start) throws MojoExecutionException {
		ClassLoader realm = getClass().getClassLoader();
		ClassLoader parent = realm instanceof ClassRealm ? ((ClassRealm) realm).getParentClassLoader()
				: realm.getParent();
		URLClassLoader loader = new URLClassLoader(getClassPathUrls(), parent);
		ClassLoader original = null;
		getLog().info("Project: " + project);
		try {
			if (closedWorld) {
				getLog().info("Closed world");
				System.setProperty("spring.init.closed-world", "true");
			} else {
				getLog().info("Open world");
			}
			original = ClassUtils.overrideThreadContextClassLoader(loader);
			Class<?> type = loader.loadClass(SPRING_INIT_APPLICATION_CLASS_NAME);
			getLog().info("Generating: " + start + " in: " + outputDirectory);
			type.getMethod("main", String[].class).invoke(null,
					new Object[] { new String[] { start, outputDirectory.getAbsolutePath() } });
		} catch (Exception e) {
			throw new MojoExecutionException("Cannot generate initializer class: " + SPRING_INIT_APPLICATION_CLASS_NAME,
					e);
		} finally {
			System.setProperty("spring.init.closed-world", "false");
			if (original != null) {
				ClassUtils.overrideThreadContextClassLoader(original);
			}
		}
	}

	private String getStart() throws MojoExecutionException {
		String mainClass = this.mainClass;
		if (mainClass == null) {
			if (this.basePackage == null) {
				try {
					mainClass = MainClassFinder.findSingleMainClass(this.classesDirectory,
							SPRING_BOOT_APPLICATION_CLASS_NAME);
				} catch (IOException ex) {
					throw new MojoExecutionException(ex.getMessage(), ex);
				}
			} else {
				return this.basePackage;
			}
		}
		if (mainClass == null) {
			throw new MojoExecutionException("Unable to find a suitable main class, please add a 'mainClass' property");
		}
		return mainClass;
	}

	protected URL[] getClassPathUrls() throws MojoExecutionException {
		try {
			List<URL> urls = new ArrayList<>();
			addProjectClasses(urls);
			addDependencies(urls);
			addTools(urls);
			getLog().debug("Classpath: " + urls);
			return urls.toArray(new URL[0]);
		} catch (IOException ex) {
			throw new MojoExecutionException("Unable to build classpath", ex);
		}
	}

	private void addTools(List<URL> urls) {
		if (!findJar(urls, "spring-init-tools")) {
			urls.add(InitializerApplication.class.getProtectionDomain().getCodeSource().getLocation());
		}
		if (!findJar(urls, "javapoet")) {
			urls.add(ClassName.class.getProtectionDomain().getCodeSource().getLocation());
		}
	}

	private boolean findJar(List<URL> urls, String string) {
		for (URL url : urls) {
			if (url.toString().contains(string)) {
				return true;
			}
		}
		return false;
	}

	private void addProjectClasses(List<URL> urls) throws MalformedURLException {
		urls.add(this.classesDirectory.toURI().toURL());
	}

	private void addDependencies(List<URL> urls) throws MalformedURLException, MojoExecutionException {
		Set<Artifact> artifacts = this.project.getArtifacts();
		for (Artifact artifact : artifacts) {
			if (artifact.getFile() != null) {
				urls.add(artifact.getFile().toURI().toURL());
			}
		}
	}

}
