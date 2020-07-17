package app.ecp.mthd;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
public class SampleConfiguration {

	@Bean
	@ConfigurationProperties("person.mthd")
	public BarProperties barProperties() {
		return new BarProperties();
	}

    @Bean
    public Foo foo(BarProperties barProperties) {
        return new Foo(barProperties.getName());
    }

}
