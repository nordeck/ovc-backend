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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import net.nordeck.ovc.backend.dto.NotificationDTO;
import net.nordeck.ovc.backend.dto.NotificationsPageDTO;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import net.nordeck.ovc.backend.entity.NotificationEntity;
import net.nordeck.ovc.backend.repository.NotificationRepository;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.nordeck.ovc.backend.dto.Role.ORGANIZER;

@Service
public class NotificationServiceImpl implements NotificationService
{

    private NotificationRepository notificationRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${portal.domain}")
    private String portalDomain;

    @Value("${portal.meeting-join-path}")
    private String meetingJoinPath;


    public NotificationServiceImpl(@Autowired NotificationRepository notificationRepository)
    {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @PreAuthorize("@permissionControlService.canReadNotification(#id)")
    public NotificationDTO findById(UUID id)
    {
        Optional<NotificationEntity> entityOptional = notificationRepository.findById(id);
        if (entityOptional.isEmpty())
        {
            throw new EntityNotFoundException(String.format("Could not find Notification for id <%s>.", id));
        }
        else
        {
            return NotificationDTO.buildFromEntity(entityOptional.get());
        }
    }

    @Override
    @PreAuthorize("@permissionControlService.isUserAuthenticated()")
    public NotificationsPageDTO findAllForUser(PageRequest pageRequest)
    {
        String userId = AuthenticatedUserService.getAuthenticatedUser();
        Page<NotificationEntity> page = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        List<NotificationDTO> content = page.getContent()
                .stream()
                .map(NotificationDTO::buildFromEntity).collect(Collectors.toList());
        return new NotificationsPageDTO(page.getTotalPages(), page.getTotalElements(),
                                                               page.getSize(), page.getNumber(),
                                                               content.size(), content);
    }

    @Override
    @PreAuthorize("@permissionControlService.canReadNotification(#id)")
    public Void delete(UUID id)
    {
        notificationRepository.deleteById(id);
        return null;
    }

    @Override
    @PreAuthorize("@permissionControlService.isUserAuthenticated()")
    public Void deleteAllForUser()
    {
        String userId = AuthenticatedUserService.getAuthenticatedUser();
        notificationRepository.deleteAllByUserId(userId);
        return null;
    }

    @Override
    public void createParticipantAddedNotifications(MeetingEntity meeting, List<MeetingParticipantEntity> participants)
    {
        if (participants != null && !participants.isEmpty())
        {
            List<NotificationEntity> notifications = new ArrayList<>();
            for (MeetingParticipantEntity participant : participants)
            {

                // do not create notifications for room owner
                if (participant.getEmail().equalsIgnoreCase(meeting.getOwnerId()))
                {
                    continue;
                }

                NotificationEntity notification = createNotification(meeting, participant, PARTICIPANT_ADDED);
                Message msg = new Message();
                msg.password = meeting.getPassword();
                msg.link = portalDomain + meetingJoinPath + meeting.getId();
                try
                {
                    notification.setMessage(objectMapper.writeValueAsString(msg));
                }
                catch (Exception ex)
                {
                    LogManager.getLogger().warn("Error creating notification JSON payload. Cause: {}", ex.getMessage());
                }
                notifications.add(notification);
            }
            if (!notifications.isEmpty())
            {
                notificationRepository.saveAll(notifications);
            }
        }
    }

    @Override
    public void createParticipantDeletedNotifications(MeetingEntity meeting, List<MeetingParticipantEntity> participants)
    {
        if (participants != null && !participants.isEmpty())
        {
            List<NotificationEntity> notifications = new ArrayList<>();
            for (MeetingParticipantEntity participant : participants)
            {

                // do not create notifications for room owner
                if (participant.getEmail().equalsIgnoreCase(meeting.getOwnerId()))
                {
                    continue;
                }

                NotificationEntity notification = createNotification(meeting, participant, PARTICIPANT_DELETED);
                notifications.add(notification);
            }
            if (!notifications.isEmpty())
            {
                notificationRepository.saveAll(notifications);
            }
        }
    }

    @Override
    public void createDeleteCandidateNotifications(List<MeetingEntity> meetings)
    {
        createCandidatesNotifications(meetings, DELETE_CANDIDATE);
    }

    @Override
    public void createPasswordChangeCandidateNotifications(List<MeetingEntity> meetings)
    {
        createCandidatesNotifications(meetings, PASSWORD_CHANGE_CANDIDATE);
    }

    @Override
    public void createPasswordChangedNotifications(List<MeetingEntity> meetings)
    {
        List<NotificationEntity> notifications = new ArrayList<>();
        if (meetings != null && !meetings.isEmpty())
        {
            for (MeetingEntity meeting : meetings)
            {
                for (MeetingParticipantEntity participant : meeting.getParticipants())
                {
                    if (ORGANIZER.getValue().equals(participant.getRole()))
                    {
                        NotificationEntity notification = createNotification(meeting, participant, PASSWORD_CHANGED);
                        notification.setPasswordChangeDueDate(ZonedDateTime.now());
                        notifications.add(notification);
                    }
                }
            }
        }
        if (!notifications.isEmpty())
        {
            notificationRepository.saveAll(notifications);
        }
    }

    @Override
    @PreAuthorize("@permissionControlService.canReadNotification(#id)")
    public void updateView(UUID id)
    {
        Optional<NotificationEntity> opt = notificationRepository.findById(id);
        if (opt.isPresent())
        {
            NotificationEntity notification = opt.get();
            notification.setViewed(true);
            notification.setViewedAt(ZonedDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @PreAuthorize("@permissionControlService.isUserAuthenticated()")
    public void updateViewAll()
    {
        String userId = AuthenticatedUserService.getAuthenticatedUser();
        List<NotificationEntity> notifications = notificationRepository.findAllByUserId(userId);
        List<NotificationEntity> toUpdate = new ArrayList<>();
        for (NotificationEntity notification : notifications)
        {
            if (!notification.isViewed())
            {
                notification.setViewed(true);
                notification.setViewedAt(ZonedDateTime.now());
                toUpdate.add(notification);
            }
        }
        notificationRepository.saveAll(toUpdate);
    }

    private void createCandidatesNotifications(List<MeetingEntity> meetings, String type)
    {
        List<NotificationEntity> notifications = new ArrayList<>();
        if (meetings != null && !meetings.isEmpty())
        {
            for (MeetingEntity meeting : meetings)
            {
                for (MeetingParticipantEntity participant : meeting.getParticipants())
                {
                    if (ORGANIZER.getValue().equals(participant.getRole()))
                    {
                        NotificationEntity notification = createNotification(meeting, participant, type);
                        notifications.add(notification);
                    }
                }
            }
        }
        if (!notifications.isEmpty())
        {
            notificationRepository.saveAll(notifications);
        }
    }

    private static NotificationEntity createNotification(MeetingEntity meeting,
                                                         MeetingParticipantEntity participant,
                                                         String type)
    {
        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(participant.getEmail());
        notification.setMeetingId(meeting.getId());
        notification.setType(type);
        notification.setRoomName(meeting.getName());
        notification.setMessage("");
        notification.setPasswordChangeDueDate(meeting.getPasswordChangeDueDate());
        notification.setRoomDeletionDueDate(meeting.getRoomDeletionDueDate());
        notification.setCreatedAt(ZonedDateTime.now());
        return notification;
    }

    public static class Message
    {
        public String link;
        public String password;
    }

}