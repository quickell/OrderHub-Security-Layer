package org.example.authorizationserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String message = (String) request.getAttribute("jakarta.servlet.error.message");
        String path = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        if (statusCode == null) {
            statusCode = 500;
        }
        if (message == null || message.isEmpty()) {
            try {
                message = HttpStatus.valueOf(statusCode).getReasonPhrase();
            } catch (IllegalArgumentException e) {
                message = "Error";
            }
        }
        Map<String, Object> body = new HashMap<>();
        body.put("error", statusCode >= 400 ? "Request failed" : "Error");
        body.put("status", statusCode);
        body.put("message", message);
        body.put("path", path != null ? path : "");
        return ResponseEntity.status(statusCode).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
