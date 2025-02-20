package net.nordeck.ovc.backend.controller.cache;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ActiveProfiles(value = "test")
@ExtendWith({MockitoExtension.class})
public class CachedBodyHttpServletFilterTest
{
    CachedBodyHttpServletFilter customFilter = new CachedBodyHttpServletFilter();

    @BeforeEach
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDoFilterInternal() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        customFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(CachedBodyHttpServletRequest.class), any(HttpServletResponse.class));
    }
}
