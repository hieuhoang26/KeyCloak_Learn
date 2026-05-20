package com.ex.keycloak.controller;

import com.ex.keycloak.dto.UserDTO;
import com.ex.keycloak.service.IdentityProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IdentityProvider identityProvider;

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody UserDTO request) {
        String userId = identityProvider.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", userId));
    }

    @PostMapping("/with-password")
    public ResponseEntity<Map<String, String>> createWithPassword(
            @RequestBody UserDTO request,
            @RequestParam(defaultValue = "false") boolean hasTempPassword) {
        String userId = identityProvider.createUserWithPassword(request, request.getPassword(), hasTempPassword);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", userId));
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, String>> search(
            @RequestParam String email,
            @RequestParam String username) {
        return identityProvider.findByEmailAndUsername(email, username)
                .map(id -> ResponseEntity.ok(Map.of("id", id)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        identityProvider.resetPassword(id, body.get("password"));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable String id) {
        identityProvider.enableUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable String id) {
        identityProvider.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        identityProvider.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
