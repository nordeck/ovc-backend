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

import net.nordeck.ovc.backend.dto.NotificationDTO;
import net.nordeck.ovc.backend.dto.NotificationsPageDTO;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    String DELETE_CANDIDATE = "delete-candidate";
    String PASSWORD_CHANGE_CANDIDATE = "password-change-candidate";
    String PARTICIPANT_ADDED = "participant-added";
    String PARTICIPANT_DELETED = "participant-deleted";
    String PASSWORD_CHANGED = "password-changed";

    NotificationDTO findById(UUID id);

    NotificationsPageDTO findAllForUser(PageRequest pageRequest);

    Void delete(UUID id);

    void createParticipantAddedNotifications(MeetingEntity meeting, List<MeetingParticipantEntity> participants);

    void createParticipantDeletedNotifications(MeetingEntity meeting, List<MeetingParticipantEntity> participants);

    void createDeleteCandidateNotifications(List<MeetingEntity> meetings);

    void createPasswordChangeCandidateNotifications(List<MeetingEntity> meetings);

    void createPasswordChangedNotifications(List<MeetingEntity> meetings);

    void updateView(UUID id);

    Void deleteAllForUser();

    void updateViewAll();
}

