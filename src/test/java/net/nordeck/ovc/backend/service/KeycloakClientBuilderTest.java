package net.nordeck.ovc.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KeycloakClientBuilderTest {

    @InjectMocks
    private KeycloakClientBuilder clientBuilder;

    @Mock
    private KeycloakBuilder builder;

    @Mock
    private Keycloak keycloak;

    @BeforeEach
    void setup() {
        clientBuilder.serverUrl = "";
        clientBuilder.realm = "";
        clientBuilder.clientId = "";
        clientBuilder.clientSecret = "";
    }


    @Test
    void when_getClient_thenSuccess() {
        try (MockedStatic mocked = mockStatic(KeycloakBuilder.class)) {
            mocked.when(KeycloakBuilder::builder).thenReturn(builder);
            when(builder.serverUrl(anyString())).thenReturn(builder);
            when(builder.realm(anyString())).thenReturn(builder);
            when(builder.grantType(anyString())).thenReturn(builder);
            when(builder.clientId(anyString())).thenReturn(builder);
            when(builder.clientSecret(anyString())).thenReturn(builder);
            when(builder.build()).thenReturn(keycloak);

            clientBuilder.getClient();

            verify(builder, times(1)).serverUrl(anyString());
            verify(builder, times(1)).realm(anyString());
            verify(builder, times(1)).clientId(anyString());
            verify(builder, times(1)).clientSecret(anyString());
            verify(builder, times(1)).build();
        }
    }

    @Test
    void when_getClient_thenFail() {
        try (MockedStatic mocked = mockStatic(KeycloakBuilder.class)) {
            mocked.when(KeycloakBuilder::builder).thenReturn(builder);
            when(builder.serverUrl(anyString())).thenReturn(null);

            Keycloak client = clientBuilder.getClient();
            assertNull(client);
        }
    }

    @Test
    void getRealm() {
        clientBuilder.realm = "realm";
        String realm = clientBuilder.getRealm();
        assertEquals("realm", realm);
    }
}
