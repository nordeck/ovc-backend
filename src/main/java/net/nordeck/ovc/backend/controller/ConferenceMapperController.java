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
import io.swagger.v3.oas.annotations.tags.Tag;
import net.nordeck.ovc.backend.dto.MapperJibriResponseDTO;
import net.nordeck.ovc.backend.dto.MapperJigasiResponseDTO;
import net.nordeck.ovc.backend.logging.AppLogger;
import net.nordeck.ovc.backend.service.ConferenceMapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1.0/conference-mapper/")
@CrossOrigin(origins = "*",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.OPTIONS})
@Tag(name = "Conference Mapper API", description = "Conference Mapper operations")
public class ConferenceMapperController
{

    private final ConferenceMapperService service;

    private final AppLogger logger = new AppLogger("ConferenceMapperController");

    public ConferenceMapperController(@Autowired ConferenceMapperService service)
    {
        this.service = service;
    }

    @Operation(description = "Find the respective conference PIN for the given the meeting id - VOICE ONLY.")
    @ApiResponse(
            responseCode = "200",
            description = "The MapperJigasiResponseDTO object containing the respective conference PIN number.",
            content = @Content(
                    schema = @Schema(implementation = MapperJigasiResponseDTO.class)))
    @GetMapping("jigasi/by-meeting-id")
    public ResponseEntity<MapperJigasiResponseDTO> findByJigasiConference(
            @Parameter(
                    description = "Meeting id to be searched for.",
                    required = true)
            @RequestParam(name = "conference") String conference)
    {
        logger.logRequest("Endpoint 'findByJigasiConference' called.");
        try
        {
            String conferenceId = conference.substring(0, conference.indexOf("@"));
            return ResponseEntity.ok().body(service.findByJigasiConferenceId(conferenceId));
        }
        catch(StringIndexOutOfBoundsException ex)
        {
            throw new RuntimeException("The conference parameter must have the format '{UUID}@**'.", ex);
        }
    }

    @Operation(description = "Find the respective meeting id number for the given conference PIN - VOICE ONLY.")
    @ApiResponse(
            responseCode = "200",
            description = "The MapperJigasiResponseDTO object containing the respective conference PIN.",
            content = @Content(
                    schema = @Schema(implementation = MapperJigasiResponseDTO.class)))
    @GetMapping("jigasi/by-pin")
    public ResponseEntity<MapperJigasiResponseDTO> findByJigasiPin(
            @Parameter(
                    description = "Conference PIN to be searched for.",
                    required = true)
            @RequestParam(name = "id") String pin)
    {
        logger.logRequest("Endpoint 'findByJigasiPin' called.");
        return ResponseEntity.ok().body(service.findByJigasiConferencePin(pin));
    }

    @Operation(description = "Find the respective conference data for the conference PIN - VOICE & VIDEO.")
    @ApiResponse(
            responseCode = "200",
            description = "The MapperJibriResponseDTO object containing the respective conference data.",
            content = @Content(
                    schema = @Schema(implementation = MapperJibriResponseDTO.class)))
    @GetMapping("sipjibri/by-pin")
    public ResponseEntity<MapperJibriResponseDTO> findBySipJibriPin(
            @Parameter(
                    description = "Conference PIN to be searched for.",
                    required = true)
            @RequestParam(name = "pin") String pin)
    {
        logger.logRequest("Endpoint 'findBySipJibriPin' called.");
        return ResponseEntity.ok().body(service.findBySipJibriConferencePin(pin));
    }

}