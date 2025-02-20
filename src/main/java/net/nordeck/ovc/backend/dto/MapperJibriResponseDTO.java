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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
/**
 * Mapping DTO for audio-and-video Jibri plugin
 */
@Schema(description = "Jibri mapper response object", name = "MapperJibriResponse")
public class MapperJibriResponseDTO {

    @Schema(description = "Jibri host")
    String host;

    @Schema(description = "Room ID (Meeting ID)")
    String room;

    @Schema(description = "Jitsi JWT token")
    String token;

}
