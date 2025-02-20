package net.nordeck.ovc.backend.dto;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@AllArgsConstructor
@Builder
@Getter
@Setter
@Schema(description = "Meeting full object. " +
        "The record might be a meeting, an instant meeting, or a static room.",
        name = "Meeting")
public class MeetingDTO extends MeetingAbstractDTO
{

    @Schema(description = "The id for the meeting record.", requiredMode = REQUIRED)
    @JsonProperty("id")
    private UUID id;

    @Schema(description = "The unique id for the parent record, if the records represents a child from a series of " +
            "meeting. Can be set for NORMAL meeting only.")
    @JsonProperty("parent_id")
    private UUID parentId;

    @Schema(description = "The owners user id / email.", requiredMode = REQUIRED)
    @JsonProperty("owner_id")
    private String ownerId;

    @Schema(description = "The SIP PIN for the meeting - must be unique.", requiredMode = REQUIRED)
    @JsonProperty("conference_pin")
    private String conferencePin;

    @Schema(description = "The SIP phone number for the voice only conference.", requiredMode = REQUIRED)
    @JsonProperty("phone_number")
    private String phoneNumber;

    @Schema(description = "The SIP link for the voice and video conference.", requiredMode = REQUIRED)
    @JsonProperty("sip_jibri_link")
    private String sipJibriLink;

    @Schema(description = "The list of meeting participants.")
    private List<MeetingParticipantDTO> participants;

    @Schema(description = "The list of UTC datetimes in ISO-8601 format of excluded child recurrent meetings. " +
            "It is set only on recurrent parent meetings, after child meetings have been excluded.")
    @JsonProperty("excluded_dates")
    private List<String> excludedDates;

}