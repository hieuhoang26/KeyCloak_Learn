package com.ex.keycloak.service.imp;

import com.ex.keycloak.config.KeycloakProperties;
import com.ex.keycloak.dto.UserDTO;
import com.ex.keycloak.dto.UserResponse;
import com.ex.keycloak.service.IdentityProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyCloakService implements IdentityProvider {

    private Keycloak keycloak;
    private final KeycloakProperties props;

    @PostConstruct
    public void keyCloakAdminClient() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl(props.getEndpoint())
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
    }

    private UsersResource usersResource() {
        return keycloak.realm(props.getRealm()).users();
    }

    @PreDestroy
    public void closeKeyCloak() {
        keycloak.close();
    }

    @Override
    public String createUser(UserDTO request) {
        log.debug("Creating user in Keycloak....");
        UserRepresentation user = toRepresentation(request);
        Response response = usersResource().create(user);
        log.debug("Keycloak response header {}", response.getHeaders().toString());
        int responseStatus = response.getStatus();
        if (responseStatus == HttpStatus.CONFLICT.value()) {
            throw new RuntimeException(".....");
        } else if (responseStatus != HttpStatus.CREATED.value()) {
            log.error("Keycloak response body {}", response.readEntity(String.class));
            throw new RuntimeException(".....");
        }
//        String userId = CreatedResponseUtil.getCreatedId(response);
//        setPassword(userId, request.getPassword());
        String createdUserLocation = String.valueOf(response.getHeaders().get("Location"));
        response.close();
        return createdUserLocation.substring(createdUserLocation.lastIndexOf('/') + 1,
                createdUserLocation.length() - 1);

    }

    @Override
    public UserResponse getUserById(String userId) {
        return toResponse(usersResource().get(userId).toRepresentation());
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return usersResource().list().stream().map(this::toResponse).toList();
    }

    @Override
    public UserResponse updateUser(String userId, UserDTO request) {
        usersResource().get(userId).update(toRepresentation(request));
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            setPassword(userId, request.getPassword());
        }
        return toResponse(usersResource().get(userId).toRepresentation());
    }

    @Override
    public void deleteUser(String userId) {
        usersResource().get(userId).remove();
    }


    private void setPassword(String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        usersResource().get(userId).resetPassword(credential);
    }

    private UserRepresentation toRepresentation(UserDTO request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(request.isEnabled());
        return user;
    }

    private UserResponse toResponse(UserRepresentation rep) {
        return UserResponse.builder()
                .id(rep.getId())
                .username(rep.getUsername())
                .email(rep.getEmail())
                .firstName(rep.getFirstName())
                .lastName(rep.getLastName())
                .enabled(rep.isEnabled())
                .build();
    }
}
