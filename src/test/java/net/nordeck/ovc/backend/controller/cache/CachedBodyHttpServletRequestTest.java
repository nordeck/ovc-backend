package net.nordeck.ovc.backend.controller.cache;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@ActiveProfiles(value = "test")
@ExtendWith({MockitoExtension.class})
public class CachedBodyHttpServletRequestTest
{

    private HttpServletRequest httpServletRequest;

    private CachedBodyHttpServletRequest request;

    @BeforeEach
    void setup() throws IOException
    {
        httpServletRequest = mock(HttpServletRequest.class);
        request = new CachedBodyHttpServletRequest(httpServletRequest);
    }

    @Test
    void getInputStream()
    {
        ServletInputStream sis = request.getInputStream();
        assertNotNull(sis);
        assertEquals(CachedBodyServletInputStream.class, sis.getClass());
    }

    @Test
    void getReader()
    {
        BufferedReader reader = request.getReader();
        assertNotNull(reader);
    }

}
