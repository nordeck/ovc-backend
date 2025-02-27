package net.nordeck.ovc.backend.service;

import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.dto.MeetingParticipantDTO;
import net.nordeck.ovc.backend.dto.MeetingParticipantRequestDTO;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import net.nordeck.ovc.backend.repository.MeetingParticipantRepository;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.nordeck.ovc.backend.TestUtils.OWNER_EMAIL;
import static net.nordeck.ovc.backend.dto.Role.ORGANIZER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MeetingParticipantServiceImplTest
{

    @Mock
    MeetingParticipantRepository participantRepository;

    @Mock
    MeetingRepository meetingRepository;

    @Mock
    NotificationService notificationService;

    @Mock
    private Authentication auth;

    @InjectMocks
    MeetingParticipantServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new MeetingParticipantServiceImpl(participantRepository, meetingRepository, notificationService);
    }

    @Test
    void findByMeetingId_success()
    {
        MeetingParticipantEntity entity = TestUtils.getMeetingParticipantEntity();
        when(participantRepository.findAllByMeetingId(any())).thenReturn(List.of(entity));

        MeetingParticipantDTO dto = service.findByMeetingId(TestUtils.participantId).get(0);

        assertAll(
                () -> assertEquals(TestUtils.participantId, dto.getId()),
                () -> assertEquals(TestUtils.meetingId, dto.getMeetingId()),
                () -> assertEquals("email", dto.getEmail()),
                () -> assertEquals("MODERATOR", dto.getRole().getValue())
        );
    }

    @Test
    void create_success()
    {
        MeetingParticipantRequestDTO requestDTO = new MeetingParticipantRequestDTO(ORGANIZER, "email");
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        MeetingParticipantEntity participant = TestUtils.getMeetingParticipantEntity();
        when(meetingRepository.findById(any())).thenReturn(Optional.of(meeting));
        when(participantRepository.save(any())).thenReturn(participant);

        service.create(participant.getMeetingId(), requestDTO);

        verify(participantRepository, times(1)).save(any(MeetingParticipantEntity.class));
    }

    @Test
    void create_throwsRuntimeException()
    {
        MeetingParticipantRequestDTO requestDTO = new MeetingParticipantRequestDTO(ORGANIZER, OWNER_EMAIL);
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        MeetingParticipantEntity participant = TestUtils.getMeetingParticipantEntity();
        when(meetingRepository.findById(any())).thenReturn(Optional.of(meeting));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.create(participant.getMeetingId(), requestDTO));
        assertTrue(ex.getLocalizedMessage().contains("Participant already exists with email <"));
    }

    @Test
    void update_success()
    {
        MeetingParticipantRequestDTO requestDTO = new MeetingParticipantRequestDTO(ORGANIZER, "email");
        MeetingParticipantEntity entity = TestUtils.getMeetingParticipantEntity();
        when(participantRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(participantRepository.save(any())).thenReturn(entity);

        service.update(entity.getMeetingId(), entity.getId(), requestDTO);

        verify(participantRepository, times(1)).save(entity);
    }

    @Test
    void delete_success()
    {
        MeetingParticipantEntity participant = TestUtils.getMeetingParticipantEntity();
        List<MeetingParticipantEntity> participants = new ArrayList<>();
        participants.add(participant);
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        meeting.setParticipants(participants);
        when(meetingRepository.findById(any())).thenReturn(Optional.of(meeting));
        when(participantRepository.findById(any())).thenReturn(Optional.of(participant));

        service.delete(participant.getMeetingId(), participant.getId());

        verify(participantRepository, times(1)).deleteById(participant.getId());
    }
}