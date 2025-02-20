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
import net.nordeck.ovc.backend.Constants;
import net.nordeck.ovc.backend.dto.MeetingParticipantDTO;
import net.nordeck.ovc.backend.dto.MeetingParticipantRequestDTO;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import net.nordeck.ovc.backend.repository.MeetingParticipantRepository;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.nordeck.ovc.backend.Constants.NO_MEETING_FOUND_FOR_ID;


@Service
public class MeetingParticipantServiceImpl implements MeetingParticipantService
{

    private final MeetingParticipantRepository participantRepository;

    @Lazy
    private final MeetingRepository meetingRepository;

    protected NotificationService notificationService;

    public MeetingParticipantServiceImpl(
            @Autowired MeetingParticipantRepository participantRepository,
            @Autowired MeetingRepository meetingRepository,
            @Autowired NotificationService notificationService)
    {
        this.participantRepository = participantRepository;
        this.meetingRepository = meetingRepository;
        this.notificationService = notificationService;
    }

    @Override
    @PreAuthorize("@permissionControlService.canReadMeeting(#meetingId)")
    public List<MeetingParticipantDTO> findByMeetingId(UUID meetingId)
    {
        List<MeetingParticipantDTO> participants = new ArrayList<>();
        List<MeetingParticipantEntity> entities = participantRepository.findAllByMeetingId(meetingId);
        for (MeetingParticipantEntity entity : entities)
        {
            MeetingParticipantDTO dto = MeetingParticipantDTO.buildFromEntity(entity);
            participants.add(dto);
        }
        return participants;
    }


    @Override
    @PreAuthorize("@permissionControlService.canEditMeeting(#meetingId)")
    public MeetingParticipantDTO create(UUID meetingId, MeetingParticipantRequestDTO dto)
    {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(NO_MEETING_FOUND_FOR_ID, meetingId))
        );

        Optional<MeetingParticipantEntity> result = meeting.getParticipants()
                .stream()
                .filter(p -> p.getEmail().equalsIgnoreCase(dto.getEmail()))
                .findFirst();
        if (result.isPresent())
        {
            throw new RuntimeException(String.format(Constants.ERR_PARTICIPANT_DUPLICATE, dto.getEmail()));
        }

        MeetingParticipantEntity entity = new MeetingParticipantEntity();
        entity.setEmail(dto.getEmail());
        entity.setRole(dto.getRole().getValue());
        entity.setMeetingId(meetingId);

        MeetingParticipantEntity saved = participantRepository.save(entity);
        if (meeting.isStaticRoom())
        {
            notificationService.createParticipantAddedNotifications(meeting, List.of(entity));
        }
        return MeetingParticipantDTO.buildFromEntity(saved);
    }


    @Override
    @PreAuthorize("@permissionControlService.canEditMeeting(#meetingId)")
    public MeetingParticipantDTO update(UUID meetingId, UUID participantId, MeetingParticipantRequestDTO dto)
    {
        MeetingParticipantEntity mpEntity = participantRepository.findById(participantId)
                .orElseThrow(EntityNotFoundException::new);
        mpEntity.setRole(dto.getRole().getValue());
        mpEntity.setEmail(dto.getEmail().toLowerCase());
        mpEntity = participantRepository.save(mpEntity);
        return MeetingParticipantDTO.buildFromEntity(mpEntity);
    }


    @Override
    @Transactional
    @PreAuthorize("@permissionControlService.canEditMeeting(#meetingId)")
    public void delete(UUID meetingId, UUID participantId)
    {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(NO_MEETING_FOUND_FOR_ID, meetingId))
        );
        MeetingParticipantEntity participant = participantRepository.findById(participantId).orElseThrow(
                () -> new EntityNotFoundException(String.format(Constants.ERR_NO_PARTICIPANT_FOUND_FOR_ID, participantId))
        );
        if (meeting.isStaticRoom())
        {
            notificationService.createParticipantDeletedNotifications(meeting, List.of(participant));
        }
        meeting.getParticipants().remove(participant);
        participantRepository.deleteById(participantId);
    }

}