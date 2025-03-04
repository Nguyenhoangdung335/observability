package dung.test.observability.controller;

import dung.test.observability.annotation.Measured;
import dung.test.observability.annotation.Traced;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Traced
@Measured
@RestController
@RequestMapping("/api/test")
@Slf4j
public class TestController {
    private final io.opentelemetry.api.logs.Logger otelLogger;
//    private final org.slf4j.Logger slf4jLogger;

    public TestController(io.opentelemetry.api.logs.Logger otelLogger) {
        this.otelLogger = otelLogger;
//        this.slf4jLogger = LoggerFactory.getLogger(TestController.class);
    }

    @GetMapping
    public ResponseEntity<String> hello() {
        log.info("hello");

        return ResponseEntity.ok("Hello, World!");
    }

    @GetMapping("/log-test")
    public Map<String, String> logTest() {
        // Direct SDK log
        otelLogger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setBody("Direct SDK log test")
                .setAttribute(AttributeKey.stringKey("test.attribute"), "value")
                .emit();

        // SLF4J log
        log.info("SLF4J log test");

        return Map.of("status", "Logs emitted");
    }

    @GetMapping("/otel-status")
    public Map<String, Object> checkOpenTelemetryStatus() {
        boolean isGlobalSet = false;
        String message = "OpenTelemetry not configured globally";

        try {
            OpenTelemetry global = GlobalOpenTelemetry.get();
            isGlobalSet = true;
            message = "OpenTelemetry configured globally";
        } catch (Exception e) {
            message = "Error getting global OpenTelemetry: " + e.getMessage();
        }

        return Map.of(
                "globalOpenTelemetryConfigured", isGlobalSet,
                "message", message
        );
    }
}
