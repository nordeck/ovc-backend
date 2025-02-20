package net.nordeck.ovc.backend.dto;

import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles(value = "test")
public class RecurrenceDTOTest
{
    RecurrenceDTO recurrence;
    MeetingEntity meeting;

    @BeforeEach
    void setup()
    {
        recurrence = new RecurrenceDTO();
        meeting = TestUtils.getMeetingEntity();
    }

    @Test
    void givenCustomEntity_buildFromEntity_success()
    {
        ZonedDateTime seriesEndDateTime = ZonedDateTime.now().plusDays(5);
        meeting.setStaticRoom(false);
        meeting.setFrequency("CUSTOM");
        meeting.setCustomDays_monday(true);
        meeting.setCustomDays_wednesday(true);
        meeting.setSeriesEndTime(seriesEndDateTime);

        RecurrenceDTO recurrence = DTOUtils.buildRecurrenceFromEntity(meeting);

        assertEquals(RecurrenceFrequency.WEEKLY, recurrence.getFrequency());
        assertTrue(recurrence.getWeekDays().isMonday());
        assertFalse(recurrence.getWeekDays().isTuesday());
        assertTrue(recurrence.getWeekDays().isWednesday());
        assertEquals(meeting.getSeriesEndTime(), recurrence.getEndDate());
    }

    @Test
    void givenDailyEntity_buildFromEntity_success()
    {
        ZonedDateTime seriesEndDateTime = ZonedDateTime.now().plusDays(5);
        meeting.setStaticRoom(false);
        meeting.setFrequency("DAILY");
        meeting.setSeriesEndTime(seriesEndDateTime);

        RecurrenceDTO recurrence = DTOUtils.buildRecurrenceFromEntity(meeting);

        assertEquals(RecurrenceFrequency.DAILY, recurrence.getFrequency());
        assertNull(recurrence.getWeekDays());
        assertEquals(meeting.getSeriesEndTime(), recurrence.getEndDate());
    }

    @Test
    void equals_success()
    {
        ZonedDateTime seriesEndTime = ZonedDateTime.now().plusDays(5);
        RecurrenceDTO r1 = new RecurrenceDTO();
        RecurrenceDTO r2 = new RecurrenceDTO();
        r1.setFrequency(RecurrenceFrequency.WEEKLY);
        r2.setFrequency(RecurrenceFrequency.WEEKLY);
        r1.setEndDate(seriesEndTime);
        r2.setEndDate(seriesEndTime);
        r1.setWeekDays(new WeekDays(true, false, false, false, false, false, false));
        r2.setWeekDays(new WeekDays(true, false, false, false, false, false, false));

        assertTrue(r1.equals(r2));

        r2.setFrequency(RecurrenceFrequency.DAILY);
        assertFalse(r1.equals(r2));
        r2.setFrequency(RecurrenceFrequency.WEEKLY);

        r2.setEndDate(seriesEndTime.minusHours(5));
        assertFalse(r1.equals(r2));
        r2.setEndDate(seriesEndTime);

        r2.getWeekDays().setMonday(false);
        assertFalse(r1.equals(r2));
    }
}
