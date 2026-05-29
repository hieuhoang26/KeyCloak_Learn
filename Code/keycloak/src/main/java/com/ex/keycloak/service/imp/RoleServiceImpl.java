package com.ex.keycloak.service.imp;

import com.ex.keycloak.domain.Role;
import com.ex.keycloak.domain.User;
import com.ex.keycloak.dto.RoleDTO;
import com.ex.keycloak.dto.RoleResponse;
import com.ex.keycloak.exception.ResourceNotFoundException;
import com.ex.keycloak.repository.RoleRepository;
import com.ex.keycloak.repository.UserRepository;
import com.ex.keycloak.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RoleResponse create(RoleDTO dto) {
        if (roleRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + dto.getName());
        }
        Role role = Role.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        Role saved = roleRepository.save(role);
        log.info("Role created: {}", saved.getName());
        return toResponse(saved);
    }

    @Override
    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public RoleResponse findById(UUID id) {
        return roleRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
        roleRepository.delete(role);
        log.info("Role deleted: {}", role.getName());
    }

    @Override
    @Transactional
    public List<String> assignRoleToUser(String keycloakId, UUID roleId) {
        User user = findUserByKeycloakId(keycloakId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));
        user.getRoles().add(role);
        userRepository.save(user);
        log.info("Role {} assigned to user {}", role.getName(), keycloakId);
        return extractRoleNames(user);
    }

    @Override
    @Transactional
    public void revokeRoleFromUser(String keycloakId, UUID roleId) {
        User user = findUserByKeycloakId(keycloakId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));
        user.getRoles().remove(role);
        userRepository.save(user);
        log.info("Role {} revoked from user {}", role.getName(), keycloakId);
    }

    @Override
    public List<String> getUserRoles(String keycloakId) {
        User user = findUserByKeycloakId(keycloakId);
        return extractRoleNames(user);
    }

    private User findUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + keycloakId));
    }

    private List<String> extractRoleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .toList();
    }

    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}
