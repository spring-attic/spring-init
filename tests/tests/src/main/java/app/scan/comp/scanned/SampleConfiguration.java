package app.scan.comp.scanned;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleConfiguration {

    @Value("${app.value}")
    private String message;

    @Bean
    public Bar bar(Foo foo) {
        return new Bar(foo);
    }

    @Bean
    public CommandLineRunner runner(Bar bar) {
        return args -> {
            System.out.println("Message: " + message);
            System.out.println("Bar: " + bar);
            System.out.println("Foo: " + bar.getFoo());
        };
    }

}
