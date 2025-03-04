package dung.test.observability.aspects;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class LoggingAspect {

    @Before("execution(* dung.test.observability.*.*(..)) && !within(dung.test.observability.config..*)")
    public void addTraceIdToLogs(JoinPoint joinPoint) {
        // Get the current span
        Span currentSpan = Span.current();

        // Extract trace and span IDs
        String traceId = currentSpan.getSpanContext().getTraceId();
        String spanId = currentSpan.getSpanContext().getSpanId();

        // Add them to the MDC context for logging
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);

        // Get the logger for the current class
        Logger logger = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringType());

        // Log method entry with context information
        logger.debug("Entering: {}.{}() with arguments: {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                joinPoint.getArgs());
    }

    @Around("execution(* dung.test.observability.controller.*.*(..))")
    public Object logControllerEntry(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger logger = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringType());

        // Get HTTP request details
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        assert attributes != null;
        HttpServletRequest request = attributes.getRequest();

        // Extract request information
        String httpMethod = request.getMethod();
        String uri = request.getRequestURI();
        String queryParams = request.getQueryString() != null ? request.getQueryString() : "";

        // Log special headers
        Map<String, String> specialHeaders = new HashMap<>();
        for (String headerName : Arrays.asList("User-Agent", "Content-Type", "Accept", "Authorization")) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null) {
                // Mask sensitive headers like Authorization
                specialHeaders.put(headerName, "Authorization".equals(headerName) ? "****" : headerValue);
            }
        }

        // Log the API request
        logger.info("API REQUEST: {} {} | Query: {} | Headers: {} | Method: {}.{}()",
                httpMethod, uri, queryParams, specialHeaders,
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());

        // Record start time
        long startTime = System.currentTimeMillis();

        try {
            // Execute the method
            Object result = joinPoint.proceed();

            // Log response with execution time
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("API RESPONSE: {} {} completed in {} ms | Status: SUCCESS",
                    httpMethod, uri, executionTime);

            return result;
        } catch (Throwable t) {
            // Log exception with execution time
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("API RESPONSE: {} {} failed in {} ms | Status: ERROR | Exception: {}",
                    httpMethod, uri, executionTime, t.getMessage());
            throw t;
        } finally {
            // Clear MDC context
            MDC.clear();
        }
    }
}
