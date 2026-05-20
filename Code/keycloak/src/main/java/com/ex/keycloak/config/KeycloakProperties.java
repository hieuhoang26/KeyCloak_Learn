package com.ex.keycloak.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String endpoint;
    private int connectionPoolSize;
    private int connectTimeoutSeconds;
    private int readTimeoutSeconds;

}
