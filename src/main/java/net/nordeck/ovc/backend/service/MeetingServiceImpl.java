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
import lombok.SneakyThrows;
import net.nordeck.ovc.backend.Constants;
import net.nordeck.ovc.backend.dto.*;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import net.nordeck.ovc.backend.repository.MeetingParticipantRepository;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import net.nordeck.ovc.backend.repository.NotificationRepository;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static net.nordeck.ovc.backend.dto.DTOUtils.isNormalMeeting;
import static net.nordeck.ovc.backend.dto.DTOUtils.isRecurrentMeeting;
import static net.nordeck.ovc.backend.dto.Frequency.CUSTOM;
import static net.nordeck.ovc.backend.dto.Role.MODERATOR;
import static net.nordeck.ovc.backend.dto.Role.ORGANIZER;

@Service
public class MeetingServiceImpl implements MeetingService
{

    public static final String COLUMN_CREATED_AT = "createdAt";
    public static final String COLUMN_START_TIME = "startTime";
    public static final String COLUMN_NAME = "name";

    protected MeetingRepository meetingRepository;

    @Lazy
    protected MeetingParticipantRepository participantRepository;

    @Lazy
    protected NotificationRepository notificationRepository;

    @Lazy
    protected NotificationService notificationService;

    protected KeycloakClientService keycloakClientService;

    @Value("${sip.phone.number}")
    protected String sipPhoneNumber;

    @Value("${jitsi.domain}")
    protected String jitsiHost;

    @Value("${sip.jibri.link}")
    protected String sipJibriLink;


    public MeetingServiceImpl(@Autowired MeetingRepository meetingRepository,
                              @Autowired MeetingParticipantRepository participantRepository,
                              @Autowired NotificationRepository notificationRepository,
                              @Autowired NotificationService notificationService,
                              @Autowired KeycloakClientService keycloakClientService)
    {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.keycloakClientService = keycloakClientService;
    }


    @Override
    @PreAuthorize("@permissionControlService.canCreateRecords()")
    @Transactional
    public MeetingDTO create(MeetingCreateDTO dto)
    {
        String userId = AuthenticatedUserService.getAuthenticatedUser();
        MeetingEntity entity = MeetingEntity.buildFromMeetingAbstractDTO(dto);
        entity.setOwnerId(userId);
        entity.setConferencePin(getUniqueConferencePin());
        entity.setPhoneNumber(sipPhoneNumber);
        entity.setSipJibriLink(sipJibriLink);
        entity.setCreatedAt(ZonedDateTime.now());
        entity.setUpdatedAt(ZonedDateTime.now());
        entity = meetingRepository.save(entity);

        // always add the owner as moderator to the participants list
        if (dto.getParticipants() == null) dto.setParticipants(new ArrayList<>());
        if (MeetingType.STATIC.equals(dto.getType()))
        {
            dto.getParticipants().add(new MeetingParticipantRequestDTO(ORGANIZER, userId));
        }
        else {
            dto.getParticipants().add(new MeetingParticipantRequestDTO(MODERATOR, userId));
        }

        if (isNormalMeeting(dto) && isRecurrentMeeting(dto))
        {
            List<MeetingEntity> children = createRecurringMeetings(entity);
            for (MeetingEntity child : children)
            {
                createParticipants(child, dto.getParticipants());
            }
        }

        MeetingDTO created = DTOUtils.buildFromEntity(entity);
        List<MeetingParticipantEntity> participants = createParticipants(entity, dto.getParticipants());
        created.setParticipants(MeetingParticipantDTO.buildFromEntity(participants));
        handleStaticRoom(entity, participants);
        return created;
    }

    @Override
    @PreAuthorize("@permissionControlService.canEditMeeting(#meetingId)")
    @Transactional
    @SneakyThrows
    public MeetingDTO update(UUID meetingId, MeetingUpdateDTO newDTO)
    {
        MeetingEntity existing = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(Constants.NO_MEETING_FOUND_FOR_ID, meetingId)));

        MeetingUpdateDTO existingDTO = DTOUtils.buildUpdateDTOFromEntity(existing);

        if (existing.isRecurrentParent())
        {
            // if start / end date, or recurrence of parent has changed, delete children and recreate them
            if (endTimeHasChanged(newDTO, existing) ||
                    startTimeHasChanged(newDTO, existing) ||
                    recurrenceHasChanged(newDTO, existingDTO))
            {
                existing = updateDataAndSave(newDTO, existing);

                List<MeetingEntity> childrenToDelete = meetingRepository.findAllByParentId(meetingId);
                meetingRepository.deleteAll(childrenToDelete);

                List<MeetingEntity> children = createRecurringMeetings(existing);
                List<MeetingParticipantRequestDTO> participantRequests = new ArrayList<>();
                for (MeetingParticipantEntity p : existing.getParticipants())
                {
                    participantRequests.add(new MeetingParticipantRequestDTO(Role.fromValue(p.getRole()), p.getEmail()));
                }
                for (MeetingEntity child : children)
                {
                    createParticipants(child, participantRequests);
                }
            }
        }
        else if (existing.isRecurrentChild())
        {
            MeetingEntity finalExisting = existing;
            MeetingEntity parent = meetingRepository.findById(existing.getParentId()).orElseThrow(
                    () -> new EntityNotFoundException(
                            String.format(Constants.NO_PARENT_MEETING_FOUND_FOR_ID, finalExisting.getParentId())
                    ));

            // if child end time is not in the window of parent's time, reject the update
            if (newDTO.getEndTime().isAfter(parent.getEndTime()))
            {
                throw new IllegalArgumentException(Constants.ERR_CHANGE_END_TIME_IN_SERIES);
            }
            if (recurrenceHasChanged(existingDTO, newDTO))
            {
                throw new IllegalArgumentException(Constants.ERR_CHANGE_SINGLE_IN_SERIES);
            }
            existing = updateDataAndSave(newDTO, existing);
        }
        else
        {
            // single normal meeting or static room
            existing = updateDataAndSave(newDTO, existing);
            if (existing.isStaticRoom())
            {
                notificationRepository.deleteAllByMeetingId(existing.getId());
                notificationService.createParticipantAddedNotifications(existing, existing.getParticipants());
            }
        }
        return DTOUtils.buildFromEntity(existing);
    }

    @Override
    @Transactional
    @PreAuthorize("@permissionControlService.canEditMeeting(#meetingId)")
    public void delete(UUID meetingId)
    {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(Constants.NO_MEETING_FOUND_FOR_ID, meetingId)));

        if (meeting.isSingleMeeting() || meeting.isRecurrentParent())
        {
            meetingRepository.delete(meeting);
        }
        else
        {
            List<MeetingEntity> children = meetingRepository.findAllByParentIdAndExcludedIsFalse(meeting.getParentId());
            boolean lastChild = children.size() == 1;
            if (lastChild)
            {
                // delete the parent meeting
                MeetingEntity parent = meetingRepository.findById(meeting.getParentId()).orElseThrow(
                        () -> new EntityNotFoundException(
                                String.format(Constants.NO_PARENT_MEETING_FOUND_FOR_ID, meeting.getParentId())));
                meetingRepository.delete(parent);
            }
            else
            {
                // set child meeting as excluded and delete notifications
                meeting.setExcluded(true);
                meetingRepository.save(meeting);
            }
        }
    }

    @Override
    @PreAuthorize("@permissionControlService.canReadMeeting(#meetingId)")
    public MeetingDTO findById(UUID meetingId)
    {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(Constants.NO_MEETING_FOUND_FOR_ID, meetingId))
        );
        return DTOUtils.buildFromEntity(meeting);
    }

    @Override
    public MeetingBasicDTO findBasicById(UUID meetingId)
    {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(Constants.NO_MEETING_FOUND_FOR_ID, meetingId))
        );
        return DTOUtils.buildBasicDTOFromEntity(meeting);
    }

    @Override
    @PreAuthorize("@permissionControlService.userAuthenticated")
    public MeetingsPageDTO getMeetingsPage(String type, Integer offset, Integer pageSize, String order,
                                           ZonedDateTime startDateTime, ZonedDateTime endDateTime)
    {
        String userId = AuthenticatedUserService.getAuthenticatedUser();

        Page<MeetingEntity> page;

        switch (type.toUpperCase())
        {
            case "INSTANT":
            {
                PageRequest pageRequest = PageRequest.of(offset, pageSize).withSort(Sort.by(COLUMN_CREATED_AT).descending());
                page = meetingRepository.findAllInstantMeetings(userId, pageRequest);
                break;
            }
            case "STATIC":
            {
                PageRequest pageRequest = getPageRequestWithSorting(offset, pageSize, COLUMN_NAME, order);
                page = meetingRepository.findAllStaticRooms(userId, pageRequest);
                break;
            }
            default: // NORMAL MEETINGS
            {
                PageRequest pageRequest = getPageRequestWithSorting(offset, pageSize, COLUMN_START_TIME, order);
                page = meetingRepository.findAllNormalMeetings(startDateTime, endDateTime, userId, pageRequest);
            }
        }
        List<MeetingDTO> content = page.getContent().stream().map(DTOUtils::buildFromEntity).collect(Collectors.toList());
        return new MeetingsPageDTO(page.getTotalPages(), page.getTotalElements(), page.getSize(),
                                                     page.getNumber(), content.size(), content);
    }

    private PageRequest getPageRequestWithSorting(Integer offset, Integer pageSize, String sortingColumn, String order)
    {
        if ("DESC".equalsIgnoreCase(order))
        {
            return PageRequest.of(offset, pageSize).withSort(Sort.by(sortingColumn).descending());
        }
        else
        {
            return PageRequest.of(offset, pageSize).withSort(Sort.by(sortingColumn).ascending());
        }
    }

    @Override
    public MeetingBasicDTO findNextOfSeries(UUID parentId)
    {
        ZonedDateTime currentTime = ZonedDateTime.now();
        List<MeetingEntity> meetings = meetingRepository.findByParentIdAndExcludedFalseOrderByEndTimeAsc(parentId);
        if (!meetings.isEmpty())
        {
            MeetingBasicDTO next;
            for (MeetingEntity meeting : meetings)
            {
                next = DTOUtils.buildBasicDTOFromEntity(meeting);
                if (meeting.getEndTime().isAfter(currentTime))
                {
                    return next;
                }
            }
        }
        throw new EntityNotFoundException(String.format(Constants.NO_NEXT_MEETING_FOUND_FOR_PARENT_ID, parentId));
    }

    @Override
    public boolean isRegisteredUser(String email)
    {
        UserRepresentation user = keycloakClientService.searchByEmail(email);
        return user != null;
    }

    @Override
    public void updateStaticRoomsLastVisitDate(UUID meetingId)
    {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(Constants.NO_MEETING_FOUND_FOR_ID, meetingId))
        );
        meeting.setLastVisitDate(ZonedDateTime.now());
        meetingRepository.save(meeting);
    }

    protected List<MeetingEntity> createRecurringMeetings(MeetingEntity parent)
    {
        List<MeetingEntity> children = new ArrayList<>();
        ZonedDateTime startDateTime = parent.getStartTime().truncatedTo(ChronoUnit.MINUTES);

        if (CUSTOM.getValue().equals(parent.getFrequency()))
        {
            int todayWeekDay = ZonedDateTime.now().getDayOfWeek().getValue();
            int startWeekDay = startDateTime.getDayOfWeek().getValue();
            if (todayWeekDay != startWeekDay || startCustomWeekDayIsTodayAndNotSet(parent, startWeekDay, todayWeekDay))
            {
                startDateTime = calculateNextDayForCustomRecurrence(startDateTime, parent);
            }
        }

        while (startDateTime.isBefore(parent.getSeriesEndTime()))
        {
            MeetingEntity childMeeting = createChildMeeting(parent);
            ZonedDateTime endDateTime = getEndDateTime(parent, startDateTime);
            childMeeting.setStartTime(startDateTime);
            childMeeting.setEndTime(endDateTime);
            children.add(childMeeting);

            switch (parent.getFrequency())
            {
                case "DAILY" -> startDateTime = startDateTime.plusDays(1);
                case "WEEKLY" -> startDateTime = startDateTime.plusWeeks(1);
                case "MONTHLY" -> startDateTime = startDateTime.plusMonths(1);
                case "CUSTOM" -> startDateTime = calculateNextDayForCustomRecurrence(startDateTime, parent);
            }
        }
        children = meetingRepository.saveAll(children);
        return children;
    }

    protected boolean startCustomWeekDayIsTodayAndNotSet(MeetingEntity parent, int startWeekDay, int todayWeekDay)
    {
        if (todayWeekDay != startWeekDay) return false;

        switch (startWeekDay)
        {
            case 1 : return !parent.isCustomDays_monday();
            case 2 : return !parent.isCustomDays_tuesday();
            case 3 : return !parent.isCustomDays_wednesday();
            case 4 : return !parent.isCustomDays_thursday();
            case 5 : return !parent.isCustomDays_friday();
            case 6 : return !parent.isCustomDays_saturday();
            case 7 : return !parent.isCustomDays_sunday();
            default: return false;
        }
    }

    protected ZonedDateTime calculateNextDayForCustomRecurrence(ZonedDateTime currentDate, MeetingEntity meeting)
    {
        switch (currentDate.getDayOfWeek().getValue())
        {
            case 1 ->
            { // current date is a monday
                if (meeting.isCustomDays_tuesday())
                {
                    return currentDate.plusDays(1);
                }
                if (meeting.isCustomDays_wednesday())
                {
                    return currentDate.plusDays(2);
                }
                if (meeting.isCustomDays_thursday())
                {
                    return currentDate.plusDays(3);
                }
                if (meeting.isCustomDays_friday())
                {
                    return currentDate.plusDays(4);
                }
                if (meeting.isCustomDays_saturday())
                {
                    return currentDate.plusDays(5);
                }
                if (meeting.isCustomDays_sunday())
                {
                    return currentDate.plusDays(6);
                }
            }
            case 2 ->
            { // current date is a tuesday
                if (meeting.isCustomDays_wednesday())
                {
                    return currentDate.plusDays(1);
                }
                if (meeting.isCustomDays_thursday())
                {
                    return currentDate.plusDays(2);
                }
                if (meeting.isCustomDays_friday())
                {
                    return currentDate.plusDays(3);
                }
                if (meeting.isCustomDays_saturday())
                {
                    return currentDate.plusDays(4);
                }
                if (meeting.isCustomDays_sunday())
                {
                    return currentDate.plusDays(5);
                }
                if (meeting.isCustomDays_monday())
                {
                    return currentDate.plusDays(6);
                }
            }
            case 3 ->
            { // current date is a wednesday
                if (meeting.isCustomDays_thursday())
                {
                    return currentDate.plusDays(1);
                }
                if (meeting.isCustomDays_friday())
                {
                    return currentDate.plusDays(2);
                }
                if (meeting.isCustomDays_saturday())
                {
                    return currentDate.plusDays(3);
                }
                if (meeting.isCustomDays_sunday())
                {
                    return currentDate.plusDays(4);
                }
                if (meeting.isCustomDays_monday())
                {
                    return currentDate.plusDays(5);
                }
                if (meeting.isCustomDays_tuesday())
                {
                    return currentDate.plusDays(6);
                }
            }
            case 4 ->
            { // current date is a thursday
                if (meeting.isCustomDays_friday())
                {
                    return currentDate.plusDays(1);
                }
                if (meeting.isCustomDays_saturday())
                {
                    return currentDate.plusDays(2);
                }
                if (meeting.isCustomDays_sunday())
                {
                    return currentDate.plusDays(3);
                }
                if (meeting.isCustomDays_monday())
                {
                    return currentDate.plusDays(4);
                }
                if (meeting.isCustomDays_tuesday())
                {
                    return currentDate.plusDays(5);
                }
                if (meeting.isCustomDays_wednesday())
                {
                    return currentDate.plusDays(6);
                }
            }
            case 5 ->
            { // current date is a friday
                if (meeting.isCustomDays_saturday())
                {
                    return currentDate.plusDays(1);
                }
                if (meeting.isCustomDays_sunday())
                {
                    return currentDate.plusDays(2);
                }
                if (meeting.isCustomDays_monday())
                {
                    return currentDate.plusDays(3);
                }
                if (meeting.isCustomDays_tuesday())
                {
                    return currentDate.plusDays(4);
                }
                if (meeting.isCustomDays_wednesday())
                {
                    return currentDate.plusDays(5);
                }
                if (meeting.isCustomDays_thursday())
                {
                    return currentDate.plusDays(6);
                }
            }
            case 6 ->
            { // current date is a saturday
                if (meeting.isCustomDays_sunday())
                {
                    return currentDate.plusDays(1);
                }
                if (meeting.isCustomDays_monday())
                {
                    return currentDate.plusDays(2);
                }
                if (meeting.isCustomDays_tuesday())
                {
                    return currentDate.plusDays(3);
                }
                if (meeting.isCustomDays_wednesday())
                {
                    return currentDate.plusDays(4);
                }
                if (meeting.isCustomDays_thursday())
                {
                    return currentDate.plusDays(5);
                }
                if (meeting.isCustomDays_friday())
                {
                    return currentDate.plusDays(6);
                }
            }
            case 7 ->
            { // current date is a sunday
                if (meeting.isCustomDays_monday())
                {
                    return currentDate.plusDays(1);
                }
                if (meeting.isCustomDays_tuesday())
                {
                    return currentDate.plusDays(2);
                }
                if (meeting.isCustomDays_wednesday())
                {
                    return currentDate.plusDays(3);
                }
                if (meeting.isCustomDays_thursday())
                {
                    return currentDate.plusDays(4);
                }
                if (meeting.isCustomDays_friday())
                {
                    return currentDate.plusDays(5);
                }
                if (meeting.isCustomDays_saturday())
                {
                    return currentDate.plusDays(6);
                }
            }
        }
        return currentDate.plusDays(1);
    }

    private MeetingEntity updateDataAndSave(MeetingUpdateDTO newDTO, MeetingEntity existing)
    {
        existing.setStartTime(newDTO.getStartTime());
        existing.setEndTime(newDTO.getEndTime());
        existing.setPassword(newDTO.getPassword());
        existing.setInfo(newDTO.getInfo());
        existing.setName(newDTO.getName());
        existing.setLobbyEnabled(newDTO.isLobbyEnabled());
        existing.setUpdatedAt(ZonedDateTime.now());
        existing.setStartedAt(newDTO.getStartedAt());

        // update recurrence data only for parent meeting, for children, we need to recreate them
        if (existing.isRecurrentParent())
        {
            existing.setSeriesEndTime(newDTO.getRecurrence().getEndDate());
            if (DTOUtils.hasCustomWeekDays(newDTO))
            {
                existing.setFrequency(CUSTOM.getValue());
            }
            else
            {
                existing.setFrequency(newDTO.getRecurrence().getFrequency().getValue());
            }
            if (newDTO.getRecurrence().getWeekDays() != null)
            {
                existing.setCustomDays_monday(newDTO.getRecurrence().getWeekDays().isMonday());
                existing.setCustomDays_tuesday(newDTO.getRecurrence().getWeekDays().isTuesday());
                existing.setCustomDays_wednesday(newDTO.getRecurrence().getWeekDays().isWednesday());
                existing.setCustomDays_thursday(newDTO.getRecurrence().getWeekDays().isThursday());
                existing.setCustomDays_friday(newDTO.getRecurrence().getWeekDays().isFriday());
                existing.setCustomDays_saturday(newDTO.getRecurrence().getWeekDays().isSaturday());
                existing.setCustomDays_sunday(newDTO.getRecurrence().getWeekDays().isSunday());
            }
        }
        existing = meetingRepository.save(existing);
        return existing;
    }

    private boolean recurrenceHasChanged(MeetingUpdateDTO newDTO, MeetingUpdateDTO existingDTO)
    {
        return !existingDTO.getRecurrence().equals(newDTO.getRecurrence());
    }

    private boolean startTimeHasChanged(MeetingUpdateDTO newDTO, MeetingEntity existing)
    {
        return !newDTO.getStartTime().isEqual(existing.getStartTime());
    }

    private boolean endTimeHasChanged(MeetingUpdateDTO newDTO, MeetingEntity existing)
    {
        return !newDTO.getEndTime().isEqual(existing.getEndTime());
    }

    private MeetingEntity createChildMeeting(MeetingEntity parent)
    {
        MeetingEntity newMeeting = new MeetingEntity();
        newMeeting.setParentId(parent.getId());
        newMeeting.setOwnerId(parent.getOwnerId());
        newMeeting.setName(parent.getName());
        newMeeting.setInfo(parent.getInfo());
        newMeeting.setPassword(parent.getPassword());
        newMeeting.setLobbyEnabled(parent.isLobbyEnabled());
        newMeeting.setFrequency(parent.getFrequency());
        newMeeting.setPhoneNumber(parent.getPhoneNumber());
        newMeeting.setConferencePin(parent.getConferencePin());
        newMeeting.setSipJibriLink(sipJibriLink);
        newMeeting.setSeriesEndTime(parent.getSeriesEndTime());
        newMeeting.setCreatedAt(ZonedDateTime.now());
        newMeeting.setUpdatedAt(ZonedDateTime.now());
        newMeeting.setCustomDays_monday(parent.isCustomDays_monday());
        newMeeting.setCustomDays_tuesday(parent.isCustomDays_tuesday());
        newMeeting.setCustomDays_wednesday(parent.isCustomDays_wednesday());
        newMeeting.setCustomDays_thursday(parent.isCustomDays_thursday());
        newMeeting.setCustomDays_friday(parent.isCustomDays_friday());
        newMeeting.setCustomDays_saturday(parent.isCustomDays_saturday());
        newMeeting.setCustomDays_sunday(parent.isCustomDays_sunday());
        return newMeeting;
    }

    private List<MeetingParticipantEntity> createParticipants(MeetingEntity meeting,
                                                           List<MeetingParticipantRequestDTO> participants)
    {
        Map<String, MeetingParticipantEntity> toCreate = new HashMap<>();
        for (MeetingParticipantRequestDTO p : participants)
        {
            MeetingParticipantEntity entity = new MeetingParticipantEntity();
            entity.setMeetingId(meeting.getId());
            entity.setEmail(p.getEmail());
            entity.setRole(p.getRole().getValue());
            entity.setCreatedAt(ZonedDateTime.now());
            entity.setUpdatedAt(ZonedDateTime.now());
            toCreate.put(entity.getEmail(), entity);
        }
        return participantRepository.saveAll(toCreate.values());
    }

    protected void handleStaticRoom(MeetingEntity meeting, List<MeetingParticipantEntity> participants)
    {
        if (meeting.isStaticRoom())
        {
            notificationService.createParticipantAddedNotifications(meeting, participants);

            boolean hasOrganizer = false;
            for (MeetingParticipantEntity participant : participants)
            {
                if (ORGANIZER.getValue().equals(participant.getRole()))
                {
                    hasOrganizer = true;
                    break;
                }
            }
            meeting.setHasOrganizer(hasOrganizer);
            meetingRepository.save(meeting);
        }
    }

    private String getUniqueConferencePin() {
        // don't want to make an infinite loop
        for (int i = 0; i < 10; i++) {
            String pin = getNextConferencePin();
            if (!meetingRepository.existsByConferencePin(pin)) {
                return pin;
            }
        }
        throw new RuntimeException("can't generate a unique conference pin");
    }

    private static String getNextConferencePin()
    {
        return String.valueOf(new SecureRandom().nextLong(1000000000L, 9999999999L));
    }

    private ZonedDateTime getEndDateTime(MeetingEntity meeting, ZonedDateTime startDateTime)
    {
        int hoursOffset = meeting.getEndTime().getHour() - meeting.getStartTime().getHour();
        if (hoursOffset < 0)
        {
            // fix offset for over midnight meeting
            hoursOffset += 24;
        }
        int minutesOffset = meeting.getEndTime().getMinute() - meeting.getStartTime().getMinute();
        int secondsOffset = meeting.getEndTime().getSecond() - meeting.getStartTime().getSecond();

        return startDateTime
                .plusHours(hoursOffset)
                .plusMinutes(minutesOffset)
                .plusSeconds(secondsOffset);
    }

}