package net.nordeck.ovc.backend.service;

import jakarta.persistence.EntityNotFoundException;
import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.dto.*;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import net.nordeck.ovc.backend.repository.MeetingParticipantRepository;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import net.nordeck.ovc.backend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.nordeck.ovc.backend.Constants.NO_MEETING_FOUND_FOR_ID;
import static net.nordeck.ovc.backend.dto.RecurrenceFrequency.DAILY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles(value = "test")
@SpringBootTest
public class MeetingServiceImplTest
{

    @Mock
    MeetingRepository meetingRepository;

    @Mock
    MeetingParticipantRepository participantRepository;

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    KeycloakClientService keycloakClientService;

    private Authentication auth = TestUtils.initSecurityContext(null, null);

    @InjectMocks
    MeetingServiceImpl meetingService;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp()
    {
        meetingRepository = Mockito.mock(MeetingRepository.class);
        notificationRepository = Mockito.mock(NotificationRepository.class);
        notificationService = Mockito.mock(NotificationServiceImpl.class);
        meetingService = new MeetingServiceImpl(
                meetingRepository,
                participantRepository,
                notificationRepository,
                notificationService,
                keycloakClientService);
        meetingService.sipPhoneNumber = "+49 40 3003 5005";
        meetingService.sipJibriLink = "112233@sip.nordeck.io";
    }

    @Test
    void getMeetings_normal_success()
    {
        ZonedDateTime startDateTime = ZonedDateTime.now();
        ZonedDateTime endDateTime = startDateTime.plusWeeks(4);
        MeetingEntity entity1 = TestUtils.getMeetingEntity();
        MeetingEntity entity2 = TestUtils.getMeetingEntity();
        Page<MeetingEntity> toReturn = new PageImpl<>(List.of(entity1, entity2));

        when(meetingRepository.findAllNormalMeetings(any(), any(), any(), any())).thenReturn(toReturn);

        MeetingsPageDTO page = meetingService.getMeetingsPage("normal", 0, 10, "asc", startDateTime, endDateTime);

        assertEquals(2, page.getPageItems());
    }

    @Test
    void getMeetings_static_success()
    {
        ZonedDateTime startDateTime = ZonedDateTime.now();
        ZonedDateTime endDateTime = startDateTime.plusWeeks(4);
        MeetingEntity entity1 = TestUtils.getMeetingEntity();
        MeetingEntity entity2 = TestUtils.getMeetingEntity();
        Page<MeetingEntity> toReturn = new PageImpl<>(List.of(entity1, entity2));

        when(meetingRepository.findAllStaticRooms(any(), any())).thenReturn(toReturn);

        MeetingsPageDTO page = meetingService.getMeetingsPage("static", 0, 10,
                                                              "asc", startDateTime, endDateTime);

        assertEquals(2, page.getTotalItems());
        assertEquals(1, page.getTotalPages());
    }

    @Test
    void getMeetings_instant_success()
    {
        ZonedDateTime startDateTime = ZonedDateTime.now();
        ZonedDateTime endDateTime = startDateTime.plusWeeks(4);
        MeetingEntity entity1 = TestUtils.getMeetingEntity();
        Page<MeetingEntity> toReturn = new PageImpl<>(List.of(entity1));

        when(meetingRepository.findAllInstantMeetings(any(), any())).thenReturn(toReturn);

        MeetingsPageDTO page = meetingService.getMeetingsPage("instant", 0, 10,
                                                              "asc", startDateTime, endDateTime);

        assertEquals(1, page.getTotalItems());
        assertEquals(1, page.getTotalPages());
    }

    @Test
    void given_single_create_success()
    {
        MeetingCreateDTO dto = TestUtils.getMeetingCreateDTO(false, 0);
        MeetingEntity entity = MeetingEntity.buildFromMeetingAbstractDTO(dto);

        MeetingParticipantEntity participantEntity = TestUtils.getMeetingParticipantEntity();
        when(meetingRepository.save(any())).thenReturn(entity);
        when(participantRepository.saveAll(any())).thenReturn(List.of(participantEntity));

        meetingService.create(dto);

        verify(meetingRepository, times(1)).save(any(MeetingEntity.class));
        verify(participantRepository, times(1)).saveAll(any());
    }

    @Test
    void given_recurrent_custom_create_success()
    {
        MeetingCreateDTO dto = TestUtils.getMeetingCreateDTO(true, 5);
        MeetingEntity entity = MeetingEntity.buildFromMeetingAbstractDTO(dto);
        MeetingEntity c1 = TestUtils.getMeetingEntity();
        MeetingEntity c2 = TestUtils.getMeetingEntity();
        MeetingEntity c3 = TestUtils.getMeetingEntity();
        MeetingEntity c4 = TestUtils.getMeetingEntity();
        MeetingEntity c5 = TestUtils.getMeetingEntity();
        MeetingParticipantEntity participantEntity = TestUtils.getMeetingParticipantEntity();
        when(meetingRepository.save(any())).thenReturn(entity);
        when(meetingRepository.saveAll(any())).thenReturn(List.of(c1, c2, c3, c4, c5));
        when(participantRepository.saveAll(any())).thenReturn(List.of(participantEntity));

        meetingService.create(dto);

        verify(meetingRepository, times(1)).saveAll(anyList());
        verify(participantRepository, times(6)).saveAll(any());
    }

    @Test
    void given_recurrent_daily_create_success()
    {
        MeetingCreateDTO dto = TestUtils.getMeetingCreateDTO(true, 3);
        dto.getRecurrence().setWeekDays(new WeekDays());
        dto.getRecurrence().setFrequency(DAILY);
        MeetingEntity entity = MeetingEntity.buildFromMeetingAbstractDTO(dto);
        MeetingEntity c1 = TestUtils.getMeetingEntity();
        MeetingEntity c2 = TestUtils.getMeetingEntity();
        MeetingEntity c3 = TestUtils.getMeetingEntity();
        MeetingParticipantEntity participantEntity = TestUtils.getMeetingParticipantEntity();
        when(meetingRepository.save(any())).thenReturn(entity);
        when(meetingRepository.saveAll(any())).thenReturn(List.of(c1, c2, c3));
        when(participantRepository.saveAll(any())).thenReturn(List.of(participantEntity));

        meetingService.create(dto);

        verify(meetingRepository, times(1)).saveAll(anyList());
        verify(participantRepository, times(4)).saveAll(any());
    }

    @Test
    void given_single_update_success()
    {
        MeetingUpdateDTO dto = TestUtils.getMeetingUpdateDTO();
        MeetingEntity existing = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(meetingRepository.save(existing)).thenReturn(existing);

        meetingService.update(existing.getId(), dto);

        verify(meetingRepository, times(1)).findById(existing.getId());
        verify(meetingRepository, times(1)).save(existing);
    }

    @Test
    void given_single_update_throwsEntityNotFoundException()
    {
        MeetingUpdateDTO dto = TestUtils.getMeetingUpdateDTO();
        MeetingEntity existing = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(existing.getId())).thenReturn(Optional.empty());

        Exception ex = assertThrows(EntityNotFoundException.class,
                                    () -> meetingService.update(existing.getId(), dto));

        assertTrue(ex.getMessage().contains("No meeting found for id"));
        verify(meetingRepository, times(1)).findById(existing.getId());
    }

    @Test
    void given_recurrent_parent_update_success()
    {
        MeetingEntity parent = TestUtils.getMeetingEntity();
        MeetingEntity child = TestUtils.getMeetingEntity();
        child.setParentId(parent.getId());
        child.setFrequency("DAILY");
        parent.setFrequency("DAILY");
        parent.setSeriesEndTime(parent.getEndTime());
        child.setSeriesEndTime(parent.getSeriesEndTime());
        MeetingUpdateDTO dto = TestUtils.getMeetingUpdateDTO();
        dto.setRecurrence(new RecurrenceDTO());
        dto.getRecurrence().setEndDate(ZonedDateTime.now().plusDays(1));
        dto.getRecurrence().setFrequency(DAILY);
        when(meetingRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(meetingRepository.save(parent)).thenReturn(parent);
        when(meetingRepository.save(any())).thenReturn(child);

        meetingService.update(parent.getId(), dto);

        verify(meetingRepository, times(1)).findById(parent.getId());
        verify(meetingRepository, times(1)).save(parent);
    }

    @Test
    void given_recurrent_child_update_success()
    {
        UUID parentId = UUID.randomUUID();
        MeetingEntity existing = TestUtils.getMeetingEntity();
        existing.setFrequency("DAILY");
        existing.setParentId(parentId);
        existing.setSeriesEndTime(existing.getEndTime());
        MeetingUpdateDTO dto = TestUtils.getMeetingUpdateDTO();
        RecurrenceDTO recurrence = new RecurrenceDTO();
        recurrence.setFrequency(DAILY);
        recurrence.setEndDate(existing.getEndTime());
        dto.setRecurrence(recurrence);

        when(meetingRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(meetingRepository.findById(parentId)).thenReturn(Optional.of(existing));
        when(meetingRepository.save(existing)).thenReturn(existing);

        meetingService.update(existing.getId(), dto);

        verify(meetingRepository, times(1)).findById(existing.getId());
        verify(meetingRepository, times(1)).findById(parentId);
        verify(meetingRepository, times(1)).save(existing);
    }

    @Test
    void given_recurrent_child_update_throwsEntityNotFoundException()
    {
        UUID parentId = UUID.randomUUID();
        MeetingEntity existing = TestUtils.getMeetingEntity();
        existing.setFrequency("DAILY");
        existing.setParentId(parentId);
        existing.setSeriesEndTime(existing.getEndTime());
        MeetingUpdateDTO dto = TestUtils.getMeetingUpdateDTO();
        RecurrenceDTO recurrence = new RecurrenceDTO();
        recurrence.setFrequency(DAILY);
        recurrence.setWeekDays(new WeekDays());
        recurrence.setEndDate(existing.getEndTime());
        dto.setRecurrence(recurrence);

        when(meetingRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(meetingRepository.findById(parentId)).thenReturn(Optional.empty());
        when(meetingRepository.save(existing)).thenReturn(existing);

        Exception ex = assertThrows(EntityNotFoundException.class,
                                    () -> meetingService.update(existing.getId(), dto));
        assertTrue(ex.getMessage().contains("No parent meeting found for id"));

        verify(meetingRepository, times(1)).findById(existing.getId());
        verify(meetingRepository, times(1)).findById(parentId);
    }

    @Test
    void given_recurrent_child_invalidEndDate_update_throwsIllegalArgumentException()
    {
        UUID parentId = UUID.randomUUID();
        MeetingEntity existing = TestUtils.getMeetingEntity();
        existing.setFrequency("DAILY");
        existing.setParentId(parentId);
        existing.setSeriesEndTime(existing.getEndTime());
        MeetingUpdateDTO dto = TestUtils.getMeetingUpdateDTO();
        RecurrenceDTO recurrence = new RecurrenceDTO(
                DAILY, existing.getEndTime(), new WeekDays());
        dto.setEndTime(existing.getEndTime().plusHours(1));
        dto.setRecurrence(recurrence);

        when(meetingRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(meetingRepository.findById(parentId)).thenReturn(Optional.of(existing));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> meetingService.update(existing.getId(), dto));
        assertTrue(ex.getMessage().contains("The end time of a single meeting"));
    }

    @Test
    void given_recurrent_child_changeRecurrence_update_throwsIllegalArgumentException()
    {
        UUID parentId = UUID.randomUUID();
        MeetingEntity existing = TestUtils.getMeetingEntity();
        existing.setFrequency("DAILY");
        existing.setParentId(parentId);
        existing.setSeriesEndTime(existing.getEndTime());
        MeetingUpdateDTO dto = TestUtils.getMeetingUpdateDTO();
        RecurrenceDTO recurrence = new RecurrenceDTO(
                RecurrenceFrequency.WEEKLY, existing.getEndTime(), new WeekDays());
        dto.setRecurrence(recurrence);

        when(meetingRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(meetingRepository.findById(parentId)).thenReturn(Optional.of(existing));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> meetingService.update(existing.getId(), dto));
        assertTrue(ex.getMessage().contains("The recurrence of a single meeting must be changed"));
    }

    @Test
    void given_recurrent_parent_changeStartDate_update_delete_children()
    {
        MeetingEntity parent = TestUtils.getMeetingEntity();
        parent.setFrequency("DAILY");
        parent.setSeriesEndTime(parent.getEndTime());

        MeetingEntity child = TestUtils.getMeetingEntity();
        child.setParentId(parent.getId());
        child.setSeriesEndTime(parent.getSeriesEndTime().plusHours(1));
        child.setSeriesEndTime(parent.getSeriesEndTime());
        child.setFrequency("DAILY");

        MeetingUpdateDTO updateDTO = TestUtils.getMeetingUpdateDTO();
        updateDTO.setStartTime(parent.getStartTime().plusMinutes(15));
        updateDTO.setRecurrence(
                new RecurrenceDTO(
                        DAILY,
                        parent.getEndTime(),
                        new WeekDays()));
        when(meetingRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(meetingRepository.save(parent)).thenReturn(parent);
        when(meetingRepository.save(any())).thenReturn(child);

        meetingService.update(parent.getId(), updateDTO);

        verify(meetingRepository, times(1)).findById(parent.getId());
        verify(meetingRepository, times(1)).save(parent);
        verify(meetingRepository, times(1)).findAllByParentId(parent.getId());
        verify(meetingRepository, times(1)).deleteAll(any());
    }

    @Test
    void given_single_delete_success()
    {
        MeetingEntity existing = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        meetingService.delete(existing.getId());

        verify(meetingRepository, times(1)).findById(existing.getId());
        verify(meetingRepository, times(1)).delete(existing);
    }

    @Test
    void given_single_delete_throwsEntityNotFoundException()
    {
        MeetingEntity existing = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(existing.getId())).thenReturn(Optional.empty());

        Exception ex = assertThrows(EntityNotFoundException.class,
                                    () -> meetingService.delete(existing.getId()));
        assertTrue(ex.getMessage().contains("No meeting found for id "));

        verify(meetingRepository, times(1)).findById(existing.getId());
        verify(meetingRepository, times(0)).delete(existing);
    }

    @Test
    void given_recurrent_parent_delete_success()
    {
        MeetingEntity parent = TestUtils.getMeetingEntity();
        parent.setFrequency("DAILY");

        MeetingEntity child1 = TestUtils.getMeetingEntity();
        MeetingEntity child2 = TestUtils.getMeetingEntity();
        child1.setParentId(parent.getId());
        child2.setParentId(parent.getId());

        when(meetingRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(meetingRepository.findAllByParentId(parent.getId())).thenReturn(List.of(child1, child2));

        meetingService.delete(parent.getId());

        verify(meetingRepository, times(1)).delete(parent);
    }

    @Test
    void given_recurrent_last_child_delete_success()
    {
        MeetingEntity parent = TestUtils.getMeetingEntity();
        parent.setFrequency("DAILY");
        MeetingEntity child1 = TestUtils.getMeetingEntity();
        child1.setId(UUID.randomUUID());
        child1.setParentId(parent.getId());

        when(meetingRepository.findAllByParentIdAndExcludedIsFalse(parent.getId())).thenReturn(List.of(child1));
        when(meetingRepository.findById(child1.getId())).thenReturn(Optional.of(child1));
        when(meetingRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        meetingService.delete(child1.getId());

        verify(meetingRepository, times(1)).findById(child1.getId());
        verify(meetingRepository, times(1)).findById(parent.getId());
        verify(meetingRepository, times(1)).delete(parent);
        verify(meetingRepository, times(1)).findAllByParentIdAndExcludedIsFalse(parent.getId());
    }

    @Test
    void given_recurrent_not_last_child_delete_success()
    {
        MeetingEntity parent = TestUtils.getMeetingEntity();
        parent.setFrequency("DAILY");
        MeetingEntity child1 = TestUtils.getMeetingEntity();
        MeetingEntity child2 = TestUtils.getMeetingEntity();
        child1.setId(UUID.randomUUID());
        child2.setId(UUID.randomUUID());
        child1.setParentId(parent.getId());
        child2.setParentId(parent.getId());

        when(meetingRepository.findAllByParentIdAndExcludedIsFalse(parent.getId())).thenReturn(List.of(child1, child2));
        when(meetingRepository.findById(child1.getId())).thenReturn(Optional.of(child1));
        when(meetingRepository.findById(parent.getId())).thenReturn(Optional.of(parent));

        meetingService.delete(child1.getId());

        verify(meetingRepository, times(1)).findById(child1.getId());
        verify(meetingRepository, times(1)).findAllByParentIdAndExcludedIsFalse(parent.getId());
        verify(meetingRepository, times(1)).save(child1);
    }

    @Test
    void given_recurrent_last_child_delete_throwsEntityNotFoundException()
    {
        MeetingEntity parent = TestUtils.getMeetingEntity();
        parent.setFrequency("DAILY");
        MeetingEntity child1 = TestUtils.getMeetingEntity();
        child1.setId(UUID.randomUUID());
        child1.setParentId(parent.getId());

        when(meetingRepository.findAllByParentIdAndExcludedIsFalse(parent.getId())).thenReturn(List.of(child1));
        when(meetingRepository.findById(child1.getId())).thenReturn(Optional.of(child1));
        when(meetingRepository.findById(parent.getId())).thenReturn(Optional.empty());

        Exception ex = assertThrows(EntityNotFoundException.class,
                                    () -> meetingService.delete(child1.getId()));
        assertTrue(ex.getMessage().contains("No parent meeting found for id"));



        verify(meetingRepository, times(1)).findById(child1.getId());
        verify(meetingRepository, times(1)).findById(parent.getId());
        verify(meetingRepository, times(1)).findAllByParentIdAndExcludedIsFalse(parent.getId());
    }

    @Test
    void findById_success()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(meeting.getId())).thenReturn(Optional.of(meeting));

        MeetingDTO dto = meetingService.findById(meeting.getId());
        assertEquals(meeting.getId(), dto.getId());
        assertEquals(meeting.getName(), dto.getName());
    }

    @Test
    void findBasicById_success()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(meeting.getId())).thenReturn(Optional.of(meeting));

        MeetingBasicDTO dto = meetingService.findBasicById(meeting.getId());
        assertEquals(meeting.getId(), dto.getId());
        assertEquals(meeting.getName(), dto.getName());
    }

    @Test
    void handleStaticRoom()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        meeting.setStaticRoom(true);
        MeetingParticipantEntity participant = meeting.getParticipants().get(0);
        participant.setRole(Role.ORGANIZER.getValue());
        when(meetingRepository.findById(meeting.getId())).thenReturn(Optional.of(meeting));

        meetingService.handleStaticRoom(meeting, List.of(participant));
    }

    @Test
    void updateStaticRoomsLastVisitDate_success()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(meeting.getId())).thenReturn(Optional.of(meeting));

        meetingService.updateStaticRoomsLastVisitDate(meeting.getId());

        verify(meetingRepository, times(1)).findById(meeting.getId());
        verify(meetingRepository, times(1)).save(meeting);
    }

    @Test
    void updateStaticRoomsLastVisitDate_throwsEntityNotFoundException()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(meeting.getId())).thenReturn(Optional.empty());

        Exception ex = assertThrows(EntityNotFoundException.class,
                                    () -> meetingService.updateStaticRoomsLastVisitDate(meeting.getId()));

        assertTrue(ex.getMessage().contains(String.format(NO_MEETING_FOUND_FOR_ID, meeting.getId())));
    }

    @Test
    void findById_throwsEntityNotFoundException()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(meeting.getId())).thenReturn(Optional.empty());

        Exception ex = assertThrows(EntityNotFoundException.class,
                                    () -> meetingService.findById(meeting.getId()));
        assertTrue(ex.getMessage().contains("No meeting found for id"));
    }

    @Test
    void calculateNextDayForCustomRecurrence_success()
    {
        ZonedDateTime date = ZonedDateTime.now();
        MeetingEntity meeting = TestUtils.getMeetingEntity();

        for (int x = 1; x < 8; x++)
        {
            meeting.setCustomDays_monday(x == 1);
            meeting.setCustomDays_tuesday(x == 2);
            meeting.setCustomDays_wednesday(x == 3);
            meeting.setCustomDays_thursday(x == 4);
            meeting.setCustomDays_friday(x == 5);
            meeting.setCustomDays_saturday(x == 6);
            meeting.setCustomDays_sunday(x == 7);
            for (int y = 1; y < 8; y++)
            {
                meetingService.calculateNextDayForCustomRecurrence(date, meeting);
                date = date.plusDays(1);
            }
            meeting.setCustomDays_monday(false);
            meeting.setCustomDays_tuesday(false);
            meeting.setCustomDays_wednesday(false);
            meeting.setCustomDays_thursday(false);
            meeting.setCustomDays_friday(false);
            meeting.setCustomDays_saturday(false);
            meeting.setCustomDays_sunday(false);
        }
    }

    @Test
    void givenThreeMeetings_findNextOfSeries_isSuccessful() {
        MeetingEntity meetingEntity1 = TestUtils.getMeetingEntity();
        meetingEntity1.setEndTime(ZonedDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES));

        List<MeetingEntity> meetings = List.of(meetingEntity1);
        when(meetingRepository.findByParentIdAndExcludedFalseOrderByEndTimeAsc(any())).thenReturn(meetings);

        MeetingBasicDTO next = meetingService.findNextOfSeries(TestUtils.meetingId);
        assertEquals(meetingEntity1.getEndTime(), next.getEndTime());
    }

    @Test
    void givenThreeMeetings_findNextOfSeries_throwsEntityNotFoundException()
    {
        MeetingEntity meetingEntity1 = TestUtils.getMeetingEntity();
        meetingEntity1.setEndTime(ZonedDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES));

        when(meetingRepository.findByParentIdAndExcludedFalseOrderByEndTimeAsc(any())).thenReturn(List.of());

        Exception ex = assertThrows(EntityNotFoundException.class,
                                    () -> meetingService.findNextOfSeries(TestUtils.meetingId));
        assertTrue(ex.getMessage().contains("No next meeting found for parent id"));
    }

    @Test
    void isRegisteredUser_success()
    {
        when(keycloakClientService.searchByEmail(anyString())).thenReturn(new UserRepresentation());

        assertTrue(meetingService.isRegisteredUser("email"));
    }


    @Test
    void getUniqueConferencePin_success()
    {
        MeetingCreateDTO dto = TestUtils.getMeetingCreateDTO(false, 0);

        MeetingParticipantEntity participantEntity = TestUtils.getMeetingParticipantEntity();
        when(meetingRepository.save(any())).thenAnswer(invocation -> {
            MeetingEntity e = invocation.getArgument(0, MeetingEntity.class);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(participantRepository.saveAll(any())).thenReturn(List.of(participantEntity));

        // this should help with testing the random generator
        final ArrayList<String> mockDB = new ArrayList<>();
        when(meetingRepository.existsByConferencePin(anyString())).thenAnswer(invocation -> {
            String pin = invocation.getArgument(0, String.class);
            return mockDB.stream()
                    .filter(s -> s.equals(pin))
                    .findAny()
                    .map(o -> true)
                    .orElseGet(() -> {
                        mockDB.add(pin);
                        return false;
                    });
        });

        // meetingService.create will always generate a new Pin

        String conferencePin = meetingService.create(dto).getConferencePin();
        assertNotNull(conferencePin);

        String conferencePin2 = meetingService.create(dto).getConferencePin();
        assertNotNull(conferencePin2);

        // 2 generated pins should be different
        assertNotEquals(conferencePin, conferencePin2);

        verify(meetingRepository, times(2)).existsByConferencePin(anyString());
    }

    @Test
    void getUniqueConferencePin_rngAccident() {
        MeetingCreateDTO dto = TestUtils.getMeetingCreateDTO(false, 0);

        MeetingParticipantEntity participantEntity = TestUtils.getMeetingParticipantEntity();
        when(meetingRepository.save(any())).thenAnswer(invocation -> {
            MeetingEntity e = invocation.getArgument(0, MeetingEntity.class);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(participantRepository.saveAll(any())).thenReturn(List.of(participantEntity));

        // triggers some DB problem or random generator failure
        when(meetingRepository.existsByConferencePin(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> meetingService.create(dto));

        // it tried several times to generate a pin and eventually failed (didn't loop forever)
        verify(meetingRepository, atLeast(5)).existsByConferencePin(anyString());
    }

    @Test
    void startCustomWeekDayIsTodayAndNotSet()
    {
        MeetingEntity parent = TestUtils.getMeetingEntity();
        for (int i = 1; i < 8; i++)
        {
            assertTrue(meetingService.startCustomWeekDayIsTodayAndNotSet(parent, i, i));
        }
        assertFalse(meetingService.startCustomWeekDayIsTodayAndNotSet(parent, 8, 8));
    }
}