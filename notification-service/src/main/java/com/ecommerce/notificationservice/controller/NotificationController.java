package com.ecommerce.notificationservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody String message) {
        log.info("Notification received: {}", message);
        return ResponseEntity.ok("Notification sent");
    }
}
