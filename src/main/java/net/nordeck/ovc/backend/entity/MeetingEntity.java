package net.nordeck.ovc.backend.entity;

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

import jakarta.persistence.*;
import lombok.*;
import net.nordeck.ovc.backend.Constants;
import net.nordeck.ovc.backend.dto.MeetingAbstractDTO;
import net.nordeck.ovc.backend.dto.WeekDays;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static net.nordeck.ovc.backend.dto.DTOUtils.*;
import static net.nordeck.ovc.backend.dto.MeetingType.INSTANT;
import static net.nordeck.ovc.backend.dto.MeetingType.STATIC;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "meeting")
public class MeetingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "instant_meeting")
    private boolean instantMeeting;

    @Column(name = "owner_id")
    private String ownerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "info")
    private String info;

    @Column(name = "frequency")
    private String frequency;

    @Column(name = "custom_days_mo")
    private boolean customDays_monday;

    @Column(name = "custom_days_tu")
    private boolean customDays_tuesday;

    @Column(name = "custom_days_we")
    private boolean customDays_wednesday;

    @Column(name = "custom_days_th")
    private boolean customDays_thursday;

    @Column(name = "custom_days_fr")
    private boolean customDays_friday;

    @Column(name = "custom_days_sa")
    private boolean customDays_saturday;

    @Column(name = "custom_days_su")
    private boolean customDays_sunday;

    @Column(name = "start_time")
    private ZonedDateTime startTime;

    @Column(name = "end_time")
    private ZonedDateTime endTime;

    @Column(name = "series_end_time")
    private ZonedDateTime seriesEndTime;

    @Column(name = "created_at")
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private ZonedDateTime updatedAt;

    @Column(name = "password")
    private String password;

    @Column(name = "lobby_enabled")
    private boolean lobbyEnabled;

    @Column(name = "static_room")
    private boolean staticRoom;

    @Column(name = "last_visit_date")
    @CreationTimestamp
    private ZonedDateTime lastVisitDate;

    @Column(name = "delete_candidate")
    private boolean deleteCandidate;

    @Column(name = "last_password_change")
    @CreationTimestamp
    private ZonedDateTime lastPasswordChange;

    @Column(name = "password_change_candidate")
    private boolean passwordChangeCandidate;

    @Column(name = "has_organizer")
    private boolean hasOrganizer;

    @Column(name = "room_deletion_due_date")
    private ZonedDateTime roomDeletionDueDate;

    @Column(name = "password_change_due_date")
    private ZonedDateTime passwordChangeDueDate;

    @Column(name = "conference_pin", unique = true)
    private String conferencePin;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "sip_jibri_link")
    private String sipJibriLink;

    @Column(name = "excluded")
    private boolean excluded;

    @OneToMany(mappedBy = "meetingId", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<MeetingParticipantEntity> participants;

    @OneToMany(mappedBy = "parentId", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<MeetingEntity> children;

    @OneToMany(mappedBy = "meetingId", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<NotificationEntity> notifications;

    public static MeetingEntity buildFromMeetingAbstractDTO(MeetingAbstractDTO dto)
    {
        String frequency = getFrequencyValue(dto);

        MeetingEntity entity = MeetingEntity.builder()
                .instantMeeting(INSTANT.equals(dto.getType()))
                .staticRoom(STATIC.equals(dto.getType()))
                .name(StringUtils.trim(dto.getName()))
                .info(StringUtils.trim(dto.getInfo()))
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .frequency(frequency)
                .password(dto.getPassword())
                .lobbyEnabled(dto.isLobbyEnabled())
                .build();

        setRecurrence(dto, entity);
        return entity;
    }

    public boolean isRecurrentChild()
    {
        return this.parentId != null;
    }

    public boolean isRecurrentParent()
    {
        return !Constants.FREQUENCY_ONCE.equals(this.frequency) && this.parentId == null;
    }

    public boolean isSingleMeeting()
    {
        return !isRecurrentChild() && !isRecurrentParent() && Constants.FREQUENCY_ONCE.equals(frequency);
    }

    protected static void setRecurrence(MeetingAbstractDTO dto, MeetingEntity entity)
    {
        if (dto.getRecurrence() != null)
        {
            entity.setSeriesEndTime(dto.getRecurrence().getEndDate());
            if (hasWeeklyRecurrence(dto))
            {
                WeekDays weekDays = dto.getRecurrence().getWeekDays();
                entity.setCustomDays_monday(weekDays.isMonday());
                entity.setCustomDays_tuesday(weekDays.isTuesday());
                entity.setCustomDays_wednesday(weekDays.isWednesday());
                entity.setCustomDays_thursday(weekDays.isThursday());
                entity.setCustomDays_friday(weekDays.isFriday());
                entity.setCustomDays_saturday(weekDays.isSaturday());
                entity.setCustomDays_sunday(weekDays.isSunday());
            }

            if (hasCustomWeekDays(dto))
            {
                entity.setFrequency(Constants.FREQUENCY_CUSTOM);
            }
            else
            {
                entity.setFrequency(dto.getRecurrence().getFrequency().getValue());
            }
        }
    }
}