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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
public class StaticRoomsSetDefaultUserJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingParticipantRepository meetingParticipantRepository;

    @Autowired
    @Qualifier(StaticRoomsSetDefaultUserJob.JOB_NAME)
    private Job job;

    @InjectMocks
    private StaticRoomsSetDefaultUserJob mockedJob;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private JobLauncher jobLauncher;

    @BeforeEach
    void initData() {
        MeetingEntity room1 = TestUtils.getStaticRoom();
        MeetingEntity room2 = TestUtils.getStaticRoom();

        List<List<MeetingParticipantEntity>> participants = List.of(
                room1.getParticipants(),
                room2.getParticipants()
        );

        room1.setParticipants(null);
        room2.setParticipants(null);

        List<MeetingEntity> meetingEntities = meetingRepository.saveAll(List.of(room1, room2));

        // set  meeting id for every participant
        for (int i=0; i<meetingEntities.size(); i++) {
            int finalI = i;
            participants.get(i).forEach(o -> o.setMeetingId(meetingEntities.get(finalI).getId()));
            meetingEntities.get(i).setParticipants(participants.get(i));
        }

        meetingRepository.saveAll(meetingEntities);

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
        jobLauncherTestUtils.setJob(job);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(defaultJobParameters());
        JobInstance actualJobInstance = jobExecution.getJobInstance();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualJobInstance.getJobName(), is(StaticRoomsSetDefaultUserJob.JOB_NAME));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));
    }

    @Test
    void givenTwoMeetingsWithoutOrganizer_whenStepExecuted_thenExpectAllHaveAnOrganizer() {
        jobLauncherTestUtils.setJob(job);

        List<MeetingEntity> roomsBefore = meetingRepository.findAllByStaticRoomIsTrueAndHasOrganizerIsFalse(Limit.of(10));
        assertEquals(2, roomsBefore.size());

        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                StaticRoomsSetDefaultUserJob.STEP_NAME, defaultJobParameters());

        Collection actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualStepExecutions.size(), is(1));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        List<MeetingEntity> roomsAfter = meetingRepository.findAllByStaticRoomIsTrueAndHasOrganizerIsFalse(Limit.of(100));
        assertEquals(0, roomsAfter.size());
    }

    @Test
    void execute() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        mockedJob.execute();
    }

    @Test
    void when_init_thenFail() {
        mockedJob.keycloakService = null;
        mockedJob.init();
    }
}