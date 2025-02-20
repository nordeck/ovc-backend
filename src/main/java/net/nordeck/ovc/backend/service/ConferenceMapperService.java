package net.nordeck.ovc.backend.service;

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

import net.nordeck.ovc.backend.dto.MapperJibriResponseDTO;
import net.nordeck.ovc.backend.dto.MapperJigasiResponseDTO;

public interface ConferenceMapperService
{
    final static String MAPPING_SUCCESSFUL = "Successfully retrieved conference mapping";
    final static String MAPPING_NOT_FOUND = "No conference mapping was found";
    final static String SIP = "sip";

    /**
     * Find the mapping entry for Jigasi (audio-only) conference by the conference pin
     * @param conferencePin the conference pin
     * @return MapperJigasiResponseDTO
     */
    MapperJigasiResponseDTO findByJigasiConferencePin(String conferencePin);

    /**
     * Find the mapping entry for Jigasi (audio-only) conference by the conference pin
     * @param meetingId the UUID of the meeting entry
     * @return MapperJigasiResponseDTO
     */
    MapperJigasiResponseDTO findByJigasiConferenceId(String meetingId);

    /**
     * Find the mapping entry for Jibri (audio-and-video) conference by the conference pin
     * @param conferencePin the conference pin
     * @return MapperJibriResponseDTO
     */
    MapperJibriResponseDTO findBySipJibriConferencePin(String conferencePin);
}
