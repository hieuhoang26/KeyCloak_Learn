package com.ex.keycloak.service.imp;

import com.ex.keycloak.constants.UserStatus;
import com.ex.keycloak.domain.User;
import com.ex.keycloak.dto.UserDTO;
import com.ex.keycloak.repository.RoleRepository;
import com.ex.keycloak.repository.UserRepository;
import com.ex.keycloak.service.IdentityProvider;
import com.ex.keycloak.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final IdentityProvider identityProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public String createUser(UserDTO dto) {
        String keycloakId = identityProvider.createUser(dto);
        persistLocalUser(keycloakId, dto);
        return keycloakId;
    }

    @Override
    @Transactional
    public String createUserWithPassword(UserDTO dto, String password, boolean hasTempPassword) {
        String keycloakId = identityProvider.createUserWithPassword(dto, password, hasTempPassword);
        persistLocalUser(keycloakId, dto);
        return keycloakId;
    }

    @Override
    public Optional<String> findByEmailAndUsername(String email, String username) {
        return identityProvider.findByEmailAndUsername(email, username);
    }

    @Override
    public void resetPassword(String keycloakId, String password) {
        identityProvider.resetPassword(keycloakId, password);
    }

    @Override
    @Transactional
    public void enableUser(String keycloakId) {
        identityProvider.enableUser(keycloakId);
        updateStatus(keycloakId, UserStatus.ACTIVE);
    }

    @Override
    @Transactional
    public void disableUser(String keycloakId) {
        identityProvider.disableUser(keycloakId);
        updateStatus(keycloakId, UserStatus.INACTIVE);
    }

    @Override
    @Transactional
    public void deleteUser(String keycloakId) {
        identityProvider.deleteUser(keycloakId);
        updateStatus(keycloakId, UserStatus.DELETED);
    }

    @Override
    @Transactional
    public void updateStatus(String keycloakId, UserStatus status) {
        userRepository.findByKeycloakId(keycloakId).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
            log.info("Local user status updated, keycloakId={}, status={}", keycloakId, status);
        });
    }

    private void persistLocalUser(String keycloakId, UserDTO dto) {
        User user = User.builder()
                .keycloakId(keycloakId)
                .email(dto.getEmail())
                .username(dto.getUsername())
                .fullName(dto.getFirstName() + " " + dto.getLastName())
                .status(UserStatus.ACTIVE)
                .build();
        User saved = userRepository.save(user);
        roleRepository.findByName("ROLE_USER").ifPresent(role -> {
            saved.getRoles().add(role);
            userRepository.save(saved);
        });
        log.info("Local user persisted, keycloakId={}, username={}", keycloakId, dto.getUsername());
    }
}
