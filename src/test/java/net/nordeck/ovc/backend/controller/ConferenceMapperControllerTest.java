package net.nordeck.ovc.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.nordeck.ovc.backend.dto.MapperJibriResponseDTO;
import net.nordeck.ovc.backend.dto.MapperJigasiResponseDTO;
import net.nordeck.ovc.backend.service.ConferenceMapperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles(value = "test")
public class ConferenceMapperControllerTest
{

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ConferenceMapperService service;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void findByJigasiConference() throws Exception
    {
        UUID conferenceId = UUID.randomUUID();
        MapperJigasiResponseDTO response = new MapperJigasiResponseDTO("message", 123456789L, conferenceId.toString());
        String json = objectMapper.writeValueAsString(response);
        when(service.findByJigasiConferenceId("bbbf0e3f-9cd5-4759-9ead-767b078c04aa")).thenReturn(response);

        mockMvc.perform(get("/api/v1.0/conference-mapper/jigasi/by-meeting-id?conference={conference}",
                            "bbbf0e3f-9cd5-4759-9ead-767b078c04aa@conference.jitsi.integration.dvb.nordeck.io"))
                .andExpect(status().isOk())
                .andExpect(content().json(json));
    }

    @Test
    void findByJigasiPin() throws Exception
    {
        UUID conferenceId = UUID.randomUUID();
        MapperJigasiResponseDTO response = new MapperJigasiResponseDTO("message", 123456789L, conferenceId.toString());
        String json = objectMapper.writeValueAsString(response);
        when(service.findByJigasiConferencePin(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1.0/conference-mapper/jigasi/by-pin?id={conferencePin}", "123456789"))
                .andExpect(status().isOk())
                .andExpect(content().json(json));
    }

    @Test
    void findBySipJibriPin() throws Exception
    {
        MapperJibriResponseDTO response = new MapperJibriResponseDTO("host", "room-id", "token");
        String json = objectMapper.writeValueAsString(response);
        when(service.findBySipJibriConferencePin(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/v1.0/conference-mapper/sipjibri/by-pin?pin={conferencePin}", "123456789"))
                .andExpect(status().isOk())
                .andExpect(content().json(json));
    }
}
