package org.ecommerce.frontendservice.Controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController()
public class HomeController {

    ClassPathResource resource = new ClassPathResource("static/index.html");

    @GetMapping("/")
    public ResponseEntity<String> home() throws IOException {
        String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @GetMapping(value = "/config.js", produces = "application/javascript")
    public ResponseEntity<String> config() {
        String apiBase = System.getenv().getOrDefault("API_BASE_URL", "http://localhost:8080");
        String js = "window.API_BASE = '" + apiBase.replace("'", "\\'") + "';\n";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/javascript")).body(js);
    }
}
