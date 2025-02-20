package net.nordeck.ovc.backend.logging;

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

import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import net.logstash.logback.argument.StructuredArguments;
import net.nordeck.ovc.backend.Constants;
import net.nordeck.ovc.backend.controller.cache.CachedBodyHttpServletRequest;
import net.nordeck.ovc.backend.service.AuthenticatedUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import java.util.stream.Collectors;

public class AppLogger
{
    public static final String UNDEFINED = "undefined";
    public static final String STACK_TRACE = "stack_trace";
    public static final String POST = "POST";
    public static final String PUT = "PUT";

    protected Logger logger;

    public AppLogger(String loggerName)
    {
        if (loggerName == null)
        {
            loggerName = "AppLogger";
        }
        logger = LoggerFactory.getLogger(loggerName);
    }

    @SneakyThrows
    public void logRequest(String message)
    {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null)
        {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            String body = null;

            if (POST.equalsIgnoreCase(request.getMethod()) || PUT.equalsIgnoreCase(request.getMethod()))
            {
                CachedBodyHttpServletRequest cachedBodyHttpServletRequest = new CachedBodyHttpServletRequest(request);
                body = cachedBodyHttpServletRequest.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            }
            String userId = getUserId();

            logger.info(message,
                        StructuredArguments.keyValue(Constants.REQUEST_METHOD, request.getMethod()),
                        StructuredArguments.keyValue(Constants.REQUEST_URI, request.getRequestURI()),
                        StructuredArguments.keyValue(Constants.REQUEST_QUERY, request.getQueryString()),
                        StructuredArguments.keyValue(Constants.REQUEST_BODY, body),
                        StructuredArguments.keyValue(Constants.AUTH_USER, userId));
        }
    }

    public void logRequestError(UUID id, String message, String infoMessage)
    {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null)
        {
            HttpServletRequest req = ((ServletRequestAttributes) requestAttributes).getRequest();
            String userId = getUserId();

            logger.error(message,
                         StructuredArguments.keyValue(Constants.ID, id),
                         StructuredArguments.keyValue(Constants.REQUEST_METHOD, req.getMethod()),
                         StructuredArguments.keyValue(Constants.REQUEST_URI, req.getRequestURI()),
                         StructuredArguments.keyValue(Constants.REQUEST_QUERY, req.getQueryString()),
                         StructuredArguments.keyValue(Constants.AUTH_USER, userId),
                         StructuredArguments.keyValue(Constants.INFO_MESSAGE, infoMessage));
        }
    }

    public void logRequestError(UUID id, String message, Exception ex)
    {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null)
        {
            HttpServletRequest req = ((ServletRequestAttributes) requestAttributes).getRequest();
            String userId = getUserId();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            ex.printStackTrace(ps);
            byte[] bytesArray = new ByteArrayInputStream(baos.toByteArray()).readAllBytes();

            logger.error(message,
                         StructuredArguments.keyValue(Constants.ID, id),
                         StructuredArguments.keyValue(Constants.REQUEST_METHOD, req.getMethod()),
                         StructuredArguments.keyValue(Constants.REQUEST_URI, req.getRequestURI()),
                         StructuredArguments.keyValue(Constants.REQUEST_QUERY, req.getQueryString()),
                         StructuredArguments.keyValue(Constants.AUTH_USER, userId),
                         StructuredArguments.keyValue(STACK_TRACE, new String(bytesArray).lines()));
        }
    }

    private String getUserId()
    {
        String userId;
        try
        {
            userId = AuthenticatedUserService.getAuthenticatedUser();
        }
        catch (AccessDeniedException e)
        {
            userId = UNDEFINED;
        }
        return userId;
    }
}
