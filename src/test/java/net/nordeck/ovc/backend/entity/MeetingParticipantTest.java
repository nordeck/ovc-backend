package net.nordeck.ovc.backend.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles(value = "test")
public class MeetingParticipantTest
{

    @Test
    void setters_and_getters()
    {
        MeetingParticipantEntity entity = new MeetingParticipantEntity();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setMeetingId(id);
        entity.setUserId("user_id");
        entity.setRole("role");
        entity.setEmail("email");
        entity.setCreatedAt(ZonedDateTime.now());
        entity.setUpdatedAt(ZonedDateTime.now());

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(id, entity.getId());
        assertEquals(id, entity.getMeetingId());
        assertEquals("user_id", entity.getUserId());
        assertEquals("email", entity.getEmail());
        assertEquals("role", entity.getRole());
    }

}
