package net.nordeck.ovc.backend.jobs;

import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
public class StaticRoomsPasswordChangeJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private MeetingRepository meetingRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private JobLauncher jobLauncher;

    @InjectMocks
    private StaticRoomsPasswordChangeJob mockedJob;

    @Autowired
    @Qualifier("StaticRoomsPasswordChangeJob")
    private Job job;

    @Value("${jobs.static-room-password-change.daysBefore}")
    private int daysBefore;

    @Value("${jobs.static-room-password-change.chunkSize}")
    private int chunkSize;

    private JobParameters defaultJobParameters() {
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addString("jobId", UUID.randomUUID().toString());
        return paramsBuilder.toJobParameters();
    }

    @BeforeEach
    void initData() {
        MeetingEntity room1 = TestUtils.getStaticRoom();
        MeetingEntity room3 = TestUtils.getStaticRoom();
        MeetingEntity room2 = TestUtils.getStaticRoom();


        List<List<MeetingParticipantEntity>> participants = List.of(
                room1.getParticipants(),
                room2.getParticipants(),
                room3.getParticipants()
        );

        // save rooms without participants
        room1.setParticipants(null);
        room2.setParticipants(null);
        room3.setParticipants(null);
        List<MeetingEntity> meetingEntities = meetingRepository.saveAll(List.of(room1, room2, room3));

        // save modified rooms with participants
        meetingEntities.get(0).setLastPasswordChange(ZonedDateTime.now().plusDays(-6)); // should be changed by step1
        meetingEntities.get(0).setPasswordChangeDueDate(ZonedDateTime.now().plusDays(-10));
        meetingEntities.get(0).setPasswordChangeCandidate(true);

        meetingEntities.get(1).setLastPasswordChange(ZonedDateTime.now().plusDays(-1)); // should be reset by step2
        meetingEntities.get(1).setPasswordChangeDueDate(ZonedDateTime.now().plusDays(-10));
        meetingEntities.get(1).setPasswordChangeCandidate(true);

        meetingEntities.get(2).setLastPasswordChange(ZonedDateTime.now().plusDays(-4)); // should be marked as new candidate by step3
        meetingEntities.get(2).setPasswordChangeDueDate(ZonedDateTime.now().plusDays(-10));
        meetingEntities.get(2).setPasswordChangeCandidate(false);

        // set  meeting id for every participant
        for (int i=0; i<meetingEntities.size(); i++) {
            int finalI = i;
            participants.get(i).forEach(o -> o.setMeetingId(meetingEntities.get(finalI).getId()));
            meetingEntities.get(i).setParticipants(participants.get(i));
        }

        meetingRepository.saveAll(meetingEntities);

        mockedJob.meetingRepository = meetingRepository;
        mockedJob.chunkSize = chunkSize;
    }

    @AfterEach
    public void cleanUp() {
        meetingRepository.deleteAll();
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    void execute() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        mockedJob.execute();
    }

    @Test
    void successWhenExecuted() throws Exception {
        jobLauncherTestUtils.setJob(job);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(defaultJobParameters());
        JobInstance actualJobInstance = jobExecution.getJobInstance();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualJobInstance.getJobName(), is("StaticRoomsPasswordChangeJob"));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));
    }

    @Test
    void givenThreeRooms_whenStep1Executed_thenExpectOnePasswordChanged() {
        jobLauncherTestUtils.setJob(job);

        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                "StaticRoomsPasswordChangeJob_Step1", defaultJobParameters());

        Collection actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualStepExecutions.size(), is(1));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        List<MeetingEntity> roomsAfter = meetingRepository.findAll()
                .stream()
                .filter(room -> room.getLastPasswordChange().getDayOfYear() == (ZonedDateTime.now().getDayOfYear()))
                .toList();
        assertEquals(1, roomsAfter.size());
    }

    @Test
    void givenThreeRooms_whenStep2Executed_thenExpectOneResetCandidate() {
        jobLauncherTestUtils.setJob(job);

        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                "StaticRoomsPasswordChangeJob_Step2", defaultJobParameters());

        Collection actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualStepExecutions.size(), is(1));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        List<MeetingEntity> roomsAfter = meetingRepository.findAll()
                .stream()
                .filter(room -> !room.isPasswordChangeCandidate() && room.getPasswordChangeDueDate() == null)
                .toList();
        assertEquals(1, roomsAfter.size());
    }

    @Test
    void givenThreeRooms_whenStep3Executed_thenExpectOnePasswordChangeCandidate() {
        jobLauncherTestUtils.setJob(job);

        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                "StaticRoomsPasswordChangeJob_Step3", defaultJobParameters());

        Collection actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualStepExecutions.size(), is(1));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        ZonedDateTime expectedDueDate = ZonedDateTime.now().plusDays(daysBefore);
        List<MeetingEntity> roomsAfter = meetingRepository.findAll()
                .stream()
                .filter(room -> room.isPasswordChangeCandidate() &&
                        room.getPasswordChangeDueDate().getDayOfYear() == (expectedDueDate.getDayOfYear()))
                .toList();
        assertEquals(1, roomsAfter.size());
    }
}