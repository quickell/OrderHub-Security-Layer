package org.example.authorizationserver.controller;

import org.example.authorizationserver.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("")
public class AuthController {

    private final JwtService jwtService;

    @Value("${app.frontend-login-callback:http://localhost:3050/theGame/login/callback}")
    private String frontendLoginCallback;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (jwtService.validateToken(token)) {
                return ResponseEntity.ok(jwtService.getProfileFromToken(token));
            }
            return ResponseEntity.status(401).build();
        }
        if (oauth2User == null) {
            return ResponseEntity.status(401).build();
        }

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String sub = oauth2User.getAttribute("sub");

        if (email == null || sub == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Missing required user attributes (email/sub) from provider"));
        }
        if (name == null) {
            name = email;
        }

        String token = jwtService.generateToken(email, name, sub);

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("email", email);
        profile.put("picture", oauth2User.getAttribute("picture"));
        profile.put("sub", sub);
        profile.put("token", token);

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/redirect-to-app")
    public RedirectView redirectToApp(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return new RedirectView(frontendLoginCallback + "?error=not_authenticated");
        }
        try {
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String sub = oauth2User.getAttribute("sub");
            if (email == null || sub == null) {
                return new RedirectView(frontendLoginCallback + "?error=missing_attributes");
            }
            if (name == null) {
                name = email;
            }
            String token = jwtService.generateToken(email, name, sub);
            String separator = frontendLoginCallback.contains("?") ? "&" : "?";
            String redirectUrl = frontendLoginCallback + separator + "token=" + token;
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            return new RedirectView(frontendLoginCallback + "?error=server_error");
        }
    }

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        Map<String, String> response = new HashMap<>();
        response.put("loginUrl", "/oauth2/authorization/google");
        return ResponseEntity.ok(response);
    }
}
