package com.swiggy.ff.reminder.service.util.aspects;

import io.prometheus.client.Histogram;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import static com.swiggy.ff.reminder.service.util.aspects.InstrumentationUtil.*;

@Component
@Aspect
public class InstrumentQueryAspect {

    @Around("@annotation(instrumentQuery)")
    public Object instrumentDbQueryAdvice(ProceedingJoinPoint joinPoint, InstrumenQuery instrumentQuery) throws Throwable {
        String queryName = instrumentQuery.name();
        Histogram.Timer timer = queryTotal.labels(instrumentDbQuery.name()).startTimer();
        dbQueryTotal.labels(queryName, Status.INVOCATION.name(), "").inc();
        dbQueryInProgress.labels(queryName).inc();
        try {
            Object object = joinPoint.proceed();
            queryTotal.labels(queryName, Status.SUCCESS.name(), "").inc();
            return object;
        } catch (Throwable t) {
            queryTotal.labels(queryName, Status.FAILURE.name(), "").inc();
            throw t;
        } finally {
            queryInProgress.labels(queryName).dec();
            timer.observeDuration();
        }
    }
}