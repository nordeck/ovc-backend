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

/**
 * Mapping DTO for audio-only Jigasi plugin
 */

@Getter
@AllArgsConstructor
@Schema(description = "Jigasi mapper response object", name = "MapperJigasiResponse")
public class MapperJigasiResponseDTO {

    @Schema(description = "Response message")
    String message;

    @Schema(description = "Jigasi mapper conference PIN")
    Long id;

    @Schema(description = "Jigasi mapper conference ID (meeting ID)")
    String conference;

}