package org.fractalx.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified security properties for the generated API gateway.
 *
 * <pre>
 * fractalx:
 *   gateway:
 *     security:
 *       enabled: true
 *       public-paths: /api/&#42;/public/&#42;&#42;, /api/&#42;/auth/&#42;&#42;
 *       bearer:
 *         enabled: true
 *         jwt-secret: my-secret-key-min-32-chars-long!!
 *       oauth2:
 *         enabled: false
 *         jwk-set-uri: http://keycloak:8080/realms/myrealm/protocol/openid-connect/certs
 *       basic:
 *         enabled: false
 *         username: admin
 *         password: changeme
 *       api-key:
 *         enabled: false
 *         valid-keys:
 *           - my-api-key-1
 *           - my-api-key-2
 * </pre>
 */
@ConfigurationProperties(prefix = "fractalx.gateway.security")
public class GatewayAuthProperties {

    private boolean enabled = false;
    private String[] publicPaths = {"/api/*/public/**", "/api/*/auth/**"};

    private Bearer bearer = new Bearer();
    private OAuth2 oauth2 = new OAuth2();
    private Basic  basic  = new Basic();
    private ApiKey apiKey = new ApiKey();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String[] getPublicPaths() { return publicPaths; }
    public void setPublicPaths(String[] publicPaths) { this.publicPaths = publicPaths; }
    public Bearer getBearer() { return bearer; }
    public void setBearer(Bearer bearer) { this.bearer = bearer; }
    public OAuth2 getOauth2() { return oauth2; }
    public void setOauth2(OAuth2 oauth2) { this.oauth2 = oauth2; }
    public Basic getBasic() { return basic; }
    public void setBasic(Basic basic) { this.basic = basic; }
    public ApiKey getApiKey() { return apiKey; }
    public void setApiKey(ApiKey apiKey) { this.apiKey = apiKey; }

    public static class Bearer {
        private boolean enabled = false;
        private String jwtSecret = "fractalx-default-secret-change-in-prod-min-32chars!!";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String s) { this.jwtSecret = s; }
    }

    public static class OAuth2 {
        private boolean enabled = false;
        private String jwkSetUri = "http://localhost:8080/realms/fractalx/protocol/openid-connect/certs";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getJwkSetUri() { return jwkSetUri; }
        public void setJwkSetUri(String uri) { this.jwkSetUri = uri; }
    }

    public static class Basic {
        private boolean enabled = false;
        private String username = "fractalx";
        private String password = "changeme";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getUsername() { return username; }
        public void setUsername(String u) { this.username = u; }
        public String getPassword() { return password; }
        public void setPassword(String p) { this.password = p; }
    }

    public static class ApiKey {
        private boolean enabled = false;
        private List<String> validKeys = new ArrayList<>(List.of("dev-key-replace-me"));
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getValidKeys() { return validKeys; }
        public void setValidKeys(List<String> keys) { this.validKeys = keys; }
    }
}
