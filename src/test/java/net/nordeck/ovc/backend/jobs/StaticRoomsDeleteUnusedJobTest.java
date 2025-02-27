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
public class StaticRoomsDeleteUnusedJobTest {

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

    @InjectMocks
    private StaticRoomsPasswordChangeJob mockedJob;

    @Autowired
    @Qualifier("StaticRoomsDeleteUnusedJob")
    private Job job;

    @Value("${jobs.static-room-delete-unused.daysLimit:90}")
    private int daysLimit;

    @Value("${jobs.static-room-delete-unused.daysBefore:15}")
    private int daysBefore;

    @Value("${jobs.static-room-delete-unused.chunkSize:200}")
    private int chunkSize;

    private JobParameters defaultJobParameters() {
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addString("jobId", UUID.randomUUID().toString());
        return paramsBuilder.toJobParameters();
    }

    @BeforeEach
    void initData() {
        MeetingEntity room1 = TestUtils.getStaticRoom();
        MeetingEntity room2 = TestUtils.getStaticRoom();
        MeetingEntity room3 = TestUtils.getStaticRoom();

        List<List<MeetingParticipantEntity>> participants = List.of(
                room1.getParticipants(),
                room2.getParticipants(),
                room3.getParticipants()
        );

        room1.setParticipants(null);
        room2.setParticipants(null);
        room3.setParticipants(null);

        List<MeetingEntity> meetingEntities = meetingRepository.saveAll(List.of(room1, room2, room3));

        meetingEntities.get(0).setLastVisitDate(ZonedDateTime.now().minusDays(6)); // should be deleted by step1
        meetingEntities.get(0).setRoomDeletionDueDate(ZonedDateTime.now().minusDays(10));
        meetingEntities.get(0).setDeleteCandidate(true);

        meetingEntities.get(1).setLastVisitDate(ZonedDateTime.now().minusDays(1)); // should be reset by step2
        meetingEntities.get(1).setRoomDeletionDueDate(ZonedDateTime.now().minusDays(10));
        meetingEntities.get(1).setDeleteCandidate(true);

        meetingEntities.get(2).setLastVisitDate(ZonedDateTime.now().minusDays(3)); // should be marked as candidate by step3
        meetingEntities.get(2).setRoomDeletionDueDate(ZonedDateTime.now().minusDays(10));
        meetingEntities.get(2).setDeleteCandidate(false);


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
    void cleanData() {
        meetingRepository.deleteAll();
        meetingParticipantRepository.deleteAll();
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

        assertThat(actualJobInstance.getJobName(), is("StaticRoomsDeleteUnusedJob"));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));
    }

    @Test
    void givenThreeRooms_whenStep1Executed_thenExpectOneRoomDeleted() {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                "StaticRoomsDeleteUnusedJob_Step1", defaultJobParameters());

        Collection actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualStepExecutions.size(), is(1));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        List<MeetingEntity> roomsAfter = meetingRepository.findAll().stream().toList();
        assertEquals(2, roomsAfter.size());
    }

    @Test
    void givenThreeRooms_whenStep2Executed_thenExpectOneResetCandidate() {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                "StaticRoomsDeleteUnusedJob_Step2", defaultJobParameters());

        Collection actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertEquals(1, actualStepExecutions.size());
        assertEquals("COMPLETED", actualJobExitStatus.getExitCode());

        List<MeetingEntity> roomsAfter = meetingRepository.findAll()
                .stream()
                .filter(room -> !room.isDeleteCandidate() &&
                        room.getRoomDeletionDueDate() == null)
                .toList();
        assertEquals(1, roomsAfter.size());
    }

    @Test
    void givenThreeRooms_whenStep3Executed_thenExpectOneDeleteCandidate() {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                "StaticRoomsDeleteUnusedJob_Step3", defaultJobParameters());

        Collection actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualStepExecutions.size(), is(1));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        ZonedDateTime expectedDueDate = ZonedDateTime.now().plusDays(daysBefore);
        List<MeetingEntity> roomsAfter = meetingRepository.findAll()
                .stream()
                .filter(room -> room.isDeleteCandidate() &&
                        room.getRoomDeletionDueDate().getDayOfYear() == (expectedDueDate.getDayOfYear())
                )
                .toList();
        assertEquals(1, roomsAfter.size());
    }
}