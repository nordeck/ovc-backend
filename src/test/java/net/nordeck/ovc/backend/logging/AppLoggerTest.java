package net.nordeck.ovc.backend.logging;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import net.logstash.logback.argument.StructuredArgument;
import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.controller.cache.CachedBodyServletInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ActiveProfiles(value = "test")
@SpringBootTest
public class AppLoggerTest
{
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String MESSAGE = "MESSAGE";
    public static final String QUERY_STRING = "QUERY_STRING";
    public static final String URI = "http://localhost:8080/";
    public static final String AUTH_USER = "AUTH_USER";
    public static final String INFO_MESSAGE = "INFO_MESSAGE";
    public static final String BODY_TEXT = "{ \"body\":\"text\" }";

    @Mock
    private Authentication auth;

    @Mock
    private Logger logger;

    private static MockedStatic<RequestContextHolder> utilities;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletRequestAttributes requestAttributes;

    @InjectMocks
    protected AppLogger appLogger;

    @Captor
    ArgumentCaptor<String> messageCaptor;

    @Captor
    ArgumentCaptor<StructuredArgument> idCaptor;

    @Captor
    ArgumentCaptor<StructuredArgument> methodCaptor;

    @Captor
    ArgumentCaptor<StructuredArgument> uriCaptor;

    @Captor
    ArgumentCaptor<StructuredArgument> queryCaptor;

    @Captor
    ArgumentCaptor<StructuredArgument> authUserCaptor;

    @Captor
    ArgumentCaptor<StructuredArgument> bodyCaptor;

    @Captor
    ArgumentCaptor<StructuredArgument> infoMessageCaptor;

    @Captor
    ArgumentCaptor<StructuredArgument> stackTraceCaptor;

    @BeforeAll
    static void beforeAll()
    {
        utilities = mockStatic(RequestContextHolder.class);
    }

    @BeforeEach
    void beforeEach()
    {
        utilities.when(() -> RequestContextHolder.getRequestAttributes()).thenReturn(requestAttributes);
        when(requestAttributes.getRequest()).thenReturn(request);
        when(request.getRequestURI()).thenReturn(URI);
        when(request.getQueryString()).thenReturn(QUERY_STRING);
        when(request.getMethod()).thenReturn(GET);
        appLogger.logger = logger;
    }

    @Test
    void logGetRequestWithMessageOnly()
    {
        TestUtils.initSecurityContext(auth, AUTH_USER);
        appLogger.logRequest(MESSAGE);

        verify(logger, times(1)).info(
                messageCaptor.capture(),
                methodCaptor.capture(),
                uriCaptor.capture(),
                queryCaptor.capture(),
                bodyCaptor.capture(),
                authUserCaptor.capture());

        assertEquals(messageCaptor.getValue(), MESSAGE);
        assertEquals(methodCaptor.getValue().toString(), "request_method=" + GET);
        assertEquals(uriCaptor.getValue().toString(), "request_uri=" + URI);
        assertEquals(queryCaptor.getValue().toString(), "request_query=" + QUERY_STRING);
        assertEquals(bodyCaptor.getValue().toString(), "request_body=" + null);
        assertEquals(authUserCaptor.getValue().toString(), "auth_user=" + AUTH_USER);
    }

    @Test
    void logPostRequestWithMessageOnly() throws IOException
    {
        TestUtils.initSecurityContext(auth, AUTH_USER);
        when(request.getMethod()).thenReturn(POST);
        ServletInputStream sis = new CachedBodyServletInputStream(BODY_TEXT.getBytes(StandardCharsets.UTF_8));
        when(request.getInputStream()).thenReturn(sis);

        appLogger.logRequest(MESSAGE);

        verify(logger, times(1)).info(
                messageCaptor.capture(),
                methodCaptor.capture(),
                uriCaptor.capture(),
                queryCaptor.capture(),
                bodyCaptor.capture(),
                authUserCaptor.capture());

        assertEquals(messageCaptor.getValue(), MESSAGE);
        assertEquals(methodCaptor.getValue().toString(), "request_method=" + POST);
        assertEquals(uriCaptor.getValue().toString(), "request_uri=" + URI);
        assertEquals(queryCaptor.getValue().toString(), "request_query=" + QUERY_STRING);
        assertEquals(bodyCaptor.getValue().toString(), "request_body=" + BODY_TEXT);
        assertEquals(authUserCaptor.getValue().toString(), "auth_user=" + AUTH_USER);
    }

    @Test
    void logGetRequestErrorWithInfoMessage()
    {
        TestUtils.initSecurityContext(auth, AUTH_USER);
        UUID id = UUID.randomUUID();
        appLogger.logRequestError(id, MESSAGE, INFO_MESSAGE);

        verify(logger, times(1)).error(
                messageCaptor.capture(),
                idCaptor.capture(),
                methodCaptor.capture(),
                uriCaptor.capture(),
                queryCaptor.capture(),
                authUserCaptor.capture(),
                infoMessageCaptor.capture());

        assertEquals(messageCaptor.getValue(), MESSAGE);
        assertEquals(methodCaptor.getValue().toString(), "request_method=" + GET);
        assertEquals(uriCaptor.getValue().toString(), "request_uri=" + URI);
        assertEquals(queryCaptor.getValue().toString(), "request_query=" + QUERY_STRING);
        assertEquals(authUserCaptor.getValue().toString(), "auth_user=" + AUTH_USER);
        assertEquals(infoMessageCaptor.getValue().toString(), "info_message=" + INFO_MESSAGE);
    }

    @Test
    void logGetRequestErrorWithException()
    {
        TestUtils.initSecurityContext(auth, AUTH_USER);
        UUID id = UUID.randomUUID();
        appLogger.logRequestError(id, MESSAGE, new RuntimeException("exception message"));

        verify(logger, times(1)).error(
                messageCaptor.capture(),
                idCaptor.capture(),
                methodCaptor.capture(),
                uriCaptor.capture(),
                queryCaptor.capture(),
                authUserCaptor.capture(),
                stackTraceCaptor.capture());

        assertEquals(messageCaptor.getValue(), MESSAGE);
        assertEquals(methodCaptor.getValue().toString(), "request_method=" + GET);
        assertEquals(uriCaptor.getValue().toString(), "request_uri=" + URI);
        assertEquals(queryCaptor.getValue().toString(), "request_query=" + QUERY_STRING);
        assertEquals(authUserCaptor.getValue().toString(), "auth_user=" + AUTH_USER);
        assertTrue(stackTraceCaptor.getValue().toString().contains("stack_trace="));
    }
}
