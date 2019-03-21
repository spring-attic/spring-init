/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.slim.processor.infra;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.function.compiler.java.CompilationResult;
import org.springframework.cloud.function.compiler.java.DependencyResolver;
import org.springframework.cloud.function.compiler.java.InMemoryJavaFileObject;
import org.springframework.cloud.function.compiler.java.InputFileDescriptor;
import org.springframework.core.io.FileUrlResource;
import org.springframework.util.ClassUtils;

/**
 * @author Andy Clement
 */
public class Utils {

	public final static String PROJECT = "org.springframework.experimental";

	private final static Logger logger = LoggerFactory.getLogger(Utils.class);

	public static Map<File, List<File>> resolvedDependenciesCache = new HashMap<>();

	public static Set<String> findImports(InputFileDescriptor sourceFile) {
		Pattern pkg = Pattern.compile("^package.* ([.a-zA-Z]*).*;.*$");
		Pattern p = Pattern.compile("^import.* ([.a-zA-Z]*)\\.[a-zA-Z\\*]*;.*$");
		return Arrays.stream(sourceFile.getContent().split("\\n")).flatMap(l -> {
			Matcher mat = pkg.matcher(l);
			Set<String> s = new HashSet<>();
			while (mat.find()) {
				s.add(mat.group(1));
			}
			mat = p.matcher(l);
			while (mat.find()) {
				s.add(mat.group(1));
			}
			return s.stream();
		}).collect(Collectors.toSet());
	}

	public static File resolveJunitConsoleLauncher() {
		DependencyResolver engine = DependencyResolver.instance();
		Artifact junitLauncherArtifact = new DefaultArtifact(
				"org.junit.platform:junit-platform-console-standalone:1.3.1");
		Dependency junitLauncherDependency = new Dependency(junitLauncherArtifact,
				"test");
		File file = engine.resolve(junitLauncherDependency);
		return file;
	}

	/**
	 * Use maven to resolve the dependencies of the specified folder (expected to contain
	 * a pom), and then resolve them to either entries in the maven cache or as
	 * target/classes folders in other modules in this project build.
	 * 
	 * @param projectRootFolder the project to be resolved (should contain the pom.xml)
	 * @return a list of jars/folders - folders used for local
	 */
	public static List<File> resolveProjectDependencies(File projectRootFolder) {
		List<File> resolvedDependencies = resolvedDependenciesCache
				.get(projectRootFolder);
		if (resolvedDependencies == null) {
			long stime = System.currentTimeMillis();
			DependencyResolver engine = DependencyResolver.instance();
			try {
				File f = new File(projectRootFolder, "pom.xml");
				List<Dependency> dependencies = engine
						.dependencies(new FileUrlResource(f.toURI().toURL()));
				resolvedDependencies = new ArrayList<>();
				for (Dependency dependency : dependencies) {
					File resolvedDependency = null;
					// Example:
					// org.springframework.experimental:tests-lib:jar:1.0-SNAPSHOT
					if (dependency.toString().startsWith(PROJECT + ":")) {
						// Resolve locally
						StringTokenizer st = new StringTokenizer(dependency.toString(),
								":");
						st.nextToken();
						String submodule = st.nextToken();
						if (submodule.startsWith("spring-init-")) {
							submodule = submodule.substring("spring-init-".length());
						}
						resolvedDependency = new File(projectRootFolder,
								"../" + submodule + "/target/classes").getCanonicalFile();
						if (!resolvedDependency.exists()) {
							// try another place
							resolvedDependency = new File(projectRootFolder,
									"../../modules/" + submodule + "/target/classes")
											.getCanonicalFile();
						}
						if (!resolvedDependency.exists()) {
							// try another place
							resolvedDependency = new File(projectRootFolder,
									"../../generated/" + generated(submodule)
											+ "/target/classes").getCanonicalFile();
						}
						if (!resolvedDependency.exists()) {
							System.out.println("Bad miss? "
									+ resolvedDependency.getAbsolutePath().toString());
							resolvedDependency = null;
						}
					}
					if (resolvedDependency == null) {
						resolvedDependency = engine.resolve(dependency);
					}
					resolvedDependencies.add(resolvedDependency);
				}
				logger.debug("Resolved #{} dependencies in {}ms",
						resolvedDependencies.size(),
						(System.currentTimeMillis() - stime));
				resolvedDependenciesCache.put(projectRootFolder,
						Collections.unmodifiableList(resolvedDependencies));
			}
			catch (Throwable e) {
				throw new IllegalStateException(
						"Unexpected problem resolving dependencies", e);
			}
		}
		return resolvedDependencies;
	}

	private static String generated(String submodule) {
		switch (submodule) {
		case "spring-boot-autoconfigure":
			return "autoconfigure";
		case "spring-boot-actuator-autoconfigure":
			return "actuator";
		case "spring-boot-test-autoconfigure":
			return "test";
		default:
			return submodule;
		}
	}

	static class CompilationResultClassLoader extends URLClassLoader {

		private CompilationResult cr;
		File f;

		public CompilationResultClassLoader(List<File> dependencies, CompilationResult cr,
				ClassLoader parent) {
			super(toUrls(dependencies), parent);
			f = dependencies.get(0);
			this.cr = cr;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
			synchronized (getClassLoadingLock(name)) {
				// First, check if the class has already been loaded

				Class<?> c = findLoadedClass(name);
				if (c != null) {
					if (resolve) {
						resolveClass(c);
					}
					return c;
				}
				// Child first...
				try {
					Class<?> findClass = findClass(name);
					if (findClass != null) {
						if (resolve) {
							resolveClass(findClass);
						}
						return findClass;
					}
				}
				catch (ClassNotFoundException ncfe) {
					// ncfe.printStackTrace();
				}
				// Class<?> c = findLoadedClass(name);
				// if (c == null) {
				// if (cr != null) {
				// for (CompiledClassDefinition ccd: cr.getCcds()) {
				// if (ccd.getClassName().equals(name)) {
				// byte[] bs = ccd.getBytes();
				// try {
				// System.out.println("Defining class "+name);
				// c = defineClass(name, bs, 0, bs.length);
				// if (resolve) {
				// resolveClass(c);
				// }
				// return c;
				// } catch (ClassFormatError cfe) {
				// cfe.printStackTrace();
				// break;
				// }
				// }
				// }
				// }

				return super.loadClass(name, resolve);

				// long t0 = System.nanoTime();
				// try {
				// if (parent != null) {
				// c = parent.loadClass(name, false);
				// } else {
				// c = findBootstrapClassOrNull(name);
				// }
				// } catch (ClassNotFoundException e) {
				// // ClassNotFoundException thrown if class not found
				// // from the non-null parent class loader
				// }
				//
				// if (c == null) {
				// // If still not found, then invoke findClass in order
				// // to find the class.
				// long t1 = System.nanoTime();
				// c = findClass(name);
				//
				// // this is the defining class loader; record the stats
				// sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
				// sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
				// sun.misc.PerfCounter.getFindClasses().increment();
				// }
				// }
				// if (resolve) {
				// resolveClass(c);
				// }
				// return c;
			}
		}

		// name = java.lang.String
		@Override
		protected java.lang.Class<?> findClass(String name)
				throws ClassNotFoundException {
			// System.out.println("Looking for " + name);
			if (cr != null) {
				for (InMemoryJavaFileObject ccd : cr.getGeneratedFiles()) {
					if (ccd.getName().equals(name)) {
						byte[] bs = ccd.getBytes();
						try {
							System.out.println("Defining class " + name);
							Class<?> c = defineClass(name, bs, 0, bs.length);
							return c;
						}
						catch (ClassFormatError cfe) {
							cfe.printStackTrace();
							break;
						}
					}
				}
			}
			return super.findClass(name);
		};

		@Override
		public Enumeration<URL> findResources(String name) throws IOException {
			// System.out.println("Asked to find resources: " + name);
			Enumeration<URL> findResources = super.findResources(name);
			// System.out.println("Found any?"+findResources.hasMoreElements());
			// new URL()
			List<URL> ls = new ArrayList<>();
			if (!findResources.hasMoreElements() && cr != null) {
				for (InMemoryJavaFileObject ccd : cr.getGeneratedFiles()) {
					if (ccd.getName().equals(name)) {
						// byte[] bs = ccd.getBytes();
						URL url = new URL(
								"jar:file:/" + f.toString() + "!/" + ccd.getName());
						System.out.println(url);
						// try {
						// System.out.println("Defining class "+name);
						// Class<?> c = defineClass(name, bs, 0, bs.length);
						// return c;
						// } catch (ClassFormatError cfe) {
						// cfe.printStackTrace();
						// break;
						// }
					}
				}
			}
			return ls.size() == 0 ? findResources : Collections.enumeration(ls);
			// return findResources;
		}

		@Override
		public URL findResource(String name) {
			// System.out.println("Asked to find resource " + name);
			// for (CompiledClassDefinition ccd: cr.getCcds()) {
			//// if (ccd.getClassName().equals(name)) {
			// System.out.println(" - checking "+ccd.getClassName());
			//// }
			// }
			// if (true) throw new IllegalStateException("!!"+name);
			return super.findResource(name);
		};

		private static URL[] toUrls(List<File> dependencies) {
			return dependencies.stream().map(f -> {
				try {
					return f.toURI().toURL();
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
					return null;
				}
			}).collect(Collectors.toList()).toArray(new URL[0]);
		}

	}

	public static ClassLoader getCompilationResultClassLoader(List<File> dependencies,
			CompilationResult result, ClassLoader parent) {
		return new CompilationResultClassLoader(dependencies, result, parent);
	}

	public static Map<File, List<InputFileDescriptor>> filesCache = new HashMap<>();

	public static List<InputFileDescriptor> getFiles(File rootFolder) {
		List<InputFileDescriptor> sourcesInFolder = filesCache.get(rootFolder);
		if (sourcesInFolder == null) {
			final List<InputFileDescriptor> collectedFiles = new ArrayList<>();
			try {
				Files.walkFileTree(rootFolder.toPath(), new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
							throws IOException {
						File relativeFile = rootFolder.toPath().relativize(file).toFile();
						collectedFiles.add(new InputFileDescriptor(file.toFile(),
								relativeFile.toString(), guessClassName(
										rootFolder.toPath().relativize(file).toFile())));
						return super.visitFile(file, attrs);
					}

					private String guessClassName(File file) {
						String s = file.toString();
						if (s.endsWith(".java")) {
							return s.substring(0, s.length() - 5).replace("/", ".");
						}
						else {
							return null;
						}
					}
				});
			}
			catch (NoSuchFileException nsfe) {
			}
			catch (IOException e) {
				throw new IllegalStateException("Problems walking folder: " + rootFolder,
						e);
			}
			sourcesInFolder = Collections.unmodifiableList(collectedFiles);
			filesCache.put(rootFolder, sourcesInFolder);
		}
		return sourcesInFolder;
	}

	public static void executeTests(CompilationResult result, List<File> dependencies,
			InputFileDescriptor... testFiles) {
		try {
			List<File> fullDependencies = new ArrayList<>(dependencies);
			fullDependencies.add(resolveJunitConsoleLauncher());
			ClassLoader cl = getCompilationResultClassLoader(fullDependencies, result,
					Thread.currentThread().getContextClassLoader().getParent());
			Thread.currentThread().setContextClassLoader(cl);

			Class<?> junitLauncher = ClassUtils
					.forName("org.junit.platform.console.ConsoleLauncher", cl);
			// Call execute rather than main to avoid process exit
			Method declaredMethod = junitLauncher.getDeclaredMethod("execute",
					PrintStream.class, PrintStream.class, String[].class);
			// Type of o is ConsoleLauncherExecutionResult
			List<String> options = new ArrayList<>();
			for (InputFileDescriptor testFile : testFiles) {
				options.add("-c");
				options.add(testFile.getClassName());
			}
			options.add("--details");
			options.add("tree");
			System.out.println(options);
			Object o = declaredMethod.invoke(null, System.out, System.err,
					options.toArray(new String[] {}));
			// (Object) new String[] { "-c", testSourceFile.getClassName(), "--details",
			// "none" });
			Method getExitCode = ClassUtils
					.forName("org.junit.platform.console.ConsoleLauncherExecutionResult",
							cl)
					.getDeclaredMethod("getExitCode");
			Integer i = (Integer) getExitCode.invoke(o);
			if (i != 0) {
				throw new IllegalStateException("Test failed");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("Failed", e);
		}
	}

}
