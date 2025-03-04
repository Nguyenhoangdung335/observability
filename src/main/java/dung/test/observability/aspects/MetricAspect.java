package dung.test.observability.aspects;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class MetricAspect {
    private final Meter meter;

    @Around("@annotation(dung.test.observability.annotation.Measured) || @within(dung.test.observability.annotation.Measured)")
    public Object measureMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();

        // Create a counter for method invocations
        LongCounter invocationCounter = meter
                .counterBuilder(className + "." + methodName + ".invocations")
                .setDescription("Number of invocations")
                .build();

        // Create a histogram for method execution time
        DoubleHistogram executionTimeHistogram = meter
                .histogramBuilder(className + "." + methodName + ".execution_time")
                .setDescription("Method execution time")
                .setUnit("ms")
                .build();

        // Record start time
        long startTime = System.currentTimeMillis();

        try {
            // Execute method
            Object result = joinPoint.proceed();

            // Record successful invocation
            invocationCounter.add(1, Attributes.of(
                    AttributeKey.stringKey("outcome"), "SUCCESS"
            ));

            return result;
        } catch (Throwable t) {
            // Record failed invocation
            invocationCounter.add(1, Attributes.of(
                    AttributeKey.stringKey("outcome"), "ERROR",
                    AttributeKey.stringKey("error.type"), t.getClass().getSimpleName()
            ));
            throw t;
        } finally {
            // Record execution time
            long executionTime = System.currentTimeMillis() - startTime;
            executionTimeHistogram.record(executionTime);
        }
    }
}
