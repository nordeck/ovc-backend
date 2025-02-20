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

import jakarta.persistence.EntityNotFoundException;
import net.nordeck.ovc.backend.dto.*;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.nordeck.ovc.backend.dto.Role.MODERATOR;

@Service
public class ConferenceMapperServiceImpl implements ConferenceMapperService
{

    protected MeetingRepository meetingRepository;

    @Lazy
    protected MeetingService meetingService;

    @Lazy
    protected JitsiService jitsiService;

    @Value("${jitsi.domain}")
    protected String jitsiHost;

    @Value("${sip.jibri.link}")
    protected String sipJibriLink;

    public ConferenceMapperServiceImpl(
            @Autowired MeetingRepository meetingRepository,
            @Autowired MeetingService meetingService,
            @Autowired JitsiService jitsiService)
    {
        this.meetingRepository = meetingRepository;
        this.meetingService = meetingService;
        this.jitsiService = jitsiService;
    }


    @Override
    public MapperJigasiResponseDTO findByJigasiConferenceId(String meetingId)
    {
        Optional<MeetingEntity> optional = meetingRepository.findById(UUID.fromString(meetingId));

        if (optional.isPresent())
        {
            MeetingEntity meeting = optional.get();
            MeetingDTO meetingDTO = DTOUtils.buildFromEntity(meeting);
            return new MapperJigasiResponseDTO(MAPPING_SUCCESSFUL, Long.valueOf(meetingDTO.getConferencePin()),
                                               meetingDTO.getId().toString());
        }
        else
        {
            return new MapperJigasiResponseDTO(MAPPING_NOT_FOUND, null, meetingId);
        }
    }


    @Override
    public MapperJigasiResponseDTO findByJigasiConferencePin(String conferencePin)
    {
        return findMeetingByConferencePin(conferencePin)
                .map((meeting) -> new MapperJigasiResponseDTO(MAPPING_SUCCESSFUL, Long.valueOf(conferencePin),
                                                              meeting.getId().toString()))
                .orElse(new MapperJigasiResponseDTO(MAPPING_NOT_FOUND, Long.valueOf(conferencePin), null));
    }


    @Override
    public MapperJibriResponseDTO findBySipJibriConferencePin(String conferencePin)
    {
        return findMeetingByConferencePin(conferencePin)
                .map((meeting) ->
                     {
                         MeetingParticipantDTO participant = new MeetingParticipantDTO();
                         participant.setMeetingId(meeting.getId());
                         participant.setRole(MODERATOR);
                         String token = jitsiService.generateToken(participant, SIP, SIP, meeting.getId(), null);
                         return new MapperJibriResponseDTO(jitsiHost, meeting.getId().toString(), token);
                     })
                .orElse(new MapperJibriResponseDTO());
    }

    protected Optional<MeetingBasicDTO> findMeetingByConferencePin(String conferencePin)
    {
        List<MeetingEntity> entities = meetingRepository.findByConferencePin(conferencePin);
        if (entities == null || entities.isEmpty())
        {
            return Optional.empty();
        }
        else if (entities.size() == 1)
        {
            return Optional.of(DTOUtils.buildBasicDTOFromEntity(entities.get(0)));
        }
        else
        {
            UUID parentId = null;
            for (MeetingEntity entity : entities)
            {
                if (entity.getParentId() != null)
                {
                    parentId = entity.getParentId();
                    break;
                }
            }
            try
            {
                return Optional.of(meetingService.findNextOfSeries(parentId));
            }
            catch (EntityNotFoundException ex)
            {
                return Optional.empty();
            }
        }
    }
}
