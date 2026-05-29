package com.ex.keycloak.controller;

import com.ex.keycloak.dto.UserDTO;
import com.ex.keycloak.service.RoleService;
import com.ex.keycloak.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<Map<String, String>> create(@RequestBody UserDTO request) {
        String userId = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", userId));
    }

    @PostMapping("/with-password")
    public ResponseEntity<Map<String, String>> createWithPassword(
            @RequestBody UserDTO request,
            @RequestParam(defaultValue = "false") boolean hasTempPassword) {
        String userId = userService.createUserWithPassword(request, request.getPassword(), hasTempPassword);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", userId));
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, String>> search(
            @RequestParam String email,
            @RequestParam String username) {
        return userService.findByEmailAndUsername(email, username)
                .map(id -> ResponseEntity.ok(Map.of("id", id)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable String id) {
        userService.enableUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable String id) {
        userService.disableUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<List<String>> getUserRoles(@PathVariable String id) {
        return ResponseEntity.ok(roleService.getUserRoles(id));
    }

    @PostMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> assignRole(
            @PathVariable String id,
            @PathVariable UUID roleId) {
        return ResponseEntity.ok(roleService.assignRoleToUser(id, roleId));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeRole(
            @PathVariable String id,
            @PathVariable UUID roleId) {
        roleService.revokeRoleFromUser(id, roleId);
        return ResponseEntity.noContent().build();
    }
}
