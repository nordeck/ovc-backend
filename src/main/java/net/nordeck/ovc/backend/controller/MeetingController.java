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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.nordeck.ovc.backend.dto.*;
import net.nordeck.ovc.backend.logging.AppLogger;
import net.nordeck.ovc.backend.service.JitsiService;
import net.nordeck.ovc.backend.service.MeetingParticipantService;
import net.nordeck.ovc.backend.service.MeetingService;
import net.nordeck.ovc.backend.service.PermissionControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1.0/meetings/")
@CrossOrigin(origins = "*", allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.OPTIONS})
@Tag(name = "Meeting API", description = "Meetings operations")
public class MeetingController
{

    private final PermissionControlService permissionControlService;

    private final MeetingService meetingService;

    private final MeetingParticipantService participantService;

    private final JitsiService jitsiService;

    private final AppLogger logger = new AppLogger("MeetingController");

    @Value("${api.default-page-size}")
    Integer defaultPageSize;

    @Value("${api.default-weeks-before}")
    Integer defaultWeeksBefore;

    @Value("${api.default-weeks-after}")
    Integer defaultWeeksAfter;

    public MeetingController(
            @Autowired MeetingService meetingService,
            @Autowired MeetingParticipantService participantService,
            @Autowired JitsiService jitsiService,
            @Autowired PermissionControlService permissionControlService)
    {
        this.meetingService = meetingService;
        this.participantService = participantService;
        this.jitsiService = jitsiService;
        this.permissionControlService = permissionControlService;
    }

    @GetMapping("")
    @Operation(summary = "Get meetings after the given filter parameters.",
            description = "Get meetings after the given filter parameters.<br/>" +
                    "<br/>Parameters description:" +
                    "<ul>" +
                    "<li><code>type:</code></li>" +
                    "<ul>" +
                    "<li>normal <b>(default)</b>: Returns a list of normal meetings for the authenticated user.</li>" +
                    "<li>static: Returns a list of static rooms for the authenticated user.</li>" +
                    "<li>instant: Returns a list of instant meetings for the authenticated user.</li>" +
                    "</ul>" +
                    "<li><code>page-size:</code> The amount of items to return when using paging. Default value is 20.</li>" +
                    "<li><code>offset:</code> The amount of items to skip before starting to collect the result set.</li>" +
                    "<li><code>start:</code> Start datetime (ISO-8601) used for filtering on normal meeting start datetime. Default is 4 weeks before today.</li>" +
                    "<li><code>end:</code> End datetime (ISO-8601) used for filtering on normal meeting end datetime. Default is 4 weeks after today.</li>" +
                    "<li><code>order:</code> The sorting order for normal meetings and rooms: 'asc' or 'desc'.</li>" +
                    "<ul>" +
                    "<li>for normal: Sorting always by start datetime. Default: 'asc'.</li>" +
                    "<li>for static: Sorting always by creation datetime. Default: 'asc'.</li>" +
                    "<li>for instant: Not applicable. If more than one existing, always sorted by creation datetime 'desc'." +
                    "</ul>" +
                    "</ul><br/>" +
                    "<br/>Access Control:<br/>" +
                    "- For all meeting types: the calling user must be an authenticated and be a meeting owner or participant.")
    @ApiResponses( value = {
            @ApiResponse(responseCode = "200",
                    description = "List of meetings successfully retrieved.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingsPageDTO.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    @PreAuthorize("hasAuthority('default-roles-vk-bund')")
    public ResponseEntity<MeetingsPageDTO> findMeetings(
            @Parameter(description = "The type of meeting to be retrieved: 'normal', 'static', 'instant'")
            @RequestParam(defaultValue = "normal", required = false) String type,
            @Parameter(description = "The amount of items to return. Default value is 20.")
            @RequestParam(defaultValue = "20", required = false, name = "page-size") Integer pageSize,
            @Parameter(description = "The number of items to skip before starting to collect the next result set.")
            @RequestParam(defaultValue = "0", required = false) Integer offset,
            @Parameter(description = "The start date (ISO-8601) for filtering over start datetime.<br/>" +
                    "Applied only for normal meetings.<br/>" +
                    "It needs to be URL encoded (ex. '+' characters). " +
                    "Default value is 4 weeks before now.",
                    name = "start",
                    example = "2024-11-05T01:00:00.000+02:00")
            @RequestParam(required = false, name = "start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDateTime,
            @Parameter(description = "The end datetime (ISO-8601) for filtering over start datetime.<br/>" +
                    "Applied only for normal meetings.<br/>" +
                    "It needs to be URL encoded (ex. '+' characters). " +
                    "Default value is 4 weeks after now.",
                    name = "end",
                    example = "2024-12-30T01:00:00-01:00")
            @RequestParam(required = false, name = "end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDateTime,
            @Parameter(description = "The sorting order for normal meetings and static rooms: 'asc' or 'desc'. Default: 'asc'.")
            @RequestParam(defaultValue = "asc", required = false) String order
    )
    {
        if (pageSize == null) pageSize = defaultPageSize;
        if (type == null) type = "normal";
        if (offset == null) offset = 0;
        if (startDateTime == null) startDateTime = ZonedDateTime.now().minusWeeks(defaultWeeksBefore);
        if (endDateTime == null) endDateTime = ZonedDateTime.now().plusWeeks(defaultWeeksAfter);
        if (order == null) order = "asc";

        logger.logRequest( "Endpoint 'findMeetings' called.");

        return ResponseEntity.ok(meetingService.getMeetingsPage(type, offset, pageSize, order, startDateTime, endDateTime));
    }


    @GetMapping("{mId}")
    @Operation(summary = "Get a meeting by its id.",
            description = "Get a meeting by its id.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- For meetings and static rooms: owner and all participants are allowed.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting successfully retrieved.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingDTO.class))),
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
    @PreAuthorize("hasAuthority('default-roles-vk-bund')")
    public ResponseEntity<MeetingDTO> findById(
            @Parameter(description = "Id of the meeting.", required = true)
            @PathVariable UUID mId)
    {
        logger.logRequest("Endpoint 'findById' called.");
        MeetingDTO meetingDTO = meetingService.findById(mId);
        return ResponseEntity.ok(meetingDTO);
    }


    @GetMapping("{mId}/basic")
    @Operation(summary = "Get a meeting with only basic information by its id.",
            description = "Get a meeting with only basic information by its id.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- No access restriction required.<br/>" +
                    "- The endpoint is rate limited.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting successfully retrieved.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingBasicDTO.class))),
            @ApiResponse(responseCode = "429",
                    description = "Too many requests.",
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
    public ResponseEntity<MeetingBasicDTO> findBasicById(
            @Parameter(description = "Id of the meeting.", required = true)
            @PathVariable UUID mId)
    {
        logger.logRequest("Endpoint 'findBasicById' called.");
        MeetingBasicDTO meetingDTO = meetingService.findBasicById(mId);
        return ResponseEntity.ok(meetingDTO);
    }


    @DeleteMapping(value="{mId}")
    @Operation(summary = "Delete meeting for the id.",
            description =
                    "Delete the meeting for the given id.<br/>" +
                    "If the id belongs to a parent meeting, the whole series will be deleted.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- Only meeting owner can delete a meeting.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting successfully deleted.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingDTO.class))),
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
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id of the meeting to be deleted.", required = true)
            @PathVariable UUID mId)
    {
        logger.logRequest("Endpoint 'delete' called.");
        meetingService.delete(mId);
        return ResponseEntity.ok(null);
    }


    @GetMapping(value="{mId}/permissions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the MeetingPermissions object for the meeting.",
            description = "Get the MeetingPermissions object for the meeting.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- No access restriction required.<br/>" +
                    "- The endpoint is rate limited.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "MeetingPermissions successfully retrieved.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingPermissionsDTO.class))),
            @ApiResponse(responseCode = "404",
                    description = "Resource for given id not found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "429",
                    description = "Too many requests.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    public ResponseEntity<MeetingPermissionsDTO> getPermissions(
            @Parameter(description = "Id of the meeting to be evaluated.", required = true)
            @PathVariable UUID mId)
    {
        logger.logRequest("Endpoint 'getPermissions' called.");
        MeetingPermissionsDTO permissions = permissionControlService.getPermissions(mId);
        return ResponseEntity.ok(permissions);
    }


    @PostMapping(value = "",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a meeting.",
            description = "Create a meeting.<br/>" +
                    "When creating a meeting the owner will be automatically added as a participant.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- Only authenticated users are allowed.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting successfully created.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingDTO.class))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    @PreAuthorize("hasAuthority('default-roles-vk-bund')")
    public ResponseEntity<MeetingDTO> createMeeting(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The meeting to be created")
            @RequestBody MeetingCreateDTO meeting)
    {
        logger.logRequest("Endpoint 'createMeeting' called.");
        MeetingDTO saved = meetingService.create(meeting);
        return ResponseEntity.ok(saved);
    }


    @PutMapping(value = "{mId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a meeting.",
            description = "Update a meeting.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- For usual meetings: only owners are allowed.<br/>" +
                    "- For static rooms: owner and ORGANIZERs are allowed.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting successfully updated.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingDTO.class))),
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
    @PreAuthorize("hasAuthority('default-roles-vk-bund')")
    public ResponseEntity<MeetingDTO> updateMeeting(
            @Parameter(description = "Id of the meeting to be updated.", required = true)
            @PathVariable UUID mId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The meeting to be updated.")
            @RequestBody MeetingUpdateDTO meeting)
    {
        logger.logRequest("Endpoint 'updateMeeting' called.");
        MeetingDTO updated = meetingService.update(mId, meeting);
        return ResponseEntity.ok(updated);
    }


    @PatchMapping(value="{mId}/last-visit-date",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update the 'lastVisitDate' field (set with today's date) for the given static room.",
            description = "Update the 'lastVisitDate' field (set with today's date) for the given static room.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- No access restriction required.<br/>" +
                    "- The endpoint is rate limited.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting successfully updated."),
            @ApiResponse(responseCode = "429",
                    description = "Too many requests.",
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
    public ResponseEntity<Void> updateLastVisitDate(
            @Parameter(description = "The id of the meeting to be updated.", required = true)
            @PathVariable UUID mId)
    {
        logger.logRequest("Endpoint 'updateLastVisitDate' called.");
        meetingService.updateStaticRoomsLastVisitDate(mId);
        return ResponseEntity.ok(null);
    }


    @GetMapping(value="{mId}/next-of-series")
    @Operation(summary = "Get the next meeting (in restricted form) for the series given by the parent id.",
            description =
                    "Get the next meeting (in restricted form) for the series given by the parent id.<br/>" +
                    "If the given id is not a parent id or the parent id is not found, " +
                    "an ApiErrorDTO object with the respective error is returned.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- No access restriction required.<br/>" +
                    "- The endpoint is rate limited.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting successfully retrieved.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingBasicDTO.class))),
            @ApiResponse(responseCode = "429",
                    description = "Too many requests.",
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
    public ResponseEntity<MeetingBasicDTO> getNextOfSeries(
            @Parameter(description = "Id of the parent meeting for the series.", required = true)
            @PathVariable UUID mId)
    {
        logger.logRequest("Endpoint 'getNextOfSeries' called.");
        MeetingBasicDTO meeting = meetingService.findNextOfSeries(mId);
        return ResponseEntity.ok(meeting);
    }


    @PostMapping(value = "{mId}/jitsi-link",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = { MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE })
    @Operation(summary = "Get a Jitsi token for the given parameters.",
            description = "Get a Jitsi token for the given parameters.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- No access restriction required.<br/>" +
                    "- The endpoint is rate limited." +
                    "- If password is wrong, returns HTTP 403 code.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token successfully generated.",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "429",
                    description = "Too many requests.",
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
    public String generateLink(
            @Parameter(description = "Id of the meeting.", required = true)
            @PathVariable UUID mId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Necessary parameters for the token to be created.")
            @RequestBody JitsiTokenRequestDTO requestParams)
    {
        logger.logRequest("Endpoint 'generateLink' called.");
        return jitsiService.generateLink(mId,
                                             requestParams.getPassword(),
                                             requestParams.getEmail(),
                                             requestParams.getUserDisplayName(),
                                             requestParams.getTimezone());
    }


    @GetMapping("{mId}/participants")
    @Operation(summary = "Get the participants list for the given meeting.",
            description = "Get the participants list for the given meeting.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- For usual meetings: only owners are allowed.<br/>" +
                    "- For static rooms: owner and ORGANIZERs are allowed.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Participants list successfully retrieved.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MeetingParticipantDTO.class)))),
            @ApiResponse(responseCode = "401",
                    description = "Unauthorized access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "403",
                    description = "Forbidden access.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    @PreAuthorize("hasAuthority('default-roles-vk-bund')")
    public ResponseEntity<List<MeetingParticipantDTO>> listParticipants(
            @Parameter(description = "Id of the meeting to get the participants list.", required = true)
            @PathVariable UUID mId)
    {
        logger.logRequest("Endpoint 'listParticipants' called.");
        List<MeetingParticipantDTO> participantDTOS = participantService.findByMeetingId(mId);
        return ResponseEntity.ok(participantDTOS);
    }


    @PostMapping(value = "{mId}/participants",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a meeting participant for the given meeting.",
            description = "Create a meeting participant for the given meeting.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- For usual meetings: only meeting owners are allowed.<br/>" +
                    "- For static rooms: owner and ORGANIZERs are allowed.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting participant successfully created.",
                    content = @Content(schema = @Schema(implementation = MeetingParticipantDTO.class))),
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
    @PreAuthorize("hasAuthority('default-roles-vk-bund')")
    public ResponseEntity<MeetingParticipantDTO> createParticipant(
            @Parameter(description = "Id of the meeting.", required = true)
            @PathVariable UUID mId,
            @Parameter(description = "Request body with meeting participant data.", required = true)
            @RequestBody MeetingParticipantRequestDTO dto)
    {
        logger.logRequest("Endpoint 'createParticipant' called.");
        MeetingParticipantDTO created = participantService.create(mId, dto);
        return ResponseEntity.ok(created);
    }


    @PutMapping(value = "{mId}/participants/{pId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a meeting participant.",
            description = "Update a meeting participant.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- For usual meetings: only meeting owners are allowed.<br/>" +
                    "- For static rooms: owner and organizers are allowed.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting participant successfully updated.",
                    content = @Content(schema = @Schema(implementation = MeetingParticipantDTO.class))),
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
    @PreAuthorize("hasAuthority('default-roles-vk-bund')")
    public ResponseEntity<MeetingParticipantDTO> updateParticipant(
            @Parameter(description = "Id of the meeting.", required = true)
            @PathVariable UUID mId,
            @Parameter(description = "Id of the participant.", required = true)
            @PathVariable UUID pId,
            @Parameter(description = "Request body with meeting participant data.", required = true)
            @RequestBody MeetingParticipantRequestDTO dto)
    {
        logger.logRequest("Endpoint 'updateParticipant' called.");
        MeetingParticipantDTO participant = participantService.update(mId, pId, dto);
        return ResponseEntity.ok(participant);
    }


    @DeleteMapping(value = "{mId}/participants/{pId}")
    @Operation(summary = "Delete a meeting participant.",
            description = "Delete a meeting participant.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- For usual meetings: only meeting owners are allowed.<br/>" +
                    "- For static rooms: meeting owner and ORGANIZERs are allowed.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting participant successfully deleted.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
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
    @PreAuthorize("hasAuthority('default-roles-vk-bund')")
    public ResponseEntity<Void> deleteParticipant(
            @Parameter(description = "Id of the meeting.", required = true)
            @PathVariable UUID mId,
            @Parameter(description = "Id of the participant.", required = true)
            @PathVariable UUID pId)
    {
        logger.logRequest("Endpoint 'deleteParticipant' called.");
        participantService.delete(mId, pId);
        return ResponseEntity.ok(null);
    }


    @GetMapping("{userId}/registered-user")
    @Operation(summary = "Check if the user is registered in the authentication server.",
            description = "Check if the user is registered in the authentication server.<br/>" +
                    "<br/>Access Control:<br/>" +
                    "- Only authenticated users are allowed.<br/>" +
                    "- The endpoint is rate limited.")
    @ApiResponses( value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Check successfully done.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MeetingPermissionsDTO.class))),
            @ApiResponse(responseCode = "404",
                    description = "Resource for given id not found.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "429",
                    description = "Too many requests.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class))),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorDTO.class)))
    })
    @PreAuthorize("hasAuthority('default-roles-vk-bund')")
    public boolean isRegisteredUser(
            @Parameter(description = "Id (email) of the user to be checked.", required = true)
            @PathVariable String userId)
    {
        logger.logRequest("Endpoint 'isRegisteredUser' called.");
        return meetingService.isRegisteredUser(userId);
    }
}
