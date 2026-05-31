package com.ex.keycloak.dto;


import com.ex.keycloak.constants.UserStatus;
import org.springframework.beans.factory.annotation.Value;

import java.util.Set;
import java.util.UUID;

public interface UserInfo {

    UUID getId();

    String getKeycloakId();

    String getUsername();

    UserStatus getStatus();

    String getEmail();

    Set<String> getRoles();
}