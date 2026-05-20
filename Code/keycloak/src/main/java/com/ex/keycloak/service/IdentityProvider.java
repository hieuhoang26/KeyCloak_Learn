package com.ex.keycloak.service;

import com.ex.keycloak.dto.UserDTO;

import java.util.Optional;

public interface IdentityProvider {

    String createUser(UserDTO dto);

    String createUserWithPassword(UserDTO dto, String password, boolean hasTempPassword);

    Optional<String> findByEmailAndUsername(String email, String username);

    void resetPassword(String userId, String password);

    void enableUser(String userId);

    void disableUser(String userId);

    /**
     * Permanently removes the user account. This action is irreversible.
     */
    void deleteUser(String userId);
}
