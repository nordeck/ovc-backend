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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.nordeck.ovc.backend.dto.ApiErrorDTO;
import net.nordeck.ovc.backend.dto.NotificationsPageDTO;
import net.nordeck.ovc.backend.logging.AppLogger;
import net.nordeck.ovc.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1.0/notifications/")
@CrossOrigin(origins = "*",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS })
@Tag(name = "Notification API", description = "Notification operations")
public class NotificationController
{
    @Value("${api.default-page-size}")
    Integer defaultPageSize;

    protected NotificationService service;

    private final AppLogger logger = new AppLogger("NotificationController");

    public NotificationController(@Autowired NotificationService service) { this.service = service; }


    @GetMapping("")
    @Operation(summary = "Get all notifications for the authenticated user.",
            description = "Get all notifications for the authenticated user.<br/>" +
                    "<br/>Parameters description:"+
                    "<ul>" +
                    "<li><code>limit:</code> The amount of items to return when using paging. Default value is 25.</li>" +
                    "<li><code>offset:</code> The amount of items to skip before starting to collect the result set.</li>" +
                    "</ul>" +
                    "<br/>Access Control:<br/>The calling user must be authenticated.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications page successfully retrieved.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = NotificationsPageDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Bad request.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "404",
                    description = "Resource for given id not found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    @PreAuthorize("hasAuthority(@rolesService.defaultAccessRoles)")
    public ResponseEntity<NotificationsPageDTO> findForUser(
            @Parameter(description = "The amount of items to return. Default value is 20.")
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @Parameter(description = "The number of items to skip before starting to collect the next result set.")
            @RequestParam(required = false, defaultValue = "0") Integer offset
    )
    {
        logger.logRequest("Endpoint 'findForUser' called.");
        if (pageSize == null) pageSize = defaultPageSize;
        if (offset == null) offset = 0;

        return ResponseEntity.ok(service.findAllForUser(PageRequest.of(offset, pageSize)));
    }


    @DeleteMapping("{id}")
    @Operation(summary = "Delete a notification.",
            description = "Delete a notification.<br/>" +
                    "<br/>Access Control:<br/>Only owners are allowed.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification successfully deleted."),
            @ApiResponse(responseCode = "400",
                    description = "Bad request.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "404",
                    description = "Resource for given id not found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    @PreAuthorize("hasAuthority(@rolesService.defaultAccessRoles)")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id of the to be deleted.", required = true)
            @PathVariable UUID id)
    {
        logger.logRequest("Endpoint 'delete' called.");
        Void delete = service.delete(id);
        return ResponseEntity.ok(delete);
    }


    @DeleteMapping("all")
    @Operation(summary = "Delete all notifications for authenticated user.",
            description = "Delete all notifications for authenticated user.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- Only authenticated users are allowed.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications successfully deleted."),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    @PreAuthorize("hasAuthority(@rolesService.defaultAccessRoles)")
    public ResponseEntity<Void> deleteAll()
    {
        Void delete = service.deleteAllForUser();
        return ResponseEntity.ok(delete);
    }


    @PatchMapping(value = "{id}/view")
    @Operation(summary = "Update the 'viewed' field of the given notification.",
            description = "Update the 'viewed' field of the given notification.<br/>" +
                    "<br/>Access Control:<br/>Only owners are allowed.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification successfully updated.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "400",
                    description = "Bad request.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "404",
                    description = "Resource for given id not found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
    })
    @PreAuthorize("hasAuthority(@rolesService.defaultAccessRoles)")
    public ResponseEntity<Void> updateView(
            @Parameter(description = "Id of the notification to be updated.", required = true)
            @PathVariable UUID id)
    {
        logger.logRequest("Endpoint 'updateView' called.");
        service.updateView(id);
        return ResponseEntity.ok(null);
    }


    @PatchMapping(value = "/view-all")
    @Operation(summary = "Update the 'viewed' field of all notifications for the authenticated user.",
            description = "Update the 'viewed' field of all notifications for the authenticated user.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- Only authenticated users are allowed.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notifications successfully updated.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
    })
    @PreAuthorize("hasAuthority(@rolesService.defaultAccessRoles)")
    public ResponseEntity<Void> updateViewAll()
    {
        logger.logRequest("Endpoint 'updateViewAll' called.");
        service.updateViewAll();
        return ResponseEntity.ok(null);
    }
}