package net.nordeck.ovc.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import net.nordeck.ovc.backend.dto.NotificationDTO;
import net.nordeck.ovc.backend.dto.NotificationsPageDTO;
import net.nordeck.ovc.backend.dto.Role;
import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import net.nordeck.ovc.backend.entity.NotificationEntity;
import net.nordeck.ovc.backend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles(value = "test")
public class NotificationServiceImplTest
{

    @Mock
    private NotificationRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Authentication auth;

    @InjectMocks
    private NotificationServiceImpl service;

    @BeforeEach
    void setup()
    {
        service.objectMapper = objectMapper;
    }

    @Test
    void findById_success()
    {
        NotificationEntity entity = TestUtils.getNotificationEntity();

        when(repository.findById(any())).thenReturn(Optional.of(entity));

        NotificationDTO dto = service.findById(entity.getId());

        assertEquals(entity.getType(), dto.getType());
    }

    @Test
    void findById_ThrowsEntityNotFoundException()
    {
        assertThrows(EntityNotFoundException.class, () -> service.findById(UUID.randomUUID()));
    }

    @Test
    void findAllForUser()
    {
        TestUtils.initSecurityContext(auth,null);

        NotificationEntity e1 = TestUtils.getNotificationEntity();
        NotificationEntity e2 = TestUtils.getNotificationEntity();
        Page<NotificationEntity> entitiesPage = new PageImpl<>(List.of(e1, e2));

        when(repository.findAllByUserIdOrderByCreatedAtDesc(any(), any())).thenReturn(entitiesPage);

        NotificationsPageDTO page = service.findAllForUser(PageRequest.of(0, 10));
        assertEquals(2, page.getPageItems());
        assertEquals(0, page.getPageNumber());

        SecurityContextHolder.clearContext();
    }

    @Test
    void testDelete()
    {
        service.delete(UUID.randomUUID());
        verify(repository, times(1)).deleteById(any());
    }

    @Test
    void testDeleteAllForUser()
    {
        TestUtils.initSecurityContext(auth,"user@mail.com");
        service.deleteAllForUser();
        verify(repository, times(1)).deleteAllByUserId("user@mail.com");
    }

    @Test
    void createParticipantAddedNotifications()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        MeetingParticipantEntity p1 = meeting.getParticipants().get(0);
        MeetingParticipantEntity p2 = meeting.getParticipants().get(1);

        service.createParticipantAddedNotifications(meeting, List.of(p1, p2));

        verify(repository, times(1)).saveAll(anyList());
    }

    @Test
    void createParticipantDeletedNotifications()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        MeetingParticipantEntity participant = TestUtils.getMeetingParticipantEntity();

        service.createParticipantDeletedNotifications(meeting, List.of(participant));

        verify(repository, times(1)).saveAll(anyList());
    }

    @Test
    void createDeleteCandidateNotifications()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        MeetingParticipantEntity participant = TestUtils.getMeetingParticipantEntity();
        participant.setRole(Role.ORGANIZER.getValue());
        meeting.setParticipants(List.of(participant));

        service.createDeleteCandidateNotifications(List.of(meeting));

        verify(repository, times(1)).saveAll(anyList());
    }

    @Test
    void createPasswordChangeCandidateNotifications()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        MeetingParticipantEntity participant = TestUtils.getMeetingParticipantEntity();
        participant.setRole(Role.ORGANIZER.getValue());
        meeting.setParticipants(List.of(participant));

        service.createPasswordChangeCandidateNotifications(List.of(meeting));

        verify(repository, times(1)).saveAll(anyList());
    }

    @Test
    void createPasswordChangedNotifications()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        MeetingParticipantEntity participant = TestUtils.getMeetingParticipantEntity();
        participant.setRole(Role.ORGANIZER.getValue());
        meeting.setParticipants(List.of(participant));

        service.createPasswordChangedNotifications(List.of(meeting));
        verify(repository, times(1)).saveAll(anyList());
    }

    @Test
    void updateView()
    {
        NotificationEntity notification = TestUtils.getNotificationEntity();
        when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));

        service.updateView(notification.getId());
        verify(repository, times(1)).findById(notification.getId());
    }

    @Test
    void updateViewAll()
    {
        TestUtils.initSecurityContext(auth, null);
        NotificationEntity notification = TestUtils.getNotificationEntity();
        when(repository.findAllByUserId(any())).thenReturn(List.of(notification));

        service.updateViewAll();
        verify(repository, times(1)).findAllByUserId(any());

        assertTrue(notification.isViewed());
    }
}