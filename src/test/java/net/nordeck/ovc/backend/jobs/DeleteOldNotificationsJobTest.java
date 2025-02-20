package net.nordeck.ovc.backend.jobs;

import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.entity.NotificationEntity;
import net.nordeck.ovc.backend.repository.NotificationRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
public class DeleteOldNotificationsJobTest
{

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private NotificationRepository notificationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier(DeleteOldNotificationsJob.JOB_NAME)
    private Job job;

    @InjectMocks
    private DeleteOldNotificationsJob mockedJob;

    @Value("${jobs.notifications-delete-old.age-in-days}")
    private int ageInDays;

    @Value("${jobs.notifications-delete-old.chunkSize}")
    private int chunkSize;

    @BeforeEach
    void initData()
    {
        NotificationEntity m1 = TestUtils.getNotificationEntity();
        NotificationEntity m2 = TestUtils.getNotificationEntity();
        NotificationEntity m3 = TestUtils.getNotificationEntity();

        m1.setCreatedAt(ZonedDateTime.now().minusDays(45));
        m2.setCreatedAt(ZonedDateTime.now().minusDays(35));
        m3.setCreatedAt(ZonedDateTime.now().minusDays(20));

        notificationRepository.saveAll(List.of(m1, m2, m3));

        mockedJob.notificationRepository = notificationRepository;
        mockedJob.chunkSize = chunkSize;
        jobLauncherTestUtils.setJob(job);
    }

    @AfterEach
    public void cleanUp()
    {
        jobRepositoryTestUtils.removeJobExecutions();
        notificationRepository.deleteAll();
    }

    private JobParameters defaultJobParameters()
    {
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addString("jobId", UUID.randomUUID().toString());
        return paramsBuilder.toJobParameters();
    }

    @Test
    void execute() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException
    {
        mockedJob.execute();
    }

    @Test
    void successWhenExecuted() throws Exception
    {
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(defaultJobParameters());
        JobInstance actualJobInstance = jobExecution.getJobInstance();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualJobInstance.getJobName(), is(DeleteOldNotificationsJob.JOB_NAME));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));
    }

    @Test
    void givenThreeMeetings_whenStepExecuted_thenExpectTwoHaveBeenDeleted()
    {
        ZonedDateTime dateBefore = ZonedDateTime.now().minusDays(ageInDays).truncatedTo(ChronoUnit.HOURS);
        List<NotificationEntity> roomsBefore = notificationRepository.findAllByCreatedAtBefore(dateBefore,
                                                                                               Limit.of(chunkSize));
        assertEquals(2, roomsBefore.size());

        JobExecution jobExecution = jobLauncherTestUtils.launchStep(
                "DeleteOldNotificationsJob_Step1", defaultJobParameters());

        Collection<StepExecution> actualStepExecutions = jobExecution.getStepExecutions();
        ExitStatus actualJobExitStatus = jobExecution.getExitStatus();

        assertThat(actualStepExecutions.size(), is(1));
        assertThat(actualJobExitStatus.getExitCode(), is("COMPLETED"));

        List<NotificationEntity> roomsAfter = notificationRepository.findAll();
        assertEquals(1, roomsAfter.size());
    }
}