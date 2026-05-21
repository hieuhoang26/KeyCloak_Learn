package com.ex.keycloak.service;

import com.ex.keycloak.constants.UserStatus;
import com.ex.keycloak.dto.UserDTO;

import java.util.Optional;

public interface UserService {

    String createUser(UserDTO dto);

    String createUserWithPassword(UserDTO dto, String password, boolean hasTempPassword);

    Optional<String> findByEmailAndUsername(String email, String username);

    void resetPassword(String keycloakId, String password);

    void enableUser(String keycloakId);

    void disableUser(String keycloakId);

    void deleteUser(String keycloakId);

    void updateStatus(String keycloakId, UserStatus status);
}
