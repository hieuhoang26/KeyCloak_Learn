package com.ex.keycloak.config;

import com.ex.keycloak.repository.UserRepository;
import com.ex.keycloak.security.KeycloakAuthenticationFilter;
import com.ex.keycloak.security.KeycloakAuthenticationProvider;
import com.ex.keycloak.security.KeycloakJwtPreAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final KeycloakProperties keycloakProperties;
    private final UserRepository userRepository;

    public WebSecurityConfig(KeycloakProperties keycloakProperties, UserRepository userRepository) {
        this.keycloakProperties = keycloakProperties;
        this.userRepository = userRepository;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String issuerUri = keycloakProperties.getServerUrl() + "/realms/" + keycloakProperties.getRealm();
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    public KeycloakAuthenticationProvider keycloakAuthenticationProvider() {
        return new KeycloakAuthenticationProvider(userRepository);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(keycloakAuthenticationProvider()));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        KeycloakJwtPreAuthFilter jwtPreAuthFilter = new KeycloakJwtPreAuthFilter(jwtDecoder());
        KeycloakAuthenticationFilter authFilter = new KeycloakAuthenticationFilter(authenticationManager());

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtPreAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(authFilter, KeycloakJwtPreAuthFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("POST", "/users").permitAll()
                        .requestMatchers("POST", "/users/with-password").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
