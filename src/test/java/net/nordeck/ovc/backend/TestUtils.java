package net.nordeck.ovc.backend;

import net.nordeck.ovc.backend.dto.*;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import net.nordeck.ovc.backend.entity.NotificationEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static net.nordeck.ovc.backend.service.NotificationService.DELETE_CANDIDATE;


public class TestUtils
{
    public static final String OWNER_EMAIL = "walter.white@gmail.com";
    public static final String MODERATOR_EMAIL = "donald.duck@gmail.com";
    public static final String GUEST_EMAIL2 = "mickey.mouse@gmail.com";

    public static final UUID meetingId = UUID.randomUUID();
    public static final UUID participantId = UUID.randomUUID();
    public static final ZonedDateTime startTime = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(2);
    public static final ZonedDateTime endTime = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(3);
    public static final ZonedDateTime createdAt = ZonedDateTime.now();

    public static final ZonedDateTime START_TIME = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(2);
    public static final ZonedDateTime END_TIME = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(3);
    public static final ZonedDateTime CREATED_AT = ZonedDateTime.now();
    public static final String MEETING_NAME = "Meeting Name";
    public static final String MEETING_INFO = "anyInfo";
    public static final String MEETING_PASSWORD = "anyPassword";
    public static final String JIBRI_LINK = "Jibri Link";
    public static final String PHONE_NUMBER = "Phone Number";
    public static final String CONFERENCE_PIN = "Conference PIN";

    public static MeetingEntity getMeetingEntity()
    {
        MeetingEntity meeting = MeetingEntity.builder()
                .id(meetingId)
                .ownerId(OWNER_EMAIL)
                .name("Meeting Name")
                .info("anyInfo")
                .frequency("ONCE")
                .excluded(false)
                .password("anyPassword")
                .lobbyEnabled(true)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .startTime(startTime)
                .endTime(endTime)
                .sipJibriLink(JIBRI_LINK)
                .phoneNumber(PHONE_NUMBER)
                .conferencePin(CONFERENCE_PIN)
                .build();

        MeetingParticipantEntity p1 = new MeetingParticipantEntity(UUID.randomUUID(), meeting.getId(), OWNER_EMAIL,
                                                                   "MODERATOR", OWNER_EMAIL, createdAt, createdAt);
        MeetingParticipantEntity p2 = new MeetingParticipantEntity(UUID.randomUUID(), meeting.getId(),
                                                                   MODERATOR_EMAIL, "GUEST", MODERATOR_EMAIL,
                                                                   createdAt, createdAt);
        MeetingParticipantEntity p3 = new MeetingParticipantEntity(UUID.randomUUID(), meeting.getId(), GUEST_EMAIL2,
                                                                   "GUEST", GUEST_EMAIL2, createdAt, createdAt);

        meeting.setParticipants(List.of(p1, p2, p3));
        return meeting;
    }

    public static MeetingEntity getStaticRoom()
    {
        MeetingEntity meeting = MeetingEntity.builder()
                .id(meetingId)
                .name("Static Room")
                .ownerId(OWNER_EMAIL)
                .info("info")
                .frequency("once")
                .password("anyPassword")
                .lobbyEnabled(true)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .staticRoom(true)
                .build();

        MeetingParticipantEntity p1 = new MeetingParticipantEntity(UUID.randomUUID(), meeting.getId(), OWNER_EMAIL,
                                                                   "ORGANIZER", OWNER_EMAIL, createdAt, createdAt);
        MeetingParticipantEntity p2 = new MeetingParticipantEntity(UUID.randomUUID(), meeting.getId(),
                                                                   MODERATOR_EMAIL, "MODERATOR", MODERATOR_EMAIL,
                                                                   createdAt, createdAt);
        meeting.setParticipants(List.of(p1, p2));
        return meeting;
    }

    public static MeetingDTO getMeetingDTO()
    {
        MeetingDTO dto = DTOUtils.buildFromEntity(getMeetingEntity());
        dto.setRecurrence(new RecurrenceDTO());
        dto.getRecurrence().setWeekDays(new WeekDays());
        dto.getRecurrence().setFrequency(RecurrenceFrequency.DAILY);
        return dto;
    }

    public static MeetingBasicDTO getMeetingBasicDTO()
    {
        MeetingDTO dto = DTOUtils.buildFromEntity(getMeetingEntity());
        dto.setRecurrence(new RecurrenceDTO());
        dto.getRecurrence().setFrequency(RecurrenceFrequency.DAILY);
        dto.getRecurrence().setEndDate(dto.getEndTime());
        MeetingBasicDTO meetingBasicDTO = new MeetingBasicDTO();
        meetingBasicDTO.setId(dto.getId());
        meetingBasicDTO.setName(dto.getName());
        meetingBasicDTO.setRecurrence(dto.getRecurrence());
        meetingBasicDTO.setType(dto.getType());
        meetingBasicDTO.setStartTime(dto.getStartTime());
        meetingBasicDTO.setEndTime(dto.getEndTime());
        return meetingBasicDTO;
    }

    public static MeetingUpdateDTO getMeetingUpdateDTO()
    {
        MeetingUpdateDTO dto = DTOUtils.buildUpdateDTOFromEntity(getMeetingEntity());
        return dto;
    }

    public static MeetingCreateDTO getMeetingCreateDTO(boolean recurrent, int recurrentDays)
    {
        MeetingCreateDTO dto = new MeetingCreateDTO();
        dto.setName("Meeting Create");
        dto.setInfo("Meeting Info");
        dto.setPassword("password");
        dto.setType(MeetingType.NORMAL);
        dto.setLobbyEnabled(false);
        dto.setStartTime(ZonedDateTime.now());
        dto.setEndTime(ZonedDateTime.now().plusHours(2));
        if (recurrent)
        {
            RecurrenceDTO recurrence = new RecurrenceDTO();
            recurrence.setEndDate(dto.getStartTime().plusDays(recurrentDays-1).plusHours(2));
            recurrence.setFrequency(RecurrenceFrequency.DAILY);
            WeekDays weekDays = new WeekDays(true, true, true, true, true, true, true);
            recurrence.setWeekDays(weekDays);
            dto.setRecurrence(recurrence);
            dto.setEndTime(recurrence.getEndDate());
        }
        return dto;
    }

    public static MeetingParticipantEntity getMeetingParticipantEntity()
    {
        return MeetingParticipantEntity.builder()
                .id(participantId)
                .userId("user_id")
                .meetingId(meetingId)
                .email("email")
                .role("MODERATOR")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    public static MeetingParticipantDTO getMeetingParticipantDTO()
    {
        return MeetingParticipantDTO.buildFromEntity(getMeetingParticipantEntity());
    }

    public static NotificationEntity getNotificationEntity()
    {
        NotificationEntity e = new NotificationEntity();
        e.setId(UUID.randomUUID());
        e.setMessage("TEST MESSAGE");
        e.setUserId(OWNER_EMAIL);
        e.setType(DELETE_CANDIDATE);
        e.setViewed(false);
        e.setCreatedAt(ZonedDateTime.now());
        return e;
    }

    public static NotificationDTO getNotificationDTO()
    {
        return NotificationDTO.buildFromEntity(getNotificationEntity());
    }

    public static Authentication initSecurityContext(String email, List<String> roles)
    {
        Map<String, Object> claims = new HashMap<>();
        if (email == null)
        {
            claims.put("email", OWNER_EMAIL);
        }
        else
        {
            claims.put("email", email);
        }

        Instant expiresAt = Instant.now().plusMillis(1000 * 60 * 60 * 60);
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("auth", "Bearer");
        Jwt token = new Jwt("123456", Instant.now(), expiresAt, headers, claims);

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null)
        {
            for(String role : roles)
            {
                authorities.add(new SimpleGrantedAuthority(role));
            }
        }
        JwtAuthenticationToken auth = new JwtAuthenticationToken(token, authorities, "auth-token");
        SecurityContextHolder.getContext().setAuthentication(auth);
        auth.setAuthenticated(true);
        return auth;
    }

}