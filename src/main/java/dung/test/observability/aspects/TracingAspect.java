package dung.test.observability.aspects;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TracingAspect {
    private final Tracer tracer;

    @Around("@annotation(dung.test.observability.annotation.Traced) || execution(* dung.test.observability.controller.*.*(..))")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String spanName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." +
                joinPoint.getSignature().getName();

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope _ = span.makeCurrent()) {
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && isSimpleType(args[i].getClass())) {
                    span.setAttribute("arg." + i, args[i].toString());
                }
            }

            // Execute the actual method
            Object result = joinPoint.proceed();

            // Add success attribute
            span.setAttribute("outcome", "SUCCESS");
            return result;
        } catch (Throwable t) {
            span.recordException(t);
            span.setAttribute("outcome", "ERROR");
            throw t;
        } finally {
            span.end();
        }
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.equals(String.class) ||
                Number.class.isAssignableFrom(clazz) ||
                Boolean.class.equals(clazz);
    }
}
