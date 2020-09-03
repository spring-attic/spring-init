package org.springframework.init.func;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

public interface InitializerLocator {

	ApplicationContextInitializer<GenericApplicationContext> getInitializer(String name);

}
