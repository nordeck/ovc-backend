package net.nordeck.ovc.backend.controller;

import io.jsonwebtoken.security.InvalidKeyException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import net.nordeck.ovc.backend.Constants;
import net.nordeck.ovc.backend.dto.ApiErrorDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.UUID;

import static net.nordeck.ovc.backend.controller.RestExceptionHandler.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith({MockitoExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles(value = "test")
public class RestExceptionHandlerTest
{

    public static final String REQUEST_URI = "/api/request";

    private RestExceptionHandler handler;

    private WebRequest request;

    @BeforeAll
    void initData()
    {
        handler = new RestExceptionHandler();
        request = new ServletWebRequest(new MockHttpServletRequest("GET", REQUEST_URI));
        RequestContextHolder.setRequestAttributes(request);
        RequestContextHolder.getRequestAttributes().setAttribute(Constants.ID, UUID.randomUUID(), 0);
        RequestContextHolder.getRequestAttributes().setAttribute(Constants.REQUEST_METHOD, "request_method", 0);
        RequestContextHolder.getRequestAttributes().setAttribute(Constants.REQUEST_URI, "request_uri", 0);
        RequestContextHolder.getRequestAttributes().setAttribute(Constants.REQUEST_QUERY, "request_query", 0);
    }

    @AfterAll
    void afterAll()
    {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void handleNullPointerException()
    {
        handler.handleNullPointerException(request, new NullPointerException("message"));
    }

    @Test
    void handleInvalidKeyException()
    {
        handler.handleInvalidKeyException(request, new InvalidKeyException("message"));
    }

    @Test
    void handleIllegalArgumentException()
    {
        handler.handleIllegalArgumentException(request, new IllegalArgumentException("message"));
    }

    @Test
    void handleMethodArgumentTypeMismatch()
    {
        handler.handleMethodArgumentTypeMismatch(request, 
                                       new MethodArgumentTypeMismatchException(new Object(), Integer.class, "argument", null, new Throwable("")));
    }

    @Test
    void handleConstraintViolation()
    {
        handler.handleConstraintViolation(request, new ConstraintViolationException(new HashSet<>()));
    }

    @Test
    void handleDataIntegrityViolation()
    {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(INTERNAL_SERVER_ERROR.name(), new ConstraintViolationException(null));
        ApiErrorDTO error = (ApiErrorDTO) handler.handleDataIntegrityViolation(request, ex).getBody();
        assertEquals(DATABASE_ERROR, error.getMessage());

        ex = new DataIntegrityViolationException(INTERNAL_SERVER_ERROR.name());
        error = (ApiErrorDTO) handler.handleDataIntegrityViolation(request, ex).getBody();
        assertEquals(UNEXPECTED_ERROR, error.getMessage());
    }

    @Test
    void handleEntityNotFound()
    {
        ApiErrorDTO error = (ApiErrorDTO) handler.handleEntityNotFound(request,
                                                           new EntityNotFoundException(NOT_FOUND.name())).getBody();
        assertEquals(NOT_FOUND.name(), error.getMessage());
    }

    @Test
    void handleMissingServletRequestParameter()
    {
        ResponseEntity<Object> response = handler.handleMissingServletRequestParameter(
                new MissingServletRequestParameterException("paramName", "paramType"),
                new HttpHeaders(),
                HttpStatus.OK,
                request
        );
        ApiErrorDTO error = (ApiErrorDTO) response.getBody();
        assert (HttpStatus.BAD_REQUEST.equals(error.getStatus()));
        assert (REQUEST_URI.equals(error.getPath()));
    }

    @Test
    void handleAccessDeniedException()
    {
        ApiErrorDTO error = (ApiErrorDTO) handler.handleAccessDeniedException(request,
                                                          new AccessDeniedException(ACCESS_DENIED_ERROR)).getBody();
        assertEquals(ACCESS_DENIED_ERROR, error.getMessage());
    }

    @Test
    void handleRuntimeException()
    {
        ApiErrorDTO error = (ApiErrorDTO) handler.handleRuntimeException(request,
                                                         new RuntimeException(INTERNAL_SERVER_ERROR.name())).getBody();
        assertEquals(INTERNAL_SERVER_ERROR.name(), error.getMessage());
    }

    @Test
    void testApiError()
    {
        ApiErrorDTO error = new ApiErrorDTO(INTERNAL_SERVER_ERROR, "message", new RuntimeException());
        error.setMessage("message");
        error.setInfoMessage("debug message");
        error.setTimestamp(ZonedDateTime.now());
        error.setPath("path");
        error.setStatus(INTERNAL_SERVER_ERROR);

        error.getTimestamp();
        error.getInfoMessage();
        error.getMessage();
        error.getStatus();
        error.getStatus();
        error.getPath();
    }

}