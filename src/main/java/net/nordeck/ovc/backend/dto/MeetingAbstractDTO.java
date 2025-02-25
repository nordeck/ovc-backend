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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.nordeck.ovc.backend.Constants;

import java.time.ZonedDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@NoArgsConstructor
@Getter
@Setter
public class MeetingAbstractDTO
{

    @Schema(description = "Meeting type.", requiredMode = REQUIRED)
    @JsonProperty("type")
    private MeetingType type;

    @Schema(description = "Name of the meeting.", requiredMode = REQUIRED)
    @JsonProperty("name")
    private String name;

    @Schema(description = "Information about this meeting.")
    @JsonProperty("info")
    private String info;

    @Schema(description = "Meeting start UTC datetime in ISO-8601 format. " +
            "Needs to be converted by the client with its own zone offset. " +
            "Required for NORMAL meeting.")
    @JsonProperty("start_time")
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.DATE_TIME_ISO_8601_FORMAT)
    private ZonedDateTime startTime;

    @Schema(description = "Meeting end UTC datetime in ISO-8601 format. " +
            "Needs to be converted by the client with its own zone offset. " +
            "Required for NORMAL meeting.")
    @JsonProperty("end_time")
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.DATE_TIME_ISO_8601_FORMAT)
    private ZonedDateTime endTime;

    @Schema(description = "Whether this meeting meeting happens daily, weekly or with a custom recurrence. " +
            "Could be set for NORMAL meeting only. " +
            "If the meeting happens only once, the recurrence is defined as null.",
            requiredMode = NOT_REQUIRED, nullable = true)
    @JsonProperty("recurrence")
    private RecurrenceDTO recurrence;

    @Schema(description = "Password for joining the meeting.", requiredMode = REQUIRED)
    @JsonProperty("password")
    private String password;

    @Schema(description = "Whether this meeting shows the lobby before entering it.", requiredMode = REQUIRED)
    @JsonProperty("lobby_enabled")
    private boolean lobbyEnabled;

    @Schema(description = "Whether this meeting has been started." +
            "Relevant for instant meetings.")
    @JsonProperty("started")
    private boolean started;

    @Schema(description = "The time UTC datetime in ISO-8601 format the meeting has been started. " +
            "Needs to be converted by the client with its own zone offset. " +
            "Relevant for INSTANT meeting.")
    @JsonProperty("started_at")
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.DATE_TIME_ISO_8601_FORMAT)
    private ZonedDateTime startedAt;

}