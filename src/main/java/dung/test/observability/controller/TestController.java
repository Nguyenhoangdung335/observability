package dung.test.observability.controller;

import dung.test.observability.annotation.Measured;
import dung.test.observability.annotation.Traced;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Traced
@Measured
@RestController
@RequestMapping("/api/test")
public class TestController {
    @GetMapping
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello, World!");
    }
}
