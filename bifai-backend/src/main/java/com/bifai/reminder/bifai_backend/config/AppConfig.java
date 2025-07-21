package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String name;
    private String version;
    private String description;
    
    private Security security = new Security();
    private Cors cors = new Cors();
    
    @Getter
    @Setter
    public static class Security {
        private Jwt jwt = new Jwt();
        
        @Getter
        @Setter
        public static class Jwt {
            private String secret;
            private Long expiration;
            private Long refreshExpiration;
        }
    }
    
    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins;
        private String allowedMethods;
        private String allowedHeaders;
        private boolean allowCredentials;
    }
}