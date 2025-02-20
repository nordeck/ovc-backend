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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Setter
@Getter
@Schema(description = "API Error object.", name = "ApiError")
public class ApiErrorDTO
{
    @Schema(description = "Error id.", requiredMode = REQUIRED)
    private UUID id;

    @Schema(description = "HTTP Status.", requiredMode = REQUIRED)
    private HttpStatus status;

    @Schema(description = "HTTP status code.", requiredMode = REQUIRED)
    private Integer code;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    @Schema(description = "Date / time the error occurred.", requiredMode = REQUIRED)
    private ZonedDateTime timestamp;

    @Schema(description = "Error message.", requiredMode = REQUIRED)
    private String message;

    @Schema(description = "Info message.", requiredMode = REQUIRED, name = "info_message")
    private String infoMessage;

    @Schema(description = "The path called.", requiredMode = REQUIRED)
    private String path;

    private ApiErrorDTO()
    {
        id = UUID.randomUUID();
        timestamp = ZonedDateTime.now();
    }

    public ApiErrorDTO(HttpStatus status)
    {
        this();
        this.setStatus(status);
        this.setCode(status.value());
    }

    public ApiErrorDTO(HttpStatus status, Throwable ex)
    {
        this();
        this.setStatus(status);
        this.setCode(status.value());
        this.setMessage("Unexpected error.");
        this.setInfoMessage(ex.getLocalizedMessage());
    }

    public ApiErrorDTO(HttpStatus status, String message, Throwable ex)
    {
        this();
        this.setStatus(status);
        this.setCode(status.value());
        this.setMessage(message);
        this.setInfoMessage(ex.getLocalizedMessage());
    }
}