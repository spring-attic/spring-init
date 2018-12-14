package lib.statc;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
public class SampleConfiguration {

	@Configuration
	@Import(AbstractSampleConfiguration.SampleConfiguration.class)
	protected static class NestedConfiguration {
	}
}

