package net.nordeck.ovc.backend.service;

import jakarta.persistence.EntityNotFoundException;
import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.dto.MeetingPermissionsDTO;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.NotificationEntity;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import net.nordeck.ovc.backend.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.nordeck.ovc.backend.TestUtils.OWNER_EMAIL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles(value = "test")
@SpringBootTest
public class PermissionControlServiceTest
{

    @Mock
    private Authentication auth;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @InjectMocks
    private PermissionControlServiceImpl permissionService;

    @BeforeEach
    void beforeEach()
    {
        initSecurityContext(null);
    }

    @AfterEach
    void afterEach()
    {
        SecurityContextHolder.clearContext();
    }

    private void initSecurityContext(String email)
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
        when(auth.getPrincipal()).thenReturn(token);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void canEditUsualMeeting_ReturnsTrue()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(any())).thenReturn(Optional.of(meeting));
        assertTrue(permissionService.canEditMeeting(meeting.getId()));
    }

    @Test
    void canEditUsualMeeting_ReturnsFalse()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        meeting.setOwnerId("nononono");
        when(meetingRepository.findById(any())).thenReturn(Optional.of(meeting));
        assertFalse(permissionService.canEditMeeting(meeting.getId()));
    }

    @Test
    void canEditStaticRoom_ReturnsTrue1()
    {
        MeetingEntity room = TestUtils.getStaticRoom();
        when(meetingRepository.findById(any())).thenReturn(Optional.of(room));
        assertTrue(permissionService.canEditMeeting(room.getId()));
    }

    @Test
    void canEditStaticRoom_ReturnsFalse1()
    {
        MeetingEntity staticRoom = TestUtils.getStaticRoom();
        staticRoom.setOwnerId("nononono0");
        staticRoom.getParticipants().get(0).setEmail("nononono1");
        staticRoom.getParticipants().get(1).setEmail("nononono2");

        when(meetingRepository.findById(any())).thenReturn(Optional.of(staticRoom));
        assertFalse(permissionService.canEditMeeting(staticRoom.getId()));
    }

    @Test
    void canEditStaticRoom_ReturnsFalse2()
    {
        MeetingEntity staticRoom = TestUtils.getStaticRoom();
        staticRoom.setOwnerId("nononono0");
        staticRoom.getParticipants().get(0).setRole("nononono1");
        staticRoom.getParticipants().get(1).setRole("nononono2");
        when(meetingRepository.findById(any())).thenReturn(Optional.of(staticRoom));
        assertFalse(permissionService.canEditMeeting(staticRoom.getId()));
    }

    @Test
    void canEditStaticRoom_ReturnsFalse3()
    {
        MeetingEntity room = TestUtils.getStaticRoom();
        room.setId(room.getId());
        room.setParticipants(null);
        when(meetingRepository.findById(any())).thenReturn(Optional.of(room));
        assertFalse(permissionService.canEditMeeting(room.getId()));
    }

    @Test
    void canEditNotification_ReturnsFalse()
    {
        NotificationEntity notification = TestUtils.getNotificationEntity();
        notification.setUserId("nonono");
        when(notificationRepository.findById(any())).thenReturn(Optional.of(notification));
        assertFalse(permissionService.canEditNotification(notification.getId()));
    }

    @Test
    void canEditNotification_ReturnsFalse2()
    {
        NotificationEntity notification = TestUtils.getNotificationEntity();
        when(notificationRepository.findById(any())).thenReturn(Optional.empty());
        assertFalse(permissionService.canEditNotification(notification.getId()));
    }

    @Test
    void canEditNotification_ReturnsTrueForExistingRecord()
    {
        NotificationEntity notification = TestUtils.getNotificationEntity();
        when(notificationRepository.findById(any())).thenReturn(Optional.of(notification));
        assertTrue(permissionService.canEditNotification(notification.getId()));
    }

    @Test
    void canEditNotification_ReturnsTrueForNewRecord()
    {
        NotificationEntity notification = TestUtils.getNotificationEntity();
        notification.setId(null);
        when(notificationRepository.findById(any())).thenReturn(Optional.of(notification));
        assertTrue(permissionService.canEditNotification(notification.getId()));
    }

    @Test
    void canReadNotification_ReturnsTrue()
    {
        NotificationEntity notification = TestUtils.getNotificationEntity();
        when(notificationRepository.findById(any())).thenReturn(Optional.empty());
        assertFalse(permissionService.canReadNotification(notification.getId()));
    }

    @Test
    void canReadNotification_ReturnsFalse()
    {
        NotificationEntity notification = TestUtils.getNotificationEntity();
        when(notificationRepository.findById(any())).thenReturn(Optional.of(notification));
        assertTrue(permissionService.canReadNotification(notification.getId()));
    }

    @Test
    void canReadUsualMeeting_ReturnsTrue()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(any())).thenReturn(Optional.of(meeting));
        assertTrue(permissionService.canReadMeeting(meeting.getId()));
    }

    @Test
    void canReadUsualMeeting_ReturnsFalse()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        meeting.getParticipants().get(0).setEmail("nononono");
        meeting.getParticipants().get(1).setEmail("nononono");
        when(meetingRepository.findById(any())).thenReturn(Optional.of(meeting));
        assertFalse(permissionService.canReadMeeting(meeting.getId()));
    }

    @Test
    void canReadUsualMeeting_ThrowsEntityNotFoundException()
    {
        when(meetingRepository.findById(any())).thenReturn(Optional.empty());

        Exception ex = assertThrows(EntityNotFoundException.class,
                                    () -> permissionService.canReadMeeting(UUID.randomUUID()));
        assertTrue(ex.getMessage().contains("No meeting found for id"));
    }

    @Test
    void canEditMeeting_newRecord_ReturnsTrue()
    {
        when(meetingRepository.findById(any())).thenReturn(Optional.empty());
        assertTrue(permissionService.canEditMeeting(null));
    }

    @Test
    void canCreateRecords_ReturnsTrue()
    {
        assertTrue(permissionService.canCreateRecords());
    }

    @Test
    void canCreateRecords_ReturnsFalse()
    {
        SecurityContextHolder.clearContext();
        assertFalse(permissionService.canCreateRecords());
    }

    @Test
    void getPermissions_success()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(any())).thenReturn(Optional.of(meeting));

        MeetingPermissionsDTO permissions = permissionService.getPermissions(meeting.getId());

        assertTrue(permissions.isUserCanRead());
        assertTrue(permissions.isUserCanEdit());
    }

}