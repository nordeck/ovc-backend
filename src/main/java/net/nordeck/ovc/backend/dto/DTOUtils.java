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

import net.nordeck.ovc.backend.Constants;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DTOUtils
{
    public static MeetingDTO buildFromEntity(MeetingEntity entity)
    {
        List<MeetingParticipantDTO> participantDTOs = new ArrayList<>();
        List<MeetingParticipantEntity> participantEntities = entity.getParticipants();
        if (participantEntities != null && !participantEntities.isEmpty())
        {
            participantEntities.forEach(p -> participantDTOs.add(MeetingParticipantDTO.buildFromEntity(p)));
        }

        MeetingDTO dto = MeetingDTO.builder()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .parentId(entity.getParentId())
                .conferencePin(entity.getConferencePin())
                .phoneNumber(entity.getPhoneNumber())
                .sipJibriLink(entity.getSipJibriLink())
                .participants(participantDTOs).build();

        setBasicDataFromEntity(dto, entity);

        dto.setRecurrence(buildRecurrenceFromEntity(entity));

        List<MeetingEntity> childEntities = entity.getChildren();
        if (childEntities != null && !childEntities.isEmpty())
        {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATE_TIME_ISO_8601_FORMAT);
            List<String> excludedDates = new ArrayList<>();
            List<MeetingEntity> excluded = childEntities.stream().filter(MeetingEntity::isExcluded).toList();
            excluded.forEach( m -> excludedDates.add(formatter.format(m.getStartTime())));
            dto.setExcludedDates(excludedDates);
        }
        return dto;
    }

    public static MeetingBasicDTO buildBasicDTOFromEntity(MeetingEntity entity)
    {
        MeetingBasicDTO dto = new MeetingBasicDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setType(getTypeFromEntity(entity));
        dto.setRecurrence(buildRecurrenceFromEntity(entity));
        return dto;
    }

    public static MeetingUpdateDTO buildUpdateDTOFromEntity(MeetingEntity entity)
    {
        MeetingUpdateDTO dto = new MeetingUpdateDTO();
        setBasicDataFromEntity(dto, entity);
        RecurrenceDTO recurrence = buildRecurrenceFromEntity(entity);
        dto.setRecurrence(recurrence);
        return dto;
    }

    public static boolean hasCustomWeekDays(MeetingAbstractDTO dto)
    {
        if (dto.getRecurrence() == null)
        {
            return false;
        }
        WeekDays weekDays = dto.getRecurrence().getWeekDays();
        if (weekDays == null)
        {
            return false;
        }
        int hits = 0;
        if (weekDays.isMonday())
        {
            hits++;
        }
        if (weekDays.isTuesday())
        {
            hits++;
        }
        if (weekDays.isWednesday())
        {
            hits++;
        }
        if (weekDays.isThursday())
        {
            hits++;
        }
        if (weekDays.isFriday())
        {
            hits++;
        }
        if (weekDays.isSaturday())
        {
            hits++;
        }
        if (weekDays.isSunday())
        {
            hits++;
        }
        return hits >= 1;
    }

    public static String getFrequencyValue(MeetingAbstractDTO dto)
    {
        String frequency = Frequency.ONCE.getValue();
        if (dto.getRecurrence() != null && dto.getRecurrence().getWeekDays() != null)
        {
            frequency = dto.getRecurrence().getFrequency().getValue();
            if (hasCustomWeekDays(dto))
            {
                frequency = Frequency.CUSTOM.getValue();
            }
        }
        return frequency;
    }

    public static boolean hasWeeklyRecurrence(MeetingAbstractDTO dto)
    {
        if (dto.getRecurrence() != null)
        {
            return RecurrenceFrequency.WEEKLY.equals(dto.getRecurrence().getFrequency());
        }
        return false;
    }

    public static boolean isRecurrentMeeting(MeetingAbstractDTO dto)
    {
        return isNormalMeeting(dto) && dto.getRecurrence() != null;
    }

    public static boolean isRecurrentParentMeeting(MeetingDTO dto)
    {
        return isRecurrentMeeting(dto) && dto.getParentId() == null;
    }

    public static boolean isNormalMeeting(MeetingAbstractDTO dto)
    {
        return MeetingType.NORMAL.equals(dto.getType());
    }

    protected static void setBasicDataFromEntity(MeetingAbstractDTO dto, MeetingEntity entity)
    {
        dto.setName(entity.getName());
        dto.setInfo(entity.getInfo());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setLobbyEnabled(entity.isLobbyEnabled());
        dto.setPassword(entity.getPassword());
        dto.setType(getTypeFromEntity(entity));
        dto.setStartedAt(entity.getStartedAt());
    }

    protected static RecurrenceDTO buildRecurrenceFromEntity(MeetingEntity entity)
    {
        RecurrenceDTO recurrence = null;
        if (Frequency.ONCE.getValue().equals(entity.getFrequency()))
        {
            return null;
        }
        else if (entity.isRecurrentParent() || entity.getParentId() != null)
        {
            recurrence = new RecurrenceDTO();
            RecurrenceFrequency freq;
            if (Frequency.CUSTOM.getValue().equals(entity.getFrequency()))
            {
                WeekDays weekDays = new WeekDays();
                freq = RecurrenceFrequency.WEEKLY;
                weekDays.setMonday(entity.isCustomDays_monday());
                weekDays.setTuesday(entity.isCustomDays_tuesday());
                weekDays.setWednesday(entity.isCustomDays_wednesday());
                weekDays.setThursday(entity.isCustomDays_thursday());
                weekDays.setFriday(entity.isCustomDays_friday());
                weekDays.setSaturday(entity.isCustomDays_saturday());
                weekDays.setSunday(entity.isCustomDays_sunday());
                recurrence.setWeekDays(weekDays);
            }
            else
            {
                freq = RecurrenceFrequency.valueOf(entity.getFrequency());
            }
            recurrence.setFrequency(freq);
            recurrence.setEndDate(entity.getSeriesEndTime());
        }
        return recurrence;
    }

    private static MeetingType getTypeFromEntity(MeetingEntity entity)
    {
        if (entity.isStaticRoom())
        {
            return MeetingType.STATIC;
        }
        else if (entity.isInstantMeeting())
        {
            return MeetingType.INSTANT;
        }
        return MeetingType.NORMAL;
    }
}
