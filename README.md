This project implements an APT plugin that converts `@Configuration` to functional bean registrations, which are known to be faster and also more amenable to AOT compilation (native image building).

Takes this:

```java
@Configuration
public class SampleConfiguration {

    @Bean
    public Foo foo() {
        return new Foo();
    }

    @Bean
    public Bar bar(Foo foo) {
        return new Bar(foo);
    }
    
}
```

and transforms it to this:

```java
public class SampleConfigurationInitializer 
      implements ApplicationContextInitializer<GenericApplicationContext> {

    @Override
    public void initialize(GenericApplicationContext context) {
        context.registerBean( SampleConfiguration.class);
        context.registerBean(Foo.class, () -> context.getBean(SampleConfiguration.class).foo());
        context.registerBean(Bar.class, () -> context.getBean(SampleConfiguration.class).bar(context.getBean(Foo.class)));
    }
    
}
```

You then need a Spring application bootstrap utility that recognizes the `ApplicationContextInitializer` and treats it in a special way.  This is provided in the modules of this project, so just add them as dependencies:

```
		<dependency>
			<groupId>org.springframework.experimental</groupId>
			<artifactId>spring-init-core</artifactId>
			<version>${init.version}</version>
		</dependency>

```

and set the APT processor up as a compiler plugin:

```
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<configuration>
					<annotationProcessorPaths>
						<path>
							<groupId>org.springframework.experimental</groupId>
							<artifactId>spring-init-processor</artifactId>
							<version>${init.version}</version>
						</path>
					</annotationProcessorPaths>
				</configuration>
			</plugin>

```

To use functional versions of Spring Boot autoconfiguration you need to include additional dependencies, for example (with `generated.version=2.1.1.BUILD-SNAPSHOT` for Spring Boot 2.1.1):

```
		<dependency>
			<groupId>org.springframework.experimental</groupId>
			<artifactId>spring-boot-autoconfigure</artifactId>
			<version>${generated.version}</version>
            <classifier>func</classifier>
		</dependency>
```

The convention is that the artifact ID is the same as the Spring Boot dependency, but the group ID is different and the classifier is "func" (to make the jar file names unique). There is also a `spring-init-library` with some scraps of support for Spring Cloud.

You can use `@Import` or `@ImportAutoConfiguration` (more idiomatically) to include existing autoconfigurations in your app. Example (a simple, non-web application):

```java
@SpringBootConfiguration
@ComponentScan
@ImportAutoConfiguration({ ConfigurationPropertiesAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class })
public class SampleApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SampleApplication.class);
		app.run(args);
	}

}
```

Look at the samples for code to copy. If you are using Eclipse you will need the M2E APT plugin.

Build and run:

```
$ ./mvnw package
$ java -jar samples/application/target/*.jar
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::  (v2.1.0.RELEASE)

...
Bar: com.acme.Bar@7c3ebc6b
```

The command line runner output comes out on stdout after the Spring Boot app has started.

## Building

Before you build, run the `update.sh` script in the "generated" directory. It will clone Spring Boot and all the source code needed to generate initializers from Spring Boot autoconfigurations.

You can then build and run everything from the command line, or from an IDE. If you are using [Eclipse](https://github.com/jbosstools/m2e-apt/issues/64), you need to disable the Maven project nature for the "processor" project (or work with it in a different workspace). It only has 2 dependencies, so it's quite easy to manually set up the classpath for that project (then you have to build it on the command line before you refresh the sample apps).

You can debug the APT processor by running on the command line and attaching a debugger, e.g:

```
(cd modules/tests-lib/; MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" mvn clean compile)
```

## Benchmarks

Benchmark startup time (seconds) with annotations, with Spring Init (slim), and with manual configuration:

```
class                              method  sample  beans    classes   heap   memory  median  mean   range
com.example.bench.SlimBenchmarkIT  annos   jlog    85.000   4185.000  6.227  43.667  0.724   0.740  0.017
com.example.bench.SlimBenchmarkIT  annos   demo    98.000   4962.000  8.255  50.530  0.837   0.855  0.028
com.example.bench.SlimBenchmarkIT  annos   actr    183.000  5224.000  9.812  53.737  0.986   1.009  0.025
com.example.bench.SlimBenchmarkIT  annos   conf    181.000  5601.000  8.201  55.619  1.179   1.242  0.057
com.example.bench.SlimBenchmarkIT  manual  jlog    56.000   4207.000  6.648  43.372  0.646   0.662  0.023
com.example.bench.SlimBenchmarkIT  manual  demo    56.000   4932.000  7.501  48.560  0.725   0.741  0.024
com.example.bench.SlimBenchmarkIT  manual  actr    103.000  5320.000  8.272  50.886  0.826   0.844  0.016
com.example.bench.SlimBenchmarkIT  manual  conf    74.000   4992.000  7.732  49.243  0.830   0.869  0.033
com.example.bench.SlimBenchmarkIT  slim    jlog    87.000   4416.000  6.334  44.230  0.729   0.737  0.018
com.example.bench.SlimBenchmarkIT  slim    demo    100.000  5207.000  8.482  51.243  0.841   0.855  0.020
com.example.bench.SlimBenchmarkIT  slim    actr    185.000  5734.000  8.515  53.691  1.007   1.022  0.021
com.example.bench.SlimBenchmarkIT  slim    conf    117.000  5290.000  9.215  52.639  0.977   0.993  0.024
```

It's not obvious that Spring Init is any faster or uses less memory than the straight annotation-based version with Spring Boot 2.3. It might be a better story in native images, but that remains to be seen.
