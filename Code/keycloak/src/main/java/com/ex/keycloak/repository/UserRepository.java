package com.ex.keycloak.repository;

import com.ex.keycloak.domain.User;
import com.ex.keycloak.dto.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);


    @Query("""
                select u
                from User u
                where u.keycloakId = :keycloakId
            """)
    Optional<UserInfo> findUserInfoByKeycloakId(String keycloakId);
}
