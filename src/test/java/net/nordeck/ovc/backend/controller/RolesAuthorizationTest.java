package net.nordeck.ovc.backend.controller;

import net.nordeck.ovc.backend.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles(value = "test")
@SpringBootTest
public class RolesAuthorizationTest
{

    protected static final String AUTH_USER = "AUTH_USER";
    protected static final String BASIC_ROLE = "BASIC_ROLE";

    private final Authentication auth = TestUtils.initSecurityContext(AUTH_USER, List.of(BASIC_ROLE));

    @InjectMocks
    private RolesAuthorization rolesAuthorization;

    @BeforeEach
    void beforeEach()
    {
        rolesAuthorization.basicAccessRole = BASIC_ROLE;
    }

    @Test
    void hasBasicAccessRole_success()
    {
        assertTrue(rolesAuthorization.hasBasicAccessRole());
    }

    @Test
    void hasBasicAccessRole_noDefaultRole_success()
    {
        rolesAuthorization.basicAccessRole = "";
        assertTrue(rolesAuthorization.getBasicAccessRole().isEmpty());
        assertTrue(rolesAuthorization.hasBasicAccessRole());
    }

    @Test
    void hasBasicAccessRole_notAuthenticated()
    {
        auth.setAuthenticated(false);
        assertFalse(rolesAuthorization.hasBasicAccessRole());
    }
}
