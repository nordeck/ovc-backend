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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Meeting Permissions object.", name = "MeetingPermissions")
public class MeetingPermissionsDTO
{
    @Schema(description = "The unique id for the meeting record.", requiredMode = REQUIRED)
    @JsonProperty("meeting_id")
    private UUID meetingId;

    @Schema(description = "The unique user id.", requiredMode = REQUIRED)
    @JsonProperty("user_id")
    private String userId;

    @Schema(description = "Whether the user can read the given meeting.")
    @JsonProperty("user_can_read")
    private boolean userCanRead;

    @Schema(description = "Whether the user can edit the given meeting.")
    @JsonProperty("user_can_edit")
    private boolean userCanEdit;

    @Schema(description = "Whether the user is a participant of the given meeting.")
    @JsonProperty("user_is_participant")
    private boolean userIsParticipant;

}