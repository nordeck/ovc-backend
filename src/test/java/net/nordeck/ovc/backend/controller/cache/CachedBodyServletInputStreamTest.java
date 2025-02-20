package net.nordeck.ovc.backend.controller.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles(value = "test")
@ExtendWith({MockitoExtension.class})
public class CachedBodyServletInputStreamTest
{
    public static final String STREAM = "CachedBodyServletInputStream";

    private CachedBodyServletInputStream cbsis;

    @BeforeEach
    void setupEach()
    {
        cbsis = new CachedBodyServletInputStream(STREAM.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void read() throws IOException
    {
        int next = cbsis.read();
        assertEquals(STREAM.getBytes()[0], next);
    }

    @Test
    void isFinished()
    {
        boolean result = cbsis.isFinished();
        assertFalse(result);
    }

    @Test
    void isReady()
    {
        assertTrue(cbsis.isReady());
    }

    @Test
    void setReadListener()
    {
        assertThrows(UnsupportedOperationException.class, () -> cbsis.setReadListener(null));
    }
}
