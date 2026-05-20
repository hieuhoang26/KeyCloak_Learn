package com.ex.keycloak.service;

import com.ex.keycloak.dto.UserDTO;
import com.ex.keycloak.dto.UserResponse;

import java.util.List;

public interface IdentityProvider {
    String createUser(UserDTO request);

    UserResponse getUserById(String userId);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(String userId, UserDTO request);
    void deleteUser(String userId);
}
