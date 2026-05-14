package com.example.codetogether.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecurityValidator implements ApplicationRunner {
    private static final String DEFAULT_JWT_SECRET = "change-this-secret-key-change-this-secret-key-123456";

    private final Environment environment;
    private final String jwtSecret;
    private final String corsAllowedOrigins;
    private final String ddlAuto;
    private final String dbPassword;

    public ProductionSecurityValidator(
            Environment environment,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.cors.allowed-origins:}") String corsAllowedOrigins,
            @Value("${spring.jpa.hibernate.ddl-auto:}") String ddlAuto,
            @Value("${spring.datasource.password:}") String dbPassword
    ) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.corsAllowedOrigins = corsAllowedOrigins;
        this.ddlAuto = ddlAuto;
        this.dbPassword = dbPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfile()) {
            return;
        }
        require(jwtSecret != null && jwtSecret.length() >= 32 && !DEFAULT_JWT_SECRET.equals(jwtSecret),
                "JWT_SECRET must be set to a strong secret in prod");
        require(corsAllowedOrigins != null && !corsAllowedOrigins.isBlank(),
                "CORS_ALLOWED_ORIGINS must be set in prod");
        require(Arrays.stream(corsAllowedOrigins.split(",")).noneMatch(origin -> origin.contains("localhost")),
                "CORS_ALLOWED_ORIGINS must not include localhost in prod");
        require(!"update".equalsIgnoreCase(ddlAuto) && !"create".equalsIgnoreCase(ddlAuto) && !"create-drop".equalsIgnoreCase(ddlAuto),
                "spring.jpa.hibernate.ddl-auto must not mutate schema in prod");
        require(dbPassword == null || !"123456".equals(dbPassword),
                "DB_PASSWORD must not use the local development default in prod");
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
