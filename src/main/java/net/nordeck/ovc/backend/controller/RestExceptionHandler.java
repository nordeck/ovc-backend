package net.nordeck.ovc.backend.controller;

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

import io.jsonwebtoken.security.InvalidKeyException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import net.nordeck.ovc.backend.dto.ApiErrorDTO;
import net.nordeck.ovc.backend.logging.AppLogger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.springframework.http.HttpStatus.*;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler
{

    public static final String ACCESS_DENIED_ERROR = "Access denied error.";
    public static final String DATABASE_ERROR = "Database error.";
    public static final String UNEXPECTED_ERROR = "Unexpected error.";
    
    private final AppLogger logger = new AppLogger(this.getClass().getName());

    private ResponseEntity<Object> buildResponseEntity(ApiErrorDTO apiErrorDTO)
    {
        return new ResponseEntity<>(apiErrorDTO, apiErrorDTO.getStatus());
    }

    @ExceptionHandler(InvalidKeyException.class)
    protected ResponseEntity<Object> handleInvalidKeyException(WebRequest request, InvalidKeyException ex)
    {
        ApiErrorDTO apiError = new ApiErrorDTO(INTERNAL_SERVER_ERROR);
        apiError.setMessage("Invalid key for JWT token");
        apiError.setInfoMessage(ex.getLocalizedMessage());
        apiError.setPath(getRequestPath(request));
        logger.logRequestError(apiError.getId(), INTERNAL_SERVER_ERROR.getReasonPhrase(), ex.getLocalizedMessage());
        return buildResponseEntity(apiError);
    }

    @Nullable
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request)
    {
        ApiErrorDTO apiError = new ApiErrorDTO(BAD_REQUEST);
        apiError.setMessage("Missing request parameter: " + ex.getParameterName());
        apiError.setInfoMessage(ex.getLocalizedMessage());
        apiError.setPath(getRequestPath(request));
        logger.logRequestError(apiError.getId(), BAD_REQUEST.getReasonPhrase(), ex);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolation(WebRequest request, ConstraintViolationException ex)
    {
        ApiErrorDTO apiError = new ApiErrorDTO(BAD_REQUEST);
        apiError.setMessage(ex.getLocalizedMessage());
        apiError.setInfoMessage(ex.getLocalizedMessage());
        apiError.setPath(getRequestPath(request));
        logger.logRequestError(apiError.getId(), BAD_REQUEST.getReasonPhrase(), ex);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<Object> handleIllegalArgumentException(WebRequest request, IllegalArgumentException ex)
    {
        ApiErrorDTO apiError = new ApiErrorDTO(INTERNAL_SERVER_ERROR);
        apiError.setMessage(ex.getLocalizedMessage());
        apiError.setInfoMessage(ex.getLocalizedMessage());
        apiError.setPath(getRequestPath(request));
        logger.logRequestError(apiError.getId(), INTERNAL_SERVER_ERROR.getReasonPhrase(), ex);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFound(WebRequest request, EntityNotFoundException ex)
    {
        ApiErrorDTO apiError = new ApiErrorDTO(NOT_FOUND);
        apiError.setMessage(ex.getLocalizedMessage());
        apiError.setInfoMessage(ex.getLocalizedMessage());
        apiError.setPath(getRequestPath(request));
        logger.logRequestError(apiError.getId(), NOT_FOUND.getReasonPhrase(), ex.getLocalizedMessage());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(WebRequest request,
                                                                      MethodArgumentTypeMismatchException ex)
    {
        ApiErrorDTO apiError = new ApiErrorDTO(BAD_REQUEST);
        Class<?> requiredType = ex.getRequiredType();
        String simpleName = requiredType == null ? "undefined" : requiredType.getSimpleName();
        String message = String.format("The parameter '%s' of value '%s' could not be converted to type '%s'.",
                                       ex.getName(), ex.getValue(), simpleName);
        apiError.setMessage(message);
        apiError.setInfoMessage(ex.getLocalizedMessage());
        apiError.setPath(getRequestPath(request));
        logger.logRequestError(apiError.getId(), BAD_REQUEST.getReasonPhrase(), ex);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleDataIntegrityViolation(WebRequest request,
                                                                  DataIntegrityViolationException ex)
    {
        if (ex.getCause() instanceof ConstraintViolationException)
        {
            ApiErrorDTO apiError = new ApiErrorDTO(INTERNAL_SERVER_ERROR);
            apiError.setMessage(DATABASE_ERROR);
            apiError.setInfoMessage(ex.getLocalizedMessage());
            apiError.setPath(getRequestPath(request));
            logger.logRequestError(apiError.getId(), INTERNAL_SERVER_ERROR.getReasonPhrase(), ex);
            return buildResponseEntity(apiError);
        }
        else
        {
            ApiErrorDTO apiError = new ApiErrorDTO(INTERNAL_SERVER_ERROR, ex);
            logger.logRequestError(apiError.getId(), INTERNAL_SERVER_ERROR.getReasonPhrase(), ex);
            return buildResponseEntity(apiError);
        }
    }

    @ExceptionHandler(NullPointerException.class)
    protected ResponseEntity<Object> handleNullPointerException(WebRequest request, NullPointerException ex)
    {
        ApiErrorDTO apiError = new ApiErrorDTO(INTERNAL_SERVER_ERROR);
        apiError.setMessage("Internal server error.");
        apiError.setInfoMessage(ex.getLocalizedMessage());
        apiError.setPath(getRequestPath(request));
        logger.logRequestError(apiError.getId(), INTERNAL_SERVER_ERROR.getReasonPhrase(), ex);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDeniedException(WebRequest request, AccessDeniedException ex)
    {
        ApiErrorDTO apiError = new ApiErrorDTO(FORBIDDEN);
        apiError.setMessage(ACCESS_DENIED_ERROR);
        apiError.setInfoMessage(ex.getLocalizedMessage());
        apiError.setPath(getRequestPath(request));
        logger.logRequestError(apiError.getId(), FORBIDDEN.getReasonPhrase(), ex.getLocalizedMessage());
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<Object> handleRuntimeException(WebRequest request, RuntimeException ex)
    {
        ApiErrorDTO apiError = new ApiErrorDTO(INTERNAL_SERVER_ERROR);
        apiError.setMessage(ex.getLocalizedMessage());
        apiError.setInfoMessage(ex.getLocalizedMessage());
        apiError.setPath(getRequestPath(request));
        logger.logRequestError(apiError.getId(), INTERNAL_SERVER_ERROR.getReasonPhrase(), ex);
        return buildResponseEntity(apiError);
    }

    private static String getRequestPath(WebRequest request)
    {
        return request.getDescription(false).replace("uri=", "");
    }

}