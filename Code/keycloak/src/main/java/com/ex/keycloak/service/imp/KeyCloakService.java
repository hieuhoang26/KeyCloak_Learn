package com.ex.keycloak.service.imp;

import com.ex.keycloak.config.KeycloakProperties;
import com.ex.keycloak.dto.UserDTO;
import com.ex.keycloak.service.IdentityProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyCloakService implements IdentityProvider {

    private Keycloak keycloak;
    private final KeycloakProperties props;

    @PostConstruct
    public void initKeycloakAdminClient() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl(props.getServerUrl())
                .realm(props.getRealm())
                .clientId(props.getClientId())
                .clientSecret(props.getClientSecret())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .resteasyClient(
                        new ResteasyClientBuilderImpl()
                                .connectTimeout(props.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                                .connectionPoolSize(props.getConnectionPoolSize())
                                .readTimeout(props.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                                .build()
                )
                .build();
        log.info("Keycloak admin client initialized, realm={}, endpoint={}", props.getRealm(), props.getServerUrl());
    }

    @PreDestroy
    public void closeKeycloakAdminClient() {
        keycloak.close();
        log.info("Keycloak admin client closed");
    }

    @Override
    public String createUser(UserDTO dto) {
        log.debug("Creating user without password, username={}", dto.getUsername());
        return createUserInKeycloak(toRepresentation(dto));
    }

    @Override
    public String createUserWithPassword(UserDTO dto, String password, boolean hasTempPassword) {
        log.debug("Creating user with password, username={}, hasTempPassword={}", dto.getUsername(), hasTempPassword);
        UserRepresentation user = toRepresentation(dto);
        user.setEmailVerified(true);
        user.setCredentials(List.of(buildCredential(password, hasTempPassword)));
        if (hasTempPassword) {
            user.setRequiredActions(List.of("UPDATE_PASSWORD"));
        }
        return createUserInKeycloak(user);
    }

    @Override
    public Optional<String> findByEmailAndUsername(String email, String username) {
        log.debug("Searching user, email={}, username={}", email, username);
        try {
            List<UserRepresentation> byEmail = usersResource().searchByEmail(email, true);
            if (!byEmail.isEmpty()) {
                return Optional.of(byEmail.get(0).getId());
            }
            List<UserRepresentation> byUsername = usersResource().searchByUsername(username, true);
            return byUsername.isEmpty() ? Optional.empty() : Optional.of(byUsername.get(0).getId());
        } catch (Exception e) {
            log.warn("Error searching user, email={}, username={}", email, username, e);
            return Optional.empty();
        }
    }

    @Override
    public void resetPassword(String userId, String password) {
        log.debug("Resetting password, userId={}", userId);
        usersResource().get(userId).resetPassword(buildCredential(password, false));
        log.info("Password reset, userId={}", userId);
    }

    @Override
    public void enableUser(String userId) {
        UserResource userResource = usersResource().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        if (user.isEnabled()) {
            log.debug("User already enabled, userId={}", userId);
            return;
        }
        user.setEnabled(true);
        userResource.update(user);
        log.info("User enabled, userId={}", userId);
    }

    @Override
    public void disableUser(String userId) {
        UserResource userResource = usersResource().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        if (!user.isEnabled()) {
            log.debug("User already disabled, userId={}", userId);
            return;
        }
        user.setEnabled(false);
        userResource.update(user);
        userResource.logout();
        log.info("User disabled and sessions invalidated, userId={}", userId);
    }

    @Override
    public void deleteUser(String userId) {
        UserResource userResource = usersResource().get(userId);
        if (userResource.toRepresentation() == null) {
            log.debug("User not found, skipping delete, userId={}", userId);
            return;
        }
        userResource.remove();
        log.info("User deleted, userId={}", userId);
    }

    private String createUserInKeycloak(UserRepresentation kcUser) {
        Response response = usersResource().create(kcUser);
        int status = response.getStatus();

        if (status == HttpStatus.CONFLICT.value()) {
            log.warn("User already exists, username={}", kcUser.getUsername());
            throw new RuntimeException("User already exists: username=" + kcUser.getUsername());
        }
        if (status != HttpStatus.CREATED.value()) {
            String body = response.readEntity(String.class);
            log.error("Unexpected Keycloak response creating user, status={}, body={}, username={}", status, body, kcUser.getUsername());
            throw new RuntimeException("Failed to create user in Keycloak, status=" + status);
        }

        // Location header is returned as a List; toString() wraps it in [...], so trim the trailing ']'
        String location = String.valueOf(response.getHeaders().get("Location"));
        response.close();
        String userId = location.substring(location.lastIndexOf('/') + 1, location.length() - 1);
        log.info("User created in Keycloak, userId={}, username={}", userId, kcUser.getUsername());
        return userId;
    }

    private CredentialRepresentation buildCredential(String password, boolean temporary) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(temporary);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        return credential;
    }

    private UserRepresentation toRepresentation(UserDTO dto) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEnabled(dto.isEnabled());
        return user;
    }

    private UsersResource usersResource() {
        return keycloak.realm(props.getRealm()).users();
    }
}
