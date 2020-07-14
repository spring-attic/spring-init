package org.springframework.init.func;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;

public class InfrastructureListener implements SmartApplicationListener {

	private static Log logger = LogFactory.getLog(InfrastructureListener.class);

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return ApplicationReadyEvent.class.isAssignableFrom(eventType);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		GenericApplicationContext context = InfrastructureUtils
				.getContext(((ApplicationReadyEvent) event).getApplicationContext().getBeanFactory());
		if (context != null) {
			try {
				// TODO: DO we need this?
				context.close();
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to close infrastructure context", e);
				} else {
					logger.info("Failed to close infrastructure context");
				}
			}
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 5;
	}

}
