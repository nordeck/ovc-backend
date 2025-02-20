package net.nordeck.ovc.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.nordeck.ovc.backend.dto.NotificationDTO;
import net.nordeck.ovc.backend.dto.NotificationsPageDTO;
import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(value = "test")
public class NotificationControllerTest
{
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    NotificationService service;

    @Mock
    private Authentication auth;

    @BeforeEach
    void initBeforeEach()
    {
        TestUtils.initSecurityContext(auth, null);
    }

    @AfterEach
    void finishAfterEach()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findForUser() throws Exception
    {
        NotificationDTO dto1 = TestUtils.getNotificationDTO();
        NotificationDTO dto2 = TestUtils.getNotificationDTO();
        ZonedDateTime startDate = ZonedDateTime.now().minusDays(4);
        ZonedDateTime endDate = ZonedDateTime.now().plusDays(4);
        NotificationsPageDTO page = new NotificationsPageDTO(1, 2, 10, 0, 2, List.of(dto1, dto2));

        when(service.findAllForUser(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1.0/notifications/?start-date=" + startDate + "&end-date=" + endDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page-items").value(2))
                .andExpect(jsonPath("$.total-items").value(2))
                .andExpect(jsonPath("$.page-size").value(10))
                .andExpect(jsonPath("$.page-number").value(0));

    }

    @Test
    void delete_success() throws Exception
    {
        NotificationDTO notification = TestUtils.getNotificationDTO();
        when(service.findById(notification.getId())).thenReturn(notification);
        doNothing().when(service).delete(notification.getId());

        mockMvc.perform(delete("/api/v1.0/notifications/{id}", notification.getId()))
                .andExpect(status().isOk());

        verify(service, times(1)).delete(notification.getId());
    }

    @Test
    void deleteAll_success() throws Exception
    {
        doNothing().when(service).deleteAllForUser();

        mockMvc.perform(delete("/api/v1.0/notifications/all"))
                .andExpect(status().isOk());

        verify(service, times(1)).deleteAllForUser();
    }

    @Test
    void updateView_success() throws Exception
    {
        NotificationDTO dto = TestUtils.getNotificationDTO();
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(patch("/api/v1.0/notifications/{id}/view", dto.getId())
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service, times(1)).updateView(dto.getId());
    }

    @Test
    void updateViewAll_success() throws Exception
    {
        NotificationDTO dto = TestUtils.getNotificationDTO();
        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(patch("/api/v1.0/notifications/view-all", dto.getId())
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(service, times(1)).updateViewAll();
    }
}
