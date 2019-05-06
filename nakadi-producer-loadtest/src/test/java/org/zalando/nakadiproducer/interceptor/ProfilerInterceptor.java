package org.zalando.nakadiproducer.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.StopWatch;

@Aspect
@Slf4j
public class ProfilerInterceptor {

    @Around("execution(* org.zalando.nakadiproducer.transmission.impl.EventTransmissionService.lockSomeEvents(..)) || " +
            "execution(* org.zalando.nakadiproducer.transmission.impl.EventTransmissionService.sendEvents(..))")
    public Object profile(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        StopWatch clock = new StopWatch("Profiling for " + proceedingJoinPoint.toShortString());
        try {
            clock.start(proceedingJoinPoint.toShortString());
            return proceedingJoinPoint.proceed();
        } finally {
            clock.stop();
            log.info(clock.prettyPrint());
        }
    }
}