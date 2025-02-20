package net.nordeck.ovc.backend.dto;

import net.nordeck.ovc.backend.Constants;
import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles(value = "test")
public class DTOUtilsTest
{
    @Test
    void buildFromEntityStatic()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setStaticRoom(true);
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertEquals(MeetingType.STATIC, dto.getType());
    }

    @Test
    void buildFromEntityInstant()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setInstantMeeting(true);
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertEquals(MeetingType.INSTANT, dto.getType());
    }

    @Test
    void buildFromEntityNormalOnce()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertEquals(MeetingType.NORMAL, dto.getType());
        assertEquals(TestUtils.MEETING_NAME, dto.getName());
        assertEquals(TestUtils.MEETING_INFO, dto.getInfo());
        assertEquals(TestUtils.OWNER_EMAIL, dto.getOwnerId());
        assertEquals(TestUtils.START_TIME, dto.getStartTime());
        assertEquals(TestUtils.END_TIME, dto.getEndTime());
        assertEquals(TestUtils.MEETING_PASSWORD, dto.getPassword());
        assertEquals(TestUtils.JIBRI_LINK, dto.getSipJibriLink());
        assertEquals(TestUtils.PHONE_NUMBER, dto.getPhoneNumber());
        assertEquals(TestUtils.CONFERENCE_PIN, dto.getConferencePin());
        assertNull(dto.getRecurrence());
        assertNull(dto.getParentId());
        assertTrue(dto.isLobbyEnabled());
        assertEquals(3, dto.getParticipants().size());
    }

    @Test
    void buildFromEntityNormalDaily()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setFrequency(Frequency.DAILY.getValue());
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertEquals(MeetingType.NORMAL, dto.getType());
        assertEquals(RecurrenceFrequency.DAILY, dto.getRecurrence().getFrequency());
    }

    @Test
    void buildFromEntityNormalWeekly()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setFrequency(Frequency.WEEKLY.getValue());
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertEquals(MeetingType.NORMAL, dto.getType());
        assertEquals(RecurrenceFrequency.WEEKLY, dto.getRecurrence().getFrequency());
    }

    @Test
    void buildFromEntityNormalWeekly_checkExcluded()
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(Constants.DATE_TIME_ISO_8601_FORMAT);
        MeetingEntity entity = TestUtils.getMeetingEntity();
        MeetingEntity excludedChild = TestUtils.getMeetingEntity();
        excludedChild.setExcluded(true);
        entity.setChildren(new ArrayList<>());
        entity.getChildren().add(excludedChild);
        entity.setFrequency(Frequency.WEEKLY.getValue());
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertEquals(MeetingType.NORMAL, dto.getType());
        assertEquals(RecurrenceFrequency.WEEKLY, dto.getRecurrence().getFrequency());
        assertEquals(dateTimeFormatter.format(excludedChild.getStartTime()), dto.getExcludedDates().get(0));
    }

    @Test
    void buildFromEntityNormalMonthly()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setFrequency(Frequency.MONTHLY.getValue());
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertEquals(MeetingType.NORMAL, dto.getType());
        assertEquals(RecurrenceFrequency.MONTHLY, dto.getRecurrence().getFrequency());
    }

    @Test
    void buildFromEntityNormalCustom()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setFrequency(Frequency.CUSTOM.getValue());
        entity.setCustomDays_monday(true);
        entity.setCustomDays_wednesday(true);
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertEquals(MeetingType.NORMAL, dto.getType());
        assertEquals(RecurrenceFrequency.WEEKLY, dto.getRecurrence().getFrequency());
        assertTrue(dto.getRecurrence().getWeekDays().isMonday());
        assertTrue(dto.getRecurrence().getWeekDays().isWednesday());
    }

    @Test
    void buildBasicDTOFromEntityNormalOnce()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        MeetingBasicDTO dto =  DTOUtils.buildBasicDTOFromEntity(entity);

        assertEquals(entity.getId(), dto.getId());
        assertEquals(MeetingType.NORMAL, dto.getType());
        assertEquals(TestUtils.MEETING_NAME, dto.getName());
        assertEquals(TestUtils.START_TIME, dto.getStartTime());
        assertEquals(TestUtils.END_TIME, dto.getEndTime());
        assertNull(dto.getRecurrence());
    }

    @Test
    void buildUpdateDTOFromEntity()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        MeetingUpdateDTO dto =  DTOUtils.buildUpdateDTOFromEntity(entity);

        assertEquals(MeetingType.NORMAL, dto.getType());
        assertEquals(TestUtils.MEETING_NAME, dto.getName());
        assertEquals(TestUtils.START_TIME, dto.getStartTime());
        assertEquals(TestUtils.END_TIME, dto.getEndTime());
        assertNull(dto.getRecurrence());
    }

    @Test
    void hasCustomWeekDays()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setFrequency(Frequency.CUSTOM.getValue());
        entity.setCustomDays_monday(true);
        entity.setCustomDays_tuesday(true);
        entity.setCustomDays_wednesday(true);
        entity.setCustomDays_thursday(true);
        entity.setCustomDays_friday(true);
        entity.setCustomDays_saturday(true);
        entity.setCustomDays_sunday(true);
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertTrue(DTOUtils.hasCustomWeekDays(dto));
    }

    @Test
    void getFrequencyValue()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setFrequency(Frequency.CUSTOM.getValue());
        entity.setCustomDays_monday(true);
        entity.setCustomDays_tuesday(true);
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertEquals(Frequency.CUSTOM.getValue(), DTOUtils.getFrequencyValue(dto));


    }

    @Test
    void hasWeeklyRecurrence()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setFrequency(Frequency.CUSTOM.getValue());
        entity.setCustomDays_monday(true);
        entity.setCustomDays_tuesday(true);
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertTrue(DTOUtils.hasWeeklyRecurrence(dto));

        dto.getRecurrence().setFrequency(RecurrenceFrequency.DAILY);
        assertFalse(DTOUtils.hasWeeklyRecurrence(dto));

        dto.setRecurrence(null);
        assertFalse(DTOUtils.hasWeeklyRecurrence(dto));
    }

    @Test
    void isRecurrentMeeting()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setFrequency(Frequency.CUSTOM.getValue());
        entity.setCustomDays_monday(true);
        entity.setCustomDays_tuesday(true);
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertTrue(DTOUtils.isRecurrentMeeting(dto));

        dto.setType(MeetingType.STATIC);
        assertFalse(DTOUtils.isRecurrentMeeting(dto));
    }

    @Test
    void isRecurrentParentMeeting()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        entity.setFrequency(Frequency.CUSTOM.getValue());
        entity.setCustomDays_monday(true);
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);

        assertTrue(DTOUtils.isRecurrentParentMeeting(dto));
    }

    @Test
    void isNormalMeeting()
    {
        MeetingEntity entity = TestUtils.getMeetingEntity();
        MeetingDTO dto =  DTOUtils.buildFromEntity(entity);
        assertTrue(DTOUtils.isNormalMeeting(dto));

        dto.setType(MeetingType.STATIC);
        assertFalse(DTOUtils.isNormalMeeting(dto));
    }


}
