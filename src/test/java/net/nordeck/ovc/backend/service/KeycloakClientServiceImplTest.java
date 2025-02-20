package net.nordeck.ovc.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles(value = "test")
public class KeycloakClientServiceImplTest {

    @Mock
    private KeycloakClientBuilder clientBuilder;

    @Mock
    private Keycloak keycloakClient;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock UserRepresentation userRepresentation;

    @InjectMocks
    private KeycloakClientServiceImpl service;

    @Test
    void when_findUserByEmail_thenSuccess() {
        when(clientBuilder.getRealm()).thenReturn("realm");
        when(keycloakClient.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByEmail(anyString(), any())).thenReturn(List.of(userRepresentation));

        service.searchByEmail("john.doe@gmail.com");

        verify(keycloakClient, times(1)).realm(anyString());
        verify(realmResource, times(1)).users();
        verify(usersResource, times(1)).searchByEmail(anyString(), any());
    }

    @Test
    void when_findUserByEmail_thenReturnNull() {
        service.keycloakClient = null;

        UserRepresentation user = service.searchByEmail("john.doe@gmail.com");

        assertNull(user);
    }

    @Test
    void init() {
        when(clientBuilder.getClient()).thenReturn(keycloakClient);

        service.init();

        assertEquals(keycloakClient, service.keycloakClient);
    }
}
