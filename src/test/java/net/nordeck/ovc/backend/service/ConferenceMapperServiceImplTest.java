package net.nordeck.ovc.backend.service;

import net.nordeck.ovc.backend.dto.DTOUtils;
import net.nordeck.ovc.backend.dto.MapperJibriResponseDTO;
import net.nordeck.ovc.backend.dto.MapperJigasiResponseDTO;
import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.nordeck.ovc.backend.service.ConferenceMapperService.MAPPING_NOT_FOUND;
import static net.nordeck.ovc.backend.service.ConferenceMapperService.MAPPING_SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles(value = "test")
@SpringBootTest
public class ConferenceMapperServiceImplTest
{

    @Mock
    MeetingRepository meetingRepository;

    @Mock
    MeetingService meetingService;

    @Mock
    JitsiService jitsiService;

    @InjectMocks
    ConferenceMapperServiceImpl conferenceMapperService;

    @BeforeEach
    void setup()
    {
        meetingRepository = mock(MeetingRepository.class);
        jitsiService = mock(JitsiServiceImpl.class);
        meetingService = mock(MeetingServiceImpl.class);
        conferenceMapperService = new ConferenceMapperServiceImpl(meetingRepository, meetingService, jitsiService);
    }

    @Test
    public void findByJigasiConferencePin_returns_NotFoundResponse()
    {
        when(meetingRepository.findByConferencePin(anyString())).thenReturn(null);

        MapperJigasiResponseDTO response = conferenceMapperService.findByJigasiConferencePin("1234");

        assertEquals(MapperJigasiResponseDTO.class, response.getClass());
        assertEquals(response.getMessage(), MAPPING_NOT_FOUND);
    }

    @Test
    public void findByJigasiConferencePin_returns_ValidResponse()
    {
        MeetingEntity meeting = TestUtils.getMeetingEntity();
        List<MeetingEntity> entities = List.of(meeting);
        when(meetingRepository.findByConferencePin(anyString())).thenReturn(entities);

        MapperJigasiResponseDTO response = conferenceMapperService.findByJigasiConferencePin("1234");

        assertEquals(MapperJigasiResponseDTO.class, response.getClass());
        assertEquals(response.getMessage(), MAPPING_SUCCESSFUL);
    }

    @Test
    public void findByJigasiConferencePin_returns_ValidResponse2()
    {
        MeetingEntity m1 = TestUtils.getMeetingEntity();
        MeetingEntity m2 = TestUtils.getMeetingEntity();
        m2.setParentId(m1.getId());
        m2.setEndTime(ZonedDateTime.now().plusHours(1));
        List<MeetingEntity> entities = List.of(m1, m2);
        when(meetingRepository.findByConferencePin(anyString())).thenReturn(entities);
        when(meetingRepository.findByParentIdAndExcludedFalseOrderByEndTimeAsc(any())).thenReturn(List.of(m2));
        when(meetingService.findNextOfSeries(m2.getId())).thenReturn(DTOUtils.buildBasicDTOFromEntity(m2));

        MapperJigasiResponseDTO response = conferenceMapperService.findByJigasiConferencePin("1234");

        assertEquals(MapperJigasiResponseDTO.class, response.getClass());
        assertEquals(response.getMessage(), MAPPING_SUCCESSFUL);
    }

    @Test
    public void findByJigasiConferenceId_returns_ValidResponse()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setConferencePin("123456789");
        when(meetingRepository.findById(any())).thenReturn(Optional.of(entity));

        MapperJigasiResponseDTO response = conferenceMapperService.findByJigasiConferenceId(entity.getId().toString());

        assertEquals(MapperJigasiResponseDTO.class, response.getClass());
        assertEquals(response.getMessage(), MAPPING_SUCCESSFUL);
    }

    @Test
    public void findByJigasiConferenceId_returns_NotFoundResponse()
    {
        when(meetingRepository.findById(any())).thenReturn(Optional.empty());

        MapperJigasiResponseDTO response = conferenceMapperService.findByJigasiConferenceId(UUID.randomUUID().toString());

        assertEquals(MapperJigasiResponseDTO.class, response.getClass());
        assertEquals(response.getMessage(), MAPPING_NOT_FOUND);
    }

    @Test
    public void findBySipJibriConferencePin_returns_ValidResponse()
    {
        List<MeetingEntity> entities = List.of(TestUtils.getMeetingEntity());
        when(meetingRepository.findByConferencePin(anyString())).thenReturn(entities);

        MapperJibriResponseDTO response = conferenceMapperService.findBySipJibriConferencePin("1234");

        assertEquals(MapperJibriResponseDTO.class, response.getClass());
        assertEquals(response.getRoom(), entities.get(0).getId().toString());
        assertEquals(response.getHost(), conferenceMapperService.jitsiHost);
    }

    @Test
    public void findBySipJibriConferencePin_returns_ValidResponse2()
    {
        MeetingEntity m1 = TestUtils.getMeetingEntity();
        MeetingEntity m2 = TestUtils.getMeetingEntity();
        m2.setParentId(m1.getId());
        m2.setEndTime(ZonedDateTime.now().plusHours(1));
        List<MeetingEntity> entities = List.of(m1, m2);
        when(meetingRepository.findByConferencePin(anyString())).thenReturn(entities);
        when(meetingRepository.findByParentIdAndExcludedFalseOrderByEndTimeAsc(any())).thenReturn(List.of(m2));
        when(meetingService.findNextOfSeries(m2.getId())).thenReturn(DTOUtils.buildBasicDTOFromEntity(m2));

        MapperJibriResponseDTO response = conferenceMapperService.findBySipJibriConferencePin("1234");

        assertEquals(MapperJibriResponseDTO.class, response.getClass());
        assertEquals(response.getRoom(), entities.get(0).getId().toString());
        assertEquals(response.getHost(), conferenceMapperService.jitsiHost);
    }

    @Test
    public void findBySipJibriConferencePin_returns_NotFoundResponse()
    {
        when(meetingRepository.findByConferencePin(anyString())).thenReturn(List.of());
        when(meetingRepository.findByParentIdAndExcludedFalseOrderByEndTimeAsc(any())).thenReturn(List.of());

        MapperJibriResponseDTO response = conferenceMapperService.findBySipJibriConferencePin("1234");

        assertEquals(MapperJibriResponseDTO.class, response.getClass());
        assertNull(response.getRoom());
        assertNull(response.getHost());
        assertNull(response.getToken());
    }
}
