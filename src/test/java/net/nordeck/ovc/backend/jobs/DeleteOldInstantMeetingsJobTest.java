package net.nordeck.ovc.backend.jobs;

import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.entity.MeetingEntity;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static net.nordeck.ovc.backend.jobs.DeleteOldInstantMeetingsJob.JOB_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
public class DeleteOldInstantMeetingsJobTest
{

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
    private DeleteOldInstantMeetingsJob mockedJob;

    @Autowired
    @Qualifier(JOB_NAME)
    private Job job;

    @AfterEach
    public void cleanUp()
    {
        jobRepositoryTestUtils.removeJobExecutions();
        meetingRepository.deleteAll();
    }

    private JobParameters defaultJobParameters()
    {
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addString("jobId", UUID.randomUUID().toString());
        return paramsBuilder.toJobParameters();
    }

    @BeforeEach
    void initData()
    {
        MeetingEntity m1 = TestUtils.getMeetingEntity();
        MeetingEntity m2 = TestUtils.getMeetingEntity();
        MeetingEntity m3 = TestUtils.getMeetingEntity();

        m1.setStartTime(null);
        m2.setStartTime(null);
        m3.setStartTime(null);

        m1.setParticipants(null);
        m2.setParticipants(null);
        m3.setParticipants(null);

        m1.setStartedAt(ZonedDateTime.now().minusDays(15));
        m2.setStartedAt(ZonedDateTime.now().minusDays(8));
        m3.setStartedAt(null);

        List<MeetingEntity> meetingEntities = meetingRepository.saveAll(List.of(m1, m2, m3));
        meetingRepository.saveAll(meetingEntities);

        mockedJob.meetingRepository = meetingRepository;
        mockedJob.chunkSize = 10;
        jobLauncherTestUtils.setJob(job);
    }

    @Test
    void execute() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
            JobParametersInvalidException, JobRestartException
    {
        mockedJob.execute();
    }

    @Test
    void successWhenExecuted() throws Exception
    {
        jobLauncherTestUtils.setJob(job);
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(defaultJobParameters());
        JobInstance actualJobInstance = jobExecution.getJobInstance();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualJobInstance.getJobName(), is(JOB_NAME));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        List<MeetingEntity> meetingsAfter = meetingRepository.findAll().stream().toList();
        assertEquals(1, meetingsAfter.size());
    }

    @Test
    void givenThreeMeetings_whenStep1Executed_thenExpectOneMeetingDeleted()
    {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                JOB_NAME + "_Step1", defaultJobParameters());

        Collection actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualStepExecutions.size(), is(1));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        List<MeetingEntity> meetingsAfter = meetingRepository.findAll().stream().toList();
        assertEquals(2, meetingsAfter.size());
    }

    @Test
    void givenThreeMeetings_whenStep2Executed_thenExpectOneMeetingDeleted()
    {
        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                JOB_NAME + "_Step2", defaultJobParameters());

        Collection actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualStepExecutions.size(), is(1));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        List<MeetingEntity> meetingsAfter = meetingRepository.findAll().stream().toList();
        assertEquals(2, meetingsAfter.size());
    }

}
