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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import net.nordeck.ovc.backend.entity.NotificationEntity;

import java.time.ZonedDateTime;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static net.nordeck.ovc.backend.Constants.DATE_TIME_ISO_8601_FORMAT;

@Builder
@AllArgsConstructor
@Getter
@Schema(description = "Notification object.", name = "Notification")
public class NotificationDTO {

    @Schema(description = "Notification id.", requiredMode = REQUIRED)
    @JsonProperty("id")
    private UUID id;

    @Schema(description = "The message text.", requiredMode = REQUIRED)
    @JsonProperty("message")
    private String message;

    @Schema(description = "The user id / email.", requiredMode = REQUIRED)
    @JsonProperty("user_id")
    private String userId;

    @Schema(description = "The notification type: delete-candidate, password-change-candidate," +
            "participant-added, participant-deleted, or password-changed.", requiredMode = REQUIRED)
    @JsonProperty("type")
    private String type;
    // Notification used types:
    // 1. delete-candidate
    // 2. password-change-candidate
    // 3. participant-added
    // 4. participant-deleted
    // 5. password-changed

    @Schema(description = "If the message has been viewed.", requiredMode = REQUIRED)
    @JsonProperty("viewed")
    private boolean viewed;

    @Schema(description = "The message creation UTC datetime in ISO-8601 format. " +
            "Needs to be converted by the client with its own zone offset.", requiredMode = REQUIRED)
    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_ISO_8601_FORMAT)
    private ZonedDateTime createdAt;

    @Schema(description = "The message viewing UTC datetime in ISO-8601 format. " +
            "Needs to be converted by the client with its own zone offset.")
    @JsonProperty("viewed_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_ISO_8601_FORMAT)
    private ZonedDateTime viewedAt;

    @Schema(description = "The related meeting for this message.", requiredMode = REQUIRED)
    @JsonProperty("meeting_id")
    private UUID meetingId;

    @Schema(description = "The room name for this message.", requiredMode = REQUIRED)
    @JsonProperty("room_name")
    private String roomName;

    @Schema(description = "If type = 'password-change-candidate', this field contains the password change due date.")
    @JsonProperty("password_change_due_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_ISO_8601_FORMAT)
    private ZonedDateTime passwordChangeDueDate;

    @Schema(description = "If type = 'delete-candidate', this field contains the password change due date.")
    @JsonProperty("room_deletion_due_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_ISO_8601_FORMAT)
    private ZonedDateTime roomDeletionDueDate;

    public static NotificationDTO buildFromEntity(NotificationEntity entity) {

        return NotificationDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .meetingId(entity.getMeetingId())
                .type(entity.getType())
                .viewed(entity.isViewed())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .viewedAt(entity.getViewedAt())
                .roomName(entity.getRoomName())
                .roomDeletionDueDate(entity.getRoomDeletionDueDate())
                .passwordChangeDueDate(entity.getPasswordChangeDueDate())
                .build();
    }
}