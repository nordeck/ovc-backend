package net.nordeck.ovc.backend.entity;

import net.nordeck.ovc.backend.dto.MeetingAbstractDTO;
import net.nordeck.ovc.backend.dto.RecurrenceFrequency;
import net.nordeck.ovc.backend.dto.WeekDays;
import net.nordeck.ovc.backend.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles(value = "test")
public class MeetingEntityTest
{
    @Test
    void setRecurrenceForWeeklyMeeting()
    {
        MeetingAbstractDTO dto = TestUtils.getMeetingDTO();
        dto.getRecurrence().setFrequency(RecurrenceFrequency.WEEKLY);
        dto.getRecurrence().setWeekDays(new WeekDays(false, true, false, true, false, false, false));

        MeetingEntity meeting = MeetingEntity.buildFromMeetingAbstractDTO(dto);

        assertTrue(meeting.isCustomDays_tuesday());
        assertTrue(meeting.isCustomDays_thursday());
        assertFalse(meeting.isCustomDays_monday());
        assertFalse(meeting.isCustomDays_wednesday());
        assertEquals("CUSTOM", meeting.getFrequency());
    }

    @Test
    void setters_and_getters()
    {
        MeetingEntity entity = new MeetingEntity();
        entity.setCreatedAt(ZonedDateTime.now());
        entity.setUpdatedAt(ZonedDateTime.now());
        entity.setLastVisitDate(ZonedDateTime.now());
        entity.setHasOrganizer(true);
        entity.setExcluded(true);

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertNotNull(entity.getLastVisitDate());
        assertTrue(entity.isHasOrganizer());
        assertTrue(entity.isExcluded());
    }

}
