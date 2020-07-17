package app.ecp.cls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BarProperties.class)
public class SampleConfiguration {

	@Autowired
	BarProperties barProperties;

    @Bean
    public Foo foo() {
        return new Foo(barProperties.getName());
    }

}
