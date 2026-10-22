package dk.digitalidentity.OS2ilm.task;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class TaskLoggingAspect {
	@Value("${os2ilm.scheduled.task.durationThreshold:1m}")
	private Duration durationThreshold;

	@Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
	public Object logTask(ProceedingJoinPoint joinPoint) throws Throwable {
		Stopwatch stopwatch = Stopwatch.createStarted();
		try {
			return joinPoint.proceed();
		} finally {
			if(stopwatch.elapsed().compareTo(durationThreshold) > 0) {
				log.warn("Task execution exceeded threshold: task={}, duration={}", joinPoint.getSignature().toShortString(), stopwatch.toString());
			}
		}
	}
	
}
