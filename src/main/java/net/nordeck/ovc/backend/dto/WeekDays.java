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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class WeekDays
{
    @JsonProperty("monday")
    private boolean monday;

    @JsonProperty("tuesday")
    private boolean tuesday;

    @JsonProperty("wednesday")
    private boolean wednesday;

    @JsonProperty("thursday")
    private boolean thursday;

    @JsonProperty("friday")
    private boolean friday;

    @JsonProperty("saturday")
    private boolean saturday;

    @JsonProperty("sunday")
    private boolean sunday;

    public boolean equals(WeekDays other) {
        if (other == null) return false;
        if (other.monday != this.monday) return false;
        if (other.tuesday != this.tuesday) return false;
        if (other.wednesday != this.wednesday) return false;
        if (other.thursday != this.thursday) return false;
        if (other.friday != this.friday) return false;
        if (other.saturday != this.saturday) return false;
        if (other.sunday != this.sunday) return false;
        return true;
    }
}