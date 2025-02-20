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
import net.nordeck.ovc.backend.dto.MeetingPermissionsDTO;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import net.nordeck.ovc.backend.entity.NotificationEntity;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import net.nordeck.ovc.backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.nordeck.ovc.backend.Constants.NO_MEETING_FOUND_FOR_ID;
import static net.nordeck.ovc.backend.dto.Role.ORGANIZER;

@Service("permissionControlService")
public class PermissionControlServiceImpl implements PermissionControlService
{

    @Lazy
    protected final MeetingRepository meetingRepository;

    @Lazy
    protected final NotificationRepository notificationRepository;

    public PermissionControlServiceImpl(
            @Autowired MeetingRepository meetingRepository,
            @Autowired NotificationRepository notificationRepository)
    {
        this.meetingRepository = meetingRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public boolean canCreateRecords()
    {
        return isUserAuthenticated();
    }

    @Override
    public boolean canReadMeeting(UUID meetingId)
    {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(NO_MEETING_FOUND_FOR_ID, meetingId)));
        return this.canReadMeeting(meeting);
    }

    private boolean canReadMeeting(MeetingEntity meeting)
    {
        String userId = AuthenticatedUserService.getAuthenticatedUser();
        Optional<MeetingParticipantEntity> result = meeting.getParticipants()
                .stream()
                .filter(p -> p.getEmail().equalsIgnoreCase(userId))
                .findFirst();
        return result.isPresent();
    }

    @Override
    public boolean canEditMeeting(UUID meetingId)
    {
        if (meetingId == null)
        {
            return canCreateRecords();
        }
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(NO_MEETING_FOUND_FOR_ID, meetingId)));
        return this.canEditMeeting(meeting);
    }

    private boolean canEditMeeting(MeetingEntity meeting)
    {
       if (meeting.isStaticRoom())
       {
           return canEditStaticRoom(meeting);
       }
       else
       {
           return canEditUsualMeeting(meeting);
       }
    }

    private boolean canEditUsualMeeting(MeetingEntity meeting)
    {
        String userId = AuthenticatedUserService.getAuthenticatedUser();
        return isOwner(meeting, userId);
    }

    private boolean canEditStaticRoom(MeetingEntity meeting)
    {
        String userId = AuthenticatedUserService.getAuthenticatedUser();
        List<MeetingParticipantEntity> participants = meeting.getParticipants();
        if (participants == null || participants.isEmpty())
        {
            return false;
        }
        Optional<MeetingParticipantEntity> result = participants
                .stream()
                .filter(p -> p.getEmail().equalsIgnoreCase(userId) && ORGANIZER.getValue().equalsIgnoreCase(p.getRole()))
                .findFirst();
        return result.isPresent();
    }

    @Override
    public MeetingPermissionsDTO getPermissions(UUID meetingId)
    {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(NO_MEETING_FOUND_FOR_ID, meetingId)));

        String userId = AuthenticatedUserService.getAuthenticatedUser();
        MeetingPermissionsDTO permissions = new MeetingPermissionsDTO();
        permissions.setUserCanRead(canReadMeeting(meeting));
        permissions.setUserCanEdit(canEditMeeting(meeting));
        permissions.setUserIsParticipant(permissions.isUserCanRead());
        permissions.setMeetingId(meetingId);
        permissions.setUserId(userId);
        return permissions;
    }

    public boolean canReadNotification(UUID id)
    {
        String userId = AuthenticatedUserService.getAuthenticatedUser();
        Optional<NotificationEntity> notificationOpt = notificationRepository.findById(id);
        if (notificationOpt.isPresent())
        {
            NotificationEntity notification = notificationOpt.get();
            return notification.getUserId().equalsIgnoreCase(userId);
        }
        return false;
    }

    public boolean canEditNotification(UUID notificationId)
    {
        // if the record is new, don't verify edit permission
        String email = AuthenticatedUserService.getAuthenticatedUser();
        if (notificationId == null) {
            return canCreateRecords();
        }
        // otherwise, only notification owners can save their records
        Optional<NotificationEntity> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent())
        {
            NotificationEntity notification = notificationOpt.get();
            return notification.getUserId().equalsIgnoreCase(email);
        }
        return false;
    }

    @Override
    public boolean isUserAuthenticated()
    {
        try
        {
            AuthenticatedUserService.getAuthenticatedUser();
        }
        catch(AccessDeniedException ex)
        {
            return false;
        }
        return true;
    }

    private boolean isOwner(MeetingEntity meeting, String userId)
    {
        return meeting.getOwnerId().equalsIgnoreCase(userId);
    }

}