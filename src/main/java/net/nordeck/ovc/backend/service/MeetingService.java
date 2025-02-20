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

import net.nordeck.ovc.backend.dto.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.ZonedDateTime;
import java.util.UUID;

public interface MeetingService
{

    MeetingDTO findById(UUID meetingId);

    MeetingDTO create(MeetingCreateDTO dto);

    MeetingDTO update(UUID meetingId, MeetingUpdateDTO meeting);

    MeetingsPageDTO getMeetingsPage(String type, Integer offset, Integer pageSize, String order, ZonedDateTime startDateTime, ZonedDateTime endDateTime);

    MeetingBasicDTO findNextOfSeries(UUID parentId);

    boolean isRegisteredUser(String email);

    void delete(UUID meetingId);

    void updateStaticRoomsLastVisitDate(UUID meetingId);

    static String generatePassword(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*-=+?";
        return RandomStringUtils.random(length, characters);
    }

    MeetingBasicDTO findBasicById(UUID mId);

}
