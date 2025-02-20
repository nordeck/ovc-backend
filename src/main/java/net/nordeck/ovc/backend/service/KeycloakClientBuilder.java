package net.nordeck.ovc.backend.service;

/*
 * Copyright 2025 Nordeck IT + Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import lombok.Getter;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KeycloakClientBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakClientBuilder.class);

    @Value("${keycloak.server-url:#{null}}")
    protected String serverUrl;

    @Value("${keycloak.realm:#{null}}")
    @Getter
    protected String realm;

    @Value("${keycloak.client-id:#{null}}")
    protected String clientId;

    @Value("${keycloak.client-secret:#{null}}")
    protected String clientSecret;

    protected Keycloak getClient() {
        try {
            return KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();
        }
        catch (Exception e) {
            LOGGER.error("Could not initialize and get a Keycloak client instance: " + e.getMessage());
        }
        return null;
    }
}
