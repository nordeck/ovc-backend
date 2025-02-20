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
import lombok.*;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Schema(description = "Meeting participant object.", name = "MeetingParticipant")
public class MeetingParticipantDTO {

    @Schema(description = "Meeting participant id.", requiredMode = REQUIRED)
    @JsonProperty("id")
    private UUID id;

    @Schema(description = "Related Meeting id for the participant record.", requiredMode = REQUIRED)
    @JsonProperty("meeting_id")
    private UUID meetingId;

    @Schema(description = "Role of the participant in the respective meeting.", requiredMode = REQUIRED)
    @JsonProperty("role")
    private Role role;

    @Schema(description = "Email of the participant.", requiredMode = REQUIRED)
    @JsonProperty("email")
    private String email;

    public static MeetingParticipantDTO buildFromEntity(MeetingParticipantEntity entity)
    {
        return MeetingParticipantDTO.builder()
                .id(entity.getId())
                .meetingId(entity.getMeetingId())
                .email(entity.getEmail().trim())
                .role(Role.fromValue(entity.getRole()))
                .build();
    }

    public static List<MeetingParticipantDTO> buildFromEntity(List<MeetingParticipantEntity> entities)
    {
        List<MeetingParticipantDTO> dtos = new ArrayList<>();
        for (MeetingParticipantEntity entity : entities)
        {
            MeetingParticipantDTO dto = buildFromEntity(entity);
            dtos.add(dto);
        }
        return dtos;
    }
}