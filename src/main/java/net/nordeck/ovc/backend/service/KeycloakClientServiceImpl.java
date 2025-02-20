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

import jakarta.annotation.PostConstruct;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakClientServiceImpl implements KeycloakClientService {

    @Autowired
    protected KeycloakClientBuilder clientBuilder;

    protected Keycloak keycloakClient;

    @PostConstruct
    public void init() {
        this.keycloakClient = clientBuilder.getClient();
    }

    @Override
    public UserRepresentation searchByEmail(String email) {
        if (keycloakClient != null) {
            List<UserRepresentation> users = keycloakClient
                    .realm(clientBuilder.getRealm())
                    .users()
                    .searchByEmail(email, false);
            if (!users.isEmpty()) {
                return users.get(0);
            }
        }
        return null;
    }
}
