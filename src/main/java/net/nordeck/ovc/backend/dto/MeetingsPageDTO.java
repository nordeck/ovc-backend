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

import java.util.List;

@AllArgsConstructor
@Getter
@Schema(name = "MeetingsPage")
public class MeetingsPageDTO
{
    @Schema(description = "Total amount of pages.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("total-pages")
    private int totalPages;

    @Schema(description = "Total amount of items.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("total-items")
    private long totalItems;

    @Schema(description = "Amount of items per page.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("page-size")
    private int pageSize;

    @Schema(description = "Amount of the current page.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("page-number")
    int pageNumber;

    @Schema(description = "Amount of items in the current page.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("page-items")
    int pageItems;

    @Schema(description = "List of meetings.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("content")
    List<MeetingDTO> content;

}