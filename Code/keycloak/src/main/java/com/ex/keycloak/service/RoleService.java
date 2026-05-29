package com.ex.keycloak.service;

import com.ex.keycloak.dto.RoleDTO;
import com.ex.keycloak.dto.RoleResponse;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    RoleResponse create(RoleDTO dto);

    List<RoleResponse> findAll();

    RoleResponse findById(UUID id);

    void delete(UUID id);

    List<String> assignRoleToUser(String keycloakId, UUID roleId);

    void revokeRoleFromUser(String keycloakId, UUID roleId);

    List<String> getUserRoles(String keycloakId);
}
