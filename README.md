# Spring Init ![ci](https://github.com/spring-projects-experimental/spring-init/workflows/deploy/badge.svg)

This project implements a code generator (activated by a build plugin) that converts `@Configuration` to functional bean registrations, which are known to be faster and also more amenable to AOT compilation (native images).

It takes this:

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
	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>org.springframework.experimental</groupId>
				<artifactId>spring-init-dependencies</artifactId>
				<version>0.1.0</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

	<dependencies>

		<dependency>
			<groupId>org.springframework.experimental</groupId>
			<artifactId>spring-init-core</artifactId>
		</dependency>

```

and set the code generator up as a compiler plugin:

```
			<plugin>
				<groupId>org.springframework.experimental</groupId>
				<artifactId>spring-init-maven-plugin</artifactId>
				<executions>
					<execution>
						<id>sources</id>
						<phase>process-sources</phase>
						<goals>
							<goal>generate</goal>
						</goals>
					</execution>
				</executions>
			</plugin>

```

To use functional versions of Spring Boot autoconfiguration you can include additional dependencies which have been pre-compiled. The versions are managed by the `spring-init-dependencies` BOM (where each version of Spring Init only supports one version of Spring Boot - 0.1.0 is paired with 2.4.0-M3). An example of including the `func` dependencies in a project that uses `spring-boot-autoconfigure`:

```
		<dependency>
			<groupId>org.springframework.experimental</groupId>
			<artifactId>spring-boot-autoconfigure-func</artifactId>
		</dependency>
```

The convention is that the artifact ID is the same as the Spring Boot dependency with a `-func` suffix, but the group ID is different. There is also a `spring-init-library` with some scraps of support for Spring Cloud.

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

Look at the samples for code to copy. If you are using Eclipse you will need the M2E tooling.

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
 :: Spring Boot ::  (v2.4.0-M3)

...
Bar: com.acme.Bar@7c3ebc6b
```

The command line runner output comes out on stdout after the Spring Boot app has started.

## Building

Before you build, run the `update.sh` script in the "generated" directory. It will clone Spring Boot and all the source code needed to generate initializers from Spring Boot autoconfigurations.

You can then build and run everything from the command line, or from an IDE.

## Benchmarks

Benchmark startup time (seconds) with annotations, with Spring Init (slim), and with manual configuration:

```
class                              method  sample  beans    classes   heap    memory  median  mean   range
com.example.bench.SlimBenchmarkIT  annos   demo    99.000   5022.000  8.418   50.813  0.802   0.817  0.015
com.example.bench.SlimBenchmarkIT  annos   actr    185.000  5282.000  9.901   53.876  0.927   0.937  0.020
com.example.bench.SlimBenchmarkIT  manual  demo    53.000   4729.000  6.996   47.400  0.681   0.696  0.026
com.example.bench.SlimBenchmarkIT  manual  actr    100.000  4917.000  7.764   49.082  0.731   0.753  0.027
com.example.bench.SlimBenchmarkIT  slim    demo    96.000   5020.000  8.113   50.275  0.785   0.797  0.013
com.example.bench.SlimBenchmarkIT  slim    actr    182.000  5416.000  10.081  54.230  0.912   0.920  0.008
```

Spring Init is slightly faster but not much different memorywise than the straight annotation-based version with Spring Boot 2.4. It is a much better story in native images, but that is work in progress still (see "func" and "tunc" samples).

You can generate a condition service, and get to practically no reflection at runtime - you need a `META-INF/services` entry for the infrastructure provider, generated with `closedWorld=true`. The "func" and "tunc" samples both use this approach. The app is a bit faster and uses a bit less memory:

```
class                              method sample  beans    classes   heap   memory  median  mean  range
com.example.bench.SlimBenchmarkIT  slim   actr    182.000  5347.000  9.501  52.914  0.849  0.860  0.018
com.example.bench.SlimBenchmarkIT  manual actr    100.000  4839.000  7.479  48.081  0.700  0.711  0.012
```
