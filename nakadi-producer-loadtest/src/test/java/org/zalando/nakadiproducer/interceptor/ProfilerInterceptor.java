package org.zalando.nakadiproducer.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.util.StopWatch;

@Aspect
@Slf4j
public class ProfilerInterceptor {

    @Pointcut("execution(* org.zalando.nakadiproducer.transmission.impl.EventTransmissionService.lockSomeEvents(..))")
    private void lockSomeEventsOperation() {
    }

    @Pointcut("execution(* org.zalando.nakadiproducer.transmission.impl.EventTransmissionService.sendEvents(..))")
    private void sendEventOperation() {
    }

    @Around("lockSomeEventsOperation() || sendEventOperation()")
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