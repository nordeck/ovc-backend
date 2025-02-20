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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.nordeck.ovc.backend.Constants;

import java.time.ZonedDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Schema(description = "Meeting recurrence", name = "Recurrence")
public class RecurrenceDTO {

    @Schema(description = "Type of recurrence.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("frequency")
    private RecurrenceFrequency frequency;

    @Schema(description = "End UTC datetime in ISO-8601 format for recurrence in an inclusive manner. " +
            "Needs to be converted by the client with proper zone offset.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("end_date")
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.DATE_TIME_ISO_8601_FORMAT)
    private ZonedDateTime endDate;

    @Schema(description = "Days of the week. Currently is used only for the frequency WEEKLY.",
            requiredMode = NOT_REQUIRED, nullable = true)
    @JsonProperty("week_days")
    private WeekDays weekDays;

    public boolean equals(RecurrenceDTO other)
    {
        if (other == null) return false;
        if (!this.getFrequency().equals(other.getFrequency())) return false;
        if (!this.getEndDate().equals(other.getEndDate())) return false;
        if (this.getWeekDays() != null)
        {
            return this.getWeekDays().equals(other.getWeekDays());
        }
        return true;
    }

}