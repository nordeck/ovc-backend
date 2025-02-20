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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Schema(description = "Meeting Jitsi token request object.", name = "JitsiTokenRequest")
public class JitsiTokenRequestDTO
{
    @Schema(description = "The meeting's password.", requiredMode = REQUIRED)
    @JsonProperty("password")
    private String password;

    @Schema(description = "The participant's email.", requiredMode = NOT_REQUIRED)
    @JsonProperty("email")
    private String email;

    @Schema(description = "The user's display name.", requiredMode = REQUIRED)
    @JsonProperty("user_display_name")
    private String userDisplayName;

    @Schema(description = "The user's time zone.", requiredMode = REQUIRED)
    @JsonProperty("timezone")
    private String timezone;
}