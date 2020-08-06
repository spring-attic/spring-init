package app.main;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.init.func.InfrastructureProvider;
import org.springframework.init.func.InfrastructureUtils;
import org.springframework.init.func.PropertiesBinder;

public class PropertiesBinderInfrastructureProvider implements InfrastructureProvider {

	@Override
	public Collection<? extends ApplicationContextInitializer<GenericApplicationContext>> getInitializers(
			GenericApplicationContext main) {
		return Arrays.asList(context -> InfrastructureUtils.registerPropertyBinder(context, ServerProperties.class,
				new ServerPropertiesPropertiesBinder()));
	}

}

class ServerPropertiesPropertiesBinder implements PropertiesBinder<ServerProperties> {

	@Override
	public ServerProperties bind(ServerProperties bean, Environment environment) {
		bean.getServlet().setRegisterDefaultServlet(false);
		bean.setPort(environment.getProperty("server.port", Integer.class,
				environment.getProperty("SERVER_PORT", Integer.class, 8080)));
		return bean;
	}

}