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

import lombok.SneakyThrows;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class AuthenticatedUserService {

    public static final String CANNOT_GET_EMAIL_ERROR_MSG = "Cannot get email from Security Context";


    @SneakyThrows
    public static String getAuthenticatedUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email;
            if (auth != null) {
                Jwt token = (Jwt) auth.getPrincipal();
                email = token.getClaim("email");
            } else {
                throw new AccessDeniedException(CANNOT_GET_EMAIL_ERROR_MSG);
            }
            return email;
        } catch (Exception e) {
            throw new AccessDeniedException(CANNOT_GET_EMAIL_ERROR_MSG);
        }
    }
}