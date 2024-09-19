package com.app.controller.requesthelper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class AuthValidator {

    @Value("${app.auth.cms.username}")
    private String validUsername;

    @Value("${app.auth.cms.password}")
    private String validPassword;

    public boolean validate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.info("Couldn't validate header, received: " + authHeader);
            return false;
        }

        String credentials;
        try {
            String base64Credentials = authHeader.substring("Basic ".length()).trim();
            credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode Base64 credentials", e);
            return false;
        }

        final String[] values = credentials.split(":", 2);
        if (values.length != 2) {
            return false;
        }

        String username = values[0];
        String password = values[1];

        return validUsername.equals(username) && validPassword.equals(password);
    }
}