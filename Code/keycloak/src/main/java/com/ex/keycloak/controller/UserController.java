package com.ex.keycloak.controller;

import com.ex.keycloak.dto.UserDTO;
import com.ex.keycloak.dto.UserResponse;
import com.ex.keycloak.service.IdentityProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IdentityProvider identityProvider;

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody UserDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(identityProvider.createUser(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(identityProvider.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(identityProvider.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable String id, @RequestBody UserDTO request) {
        return ResponseEntity.ok(identityProvider.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        identityProvider.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
