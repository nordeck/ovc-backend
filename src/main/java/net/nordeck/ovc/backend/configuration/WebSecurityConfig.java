package net.nordeck.ovc.backend.configuration;

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(name = "web.security.enabled", havingValue = "true", matchIfMissing = false)
public class WebSecurityConfig {

    @Value("${web.security.permit-all}")
    protected String[] permitAll;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ServerProperties serverProperties) throws Exception
    {

        // Configure a resource server with JWT decoder (the customized jwtAuthenticationConverter is picked by Spring Boot)
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        // Configure cors with defaults
        http.cors(Customizer.withDefaults());

        // Stateless session (state in access-token only)
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Disable CSRF because of stateless session-management
        http.csrf(AbstractHttpConfigurer::disable);

        http.authorizeHttpRequests((authorize) -> authorize
                .requestMatchers(permitAll).permitAll()
                .anyRequest().authenticated());

        // If SSL enabled, disable http (https only)
        if (serverProperties.getSsl() != null && serverProperties.getSsl().isEnabled()) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http.build();
    }

    /**
     * An authorities converter using solely realm_access.roles claim as source and doing no transformation (no prefix, case untouched)
     */
    @Component
    static class KeycloakRealmRolesGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>>
    {
        @Override
        public List<GrantedAuthority> convert(Jwt jwt) {
            final var realmAccess = (Map<String, Object>) jwt.getClaims().getOrDefault("realm_access", Map.of());
            final var realmRoles = (List<String>) realmAccess.getOrDefault("roles", List.of());
            return realmRoles.stream().map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
        }
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter)
    {
        final var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        jwtAuthenticationConverter.setPrincipalClaimName(StandardClaimNames.PREFERRED_USERNAME);
        return jwtAuthenticationConverter;
    }

}
