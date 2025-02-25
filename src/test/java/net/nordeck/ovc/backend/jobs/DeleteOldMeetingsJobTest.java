package net.nordeck.ovc.backend.jobs;

import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import net.nordeck.ovc.backend.repository.MeetingParticipantRepository;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
public class DeleteOldMeetingsJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingParticipantRepository meetingParticipantRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("DeleteOldMeetingsJob")
    private Job job;

    @InjectMocks
    private DeleteOldMeetingsJob mockedJob;

    @Value("${jobs.meetings-delete-old.age-in-days}")
    private int ageInDays;

    @Value("${jobs.meetings-delete-old.chunkSize}")
    private int chunkSize;

    @BeforeEach
    void initData() {
        MeetingEntity m1 = TestUtils.getMeetingEntity();
        MeetingEntity m2 = TestUtils.getMeetingEntity();
        MeetingEntity m3 = TestUtils.getMeetingEntity();
        m1.setEndTime(ZonedDateTime.now().minusDays(65));
        m2.setEndTime(ZonedDateTime.now().minusDays(65));
        m3.setEndTime(ZonedDateTime.now().minusDays(30));

        List<List<MeetingParticipantEntity>> participants = List.of(
                m1.getParticipants(),
                m2.getParticipants(),
                m3.getParticipants()
        );

        m1.setParticipants(null);
        m2.setParticipants(null);
        m3.setParticipants(null);

        List<MeetingEntity> meetingEntities = meetingRepository
                .saveAll(List.of(m1, m2, m3));

        // set  meeting id for every participant
        for (int i=0; i<meetingEntities.size(); i++) {
            int finalI = i;
            participants.get(i).forEach(o -> o.setMeetingId(meetingEntities.get(finalI).getId()));
            meetingEntities.get(i).setParticipants(participants.get(i));
        }

        meetingRepository.saveAll(meetingEntities);

        mockedJob.meetingRepository = meetingRepository;
        mockedJob.chunkSize = chunkSize;
        jobLauncherTestUtils.setJob(job);
    }

    @AfterEach
    public void cleanUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        meetingRepository.deleteAll();
        meetingParticipantRepository.deleteAll();
    }

    private JobParameters defaultJobParameters() {
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addString("jobId", UUID.randomUUID().toString());
        return paramsBuilder.toJobParameters();
    }

    @Test
    void successWhenExecuted() throws Exception {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(defaultJobParameters());
        JobInstance actualJobInstance = jobExecution.getJobInstance();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualJobInstance.getJobName(), is("DeleteOldMeetingsJob"));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));
    }

    @Test
    void givenThreeMeetings_whenStepExecuted_thenExpectTwoHaveBeenDeleted() {
        ZonedDateTime dateBefore = ZonedDateTime.now().minusDays(ageInDays);
        List<MeetingEntity> roomsBefore = meetingRepository.findAllByEndTimeBeforeAndStaticRoomIsFalse(dateBefore, Limit.of(10));
        assertEquals(2, roomsBefore.size());

        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                "DeleteOldMeetingsJob_Step1", defaultJobParameters());

        Collection<StepExecution> actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualStepExecutions.size(), is(1));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        List<MeetingEntity> roomsAfter = meetingRepository.findAllByEndTimeBeforeAndStaticRoomIsFalse(dateBefore, Limit.of(10));
        assertEquals(0, roomsAfter.size());
    }

    @Test
    void execute() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        mockedJob.execute();
    }
}