package aspects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class BootApplicationInterceptor {

	private static Log logger = LogFactory.getLog(BootApplicationInterceptor.class);

	@Around("execution(* org.springframework.boot.system.ApplicationHome.findSource(..))")
	public Object source(ProceedingJoinPoint joinPoint) throws Throwable {
		return proceed(joinPoint);
	}

	@Around("execution(* org.springframework.boot.system.ApplicationHome.getStartClass(..))")
	public Object start(ProceedingJoinPoint joinPoint) throws Throwable {
		return proceed(joinPoint);
	}

	@Around("execution(* org.springframework.boot.StartupInfoLogger.getValue(..))")
	public Object getValue(ProceedingJoinPoint joinPoint) throws Throwable {
		Object result = proceed(joinPoint);
		Object defaultValue = null;
		if (joinPoint.getArgs().length>2) {
			defaultValue = joinPoint.getArgs()[2];
		}
		return result != null ? result : defaultValue;
	}

	private Object proceed(ProceedingJoinPoint joinPoint) {
		try {
			Object result = joinPoint.proceed();
			if (logger.isDebugEnabled()) {
				logger.debug(joinPoint.toShortString() + ": " + result);
			}
			return result;
		}
		catch (Throwable t) {
			if (logger.isDebugEnabled()) {
				logger.debug(joinPoint.toShortString() + ": " + t);
			}
			return null;
		}
	}

}
