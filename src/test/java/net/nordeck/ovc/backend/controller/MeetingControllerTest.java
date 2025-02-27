package net.nordeck.ovc.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.dto.*;
import net.nordeck.ovc.backend.service.JitsiService;
import net.nordeck.ovc.backend.service.MeetingParticipantService;
import net.nordeck.ovc.backend.service.MeetingService;
import net.nordeck.ovc.backend.service.PermissionControlService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(value = "test")
public class MeetingControllerTest
{

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MeetingService meetingService;

    @MockBean
    MeetingParticipantService participantService;

    @MockBean
    PermissionControlService permissionControlService;

    @MockBean
    JitsiService jitsiService;

    @Autowired
    ObjectMapper objectMapper;

    private MeetingDTO meetingDTO;
    private MeetingParticipantDTO participantDTO;
    private MeetingParticipantRequestDTO participantRequestDTO;
    private String json, jsonResponse;

    private Authentication auth = TestUtils.initSecurityContext(null, null);;

    @BeforeEach
    void initData() throws IOException
    {
        meetingDTO = TestUtils.getMeetingDTO();
        participantDTO = TestUtils.getMeetingParticipantDTO();
        participantRequestDTO = new MeetingParticipantRequestDTO(participantDTO.getRole(), participantDTO.getEmail());

        json = objectMapper.writeValueAsString(participantRequestDTO);
        jsonResponse = objectMapper.writeValueAsString(participantDTO);
    }

    @AfterEach
    void finishAfterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listParticipants_success() throws Exception
    {
        List<MeetingParticipantDTO> list = List.of(participantDTO);
        jsonResponse = objectMapper.writeValueAsString(list);
        when(participantService.findByMeetingId(any())).thenReturn(list);

        mockMvc.perform(
                        get("/api/v1.0/meetings/{mId}/participants", meetingDTO.getId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(jsonResponse));

        verify(participantService, times(1)).findByMeetingId(eq(meetingDTO.getId()));
    }

    @Test
    void createParticipant_success() throws Exception
    {
        when(participantService.update(any(), any(), any())).thenReturn(participantDTO);

        mockMvc.perform(
                        post("/api/v1.0/meetings/{mId}/participants",
                             participantDTO.getMeetingId(), participantDTO.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isOk());

        verify(participantService, times(1)).create(eq(participantDTO.getMeetingId()), any());
    }

    @Test
    void updateParticipant_success() throws Exception
    {
        when(participantService.update(any(), any(), any())).thenReturn(participantDTO);

        mockMvc.perform(
                        put("/api/v1.0/meetings/{mId}/participants/{pId}",
                            participantDTO.getMeetingId(), participantDTO.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(jsonResponse));

        verify(participantService, times(1)).update(
                eq(participantDTO.getMeetingId()), eq(participantDTO.getId()), any());
    }

    @Test
    void deleteParticipant_success() throws Exception
    {
        mockMvc.perform(
                        delete("/api/v1.0/meetings/{mId}/participants/{pId}",
                               participantDTO.getMeetingId(), participantDTO.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isOk());

        verify(participantService, times(1)).delete(
                participantDTO.getMeetingId(),participantDTO.getId());
    }

    @Test
    void given_meeting_findById_success() throws Exception
    {
        when(meetingService.findById(meetingDTO.getId())).thenReturn(meetingDTO);
        jsonResponse = objectMapper.writeValueAsString(meetingDTO);

        mockMvc.perform(
                        get("/api/v1.0/meetings/{mId}",
                            meetingDTO.getId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(jsonResponse));

        verify(meetingService, times(1)).findById(eq(meetingDTO.getId()));
    }

    @Test
    void given_meeting_findBasicById_success() throws Exception
    {
        MeetingBasicDTO meetingBasicDTO = TestUtils.getMeetingBasicDTO();
        when(meetingService.findBasicById(meetingDTO.getId())).thenReturn(meetingBasicDTO);
        jsonResponse = objectMapper.writeValueAsString(meetingBasicDTO);

        mockMvc.perform(
                        get("/api/v1.0/meetings/{mId}/basic",
                            meetingDTO.getId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(jsonResponse));

        verify(meetingService, times(1)).findBasicById(eq(meetingBasicDTO.getId()));
    }

    @Test
    void given_meeting_getNextOfSeries_success() throws Exception
    {
        MeetingBasicDTO meetingBasicDTO = TestUtils.getMeetingBasicDTO();
        when(meetingService.findNextOfSeries(meetingDTO.getId())).thenReturn(meetingBasicDTO);
        jsonResponse = objectMapper.writeValueAsString(meetingBasicDTO);

        mockMvc.perform(
                        get("/api/v1.0/meetings/{mId}/next-of-series",
                            meetingDTO.getId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(jsonResponse));

        verify(meetingService, times(1)).findNextOfSeries(eq(meetingBasicDTO.getId()));
    }

    @Test
    void given_meeting_getPermissions_success() throws Exception
    {
        MeetingPermissionsDTO permissionsDTO = new MeetingPermissionsDTO();
        permissionsDTO.setUserCanRead(true);
        permissionsDTO.setUserCanEdit(false);
        permissionsDTO.setUserIsParticipant(true);
        permissionsDTO.setMeetingId(meetingDTO.getId());
        when(permissionControlService.getPermissions(meetingDTO.getId())).thenReturn(permissionsDTO);
        jsonResponse = objectMapper.writeValueAsString(permissionsDTO);

        mockMvc.perform(
                        get("/api/v1.0/meetings/{mId}/permissions",
                            meetingDTO.getId())
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(jsonResponse))
                .andExpect(jsonPath("$.user_can_read").value(true))
                .andExpect(jsonPath("$.user_can_edit").value(false))
                .andExpect(jsonPath("$.user_is_participant").value(true));

        verify(permissionControlService, times(1)).getPermissions(meetingDTO.getId());
    }

    @Test
    void given_meeting_create_success() throws Exception
    {
        MeetingCreateDTO createDTO = new MeetingCreateDTO();
        json = objectMapper.writeValueAsString(createDTO);
        when(meetingService.create(createDTO)).thenReturn(meetingDTO);

        mockMvc.perform(
                        post("/api/v1.0/meetings/")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk());

        verify(meetingService, times(1)).create(any());
    }

    @Test
    void given_meeting_update_success() throws Exception
    {
        MeetingUpdateDTO updateDTO = new MeetingUpdateDTO();
        UUID meetingId = UUID.randomUUID();
        json = objectMapper.writeValueAsString(updateDTO);
        when(meetingService.update(meetingId, updateDTO)).thenReturn(meetingDTO);

        mockMvc.perform(
                        put("/api/v1.0/meetings/{mId}", meetingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk());

        verify(meetingService, times(1)).update(any(), any());
    }

    @Test
    void isRegisteredUser_success() throws Exception
    {
        when(meetingService.isRegisteredUser(anyString())).thenReturn(true);
        mockMvc.perform(
                    get("/api/v1.0/meetings/{userId}/registered-user",
                        "user-email")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));

        when(meetingService.isRegisteredUser(anyString())).thenReturn(false);
        mockMvc.perform(
                        get("/api/v1.0/meetings/{userId}/registered-user",
                            "user-email")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").value(false));
    }

    @Test
    void given_meeting_updateLastVisitDate_success() throws Exception
    {
        UUID meetingId = UUID.randomUUID();
        mockMvc.perform(
                        patch("/api/v1.0/meetings/{meetingId}/last-visit-date", meetingId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(meetingService, times(1)).updateStaticRoomsLastVisitDate(meetingId);
    }

    @Test
    void delete_success() throws Exception
    {
        UUID meetingId = UUID.randomUUID();
        mockMvc.perform(
                        delete("/api/v1.0/meetings/{meetingId}", meetingId)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(meetingService, times(1)).delete(meetingId);
    }

    @Test
    void generateLink_success() throws Exception
    {
        MeetingDTO dto = TestUtils.getMeetingDTO();
        JitsiTokenRequestDTO request = new JitsiTokenRequestDTO(
                dto.getPassword(),
                dto.getParticipants().get(0).getEmail(),
                dto.getName(),
                "America/Sao_Paulo");
        json = objectMapper.writeValueAsString(request);
        mockMvc.perform(
                        post("/api/v1.0/meetings/{mId}/jitsi-link", dto.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                        .andExpect(status().isOk());

        verify(jitsiService, times(1)).generateLink(
                dto.getId(), dto.getPassword(), request.getEmail(), dto.getName(), "America/Sao_Paulo");
    }

    @Test
    void findMeetings_success() throws Exception
    {
        List<MeetingDTO> dtos = List.of(TestUtils.getMeetingDTO());
        MeetingsPageDTO page = new MeetingsPageDTO(1, 1, 10, 0, 1, dtos);
        when(meetingService.getMeetingsPage(any(), any(), any(), any(), any(), any())).thenReturn(page);
        mockMvc.perform(
                        get("/api/v1.0/meetings/")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page-items").value(1))
                .andExpect(jsonPath("$.page-size").value(10))
                .andExpect(jsonPath("$.page-number").value(0));

        mockMvc.perform(
                        get("/api/v1.0/meetings/?type=normal&offset=0&size=10&start=2024-09-05T01:00:00.000-01:00&end=2024-09-05T01:00:00.000Z")
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

}