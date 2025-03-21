package net.nordeck.ovc.backend.jobs;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(name = "jobs.instant-meeting-delete-old.enabled")
public class DeleteOldInstantMeetingsJob
{
    public static final String JOB_NAME = "DeleteOldInstantMeetingsJob";

    @Autowired
    MeetingRepository meetingRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${jobs.instant-meeting-delete-old.age-in-days-started:10}")
    protected int ageInDaysStarted;

    @Value("${jobs.instant-meeting-delete-old.chunkSize:200}")
    protected int chunkSize;

    @Scheduled(cron = "${jobs.instant-meeting-delete-old.cron:0 35 1 * * *}", zone = "Europe/Berlin")
    @SchedulerLock(name = JOB_NAME)
    public void execute() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
            JobParametersInvalidException, JobRestartException
    {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("JobId", UUID.randomUUID().toString())
                .toJobParameters();
        jobLauncher.run(job(), jobParameters);
    }

    @Bean(name = JOB_NAME)
    public Job job()
    {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1())
                .on("*").to(step2())
                .end()
                .build();
    }

    /**
     * Step 1: Delete meetings with "started_at" before ageInDaysStarted
     */
    @Bean(name = JOB_NAME + "_Step1")
    protected Step step1()
    {
        return new StepBuilder(JOB_NAME + "_Step1", jobRepository)
                .<List<MeetingEntity>, List<MeetingEntity>>chunk(1, transactionManager)
                .reader(step1Reader())
                .writer(step1Writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * Step 2: Delete instant meetings (start_time == null) with "started_at == null"
     */
    @Bean(name = JOB_NAME + "_Step2")
    protected Step step2()
    {
        return new StepBuilder(JOB_NAME + "_Step2", jobRepository)
                .<List<MeetingEntity>, List<MeetingEntity>>chunk(1, transactionManager)
                .reader(step2Reader())
                .writer(step2Writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean(name = JOB_NAME + "_Step1Reader")
    protected ItemReader<List<MeetingEntity>> step1Reader()
    {
        return () -> {
            ZonedDateTime dateBefore = ZonedDateTime.now().minusDays(ageInDaysStarted).truncatedTo(ChronoUnit.DAYS);
            List<MeetingEntity> meetings = meetingRepository.findAllByStartTimeIsNullAndStartedAtBefore(
                    dateBefore, Limit.of(chunkSize));
            return meetings.isEmpty() ? null : meetings;
        };
    }

    @Bean(name = JOB_NAME + "_Step1Writer")
    protected ItemWriter<List<MeetingEntity>> step1Writer()
    {
        return chunk -> {
            List<MeetingEntity> meetings = chunk.getItems().stream().flatMap(List::stream).collect(Collectors.toList());
            meetingRepository.deleteAll(meetings);
        };
    }

    @Bean(name = JOB_NAME + "_Step2Reader")
    protected ItemReader<List<MeetingEntity>> step2Reader()
    {
        return () -> {
            List<MeetingEntity> meetings = meetingRepository.findAllByStartTimeIsNullAndStartedAtIsNull(Limit.of(chunkSize));
            return meetings.isEmpty() ? null : meetings;
        };
    }

    @Bean(name = JOB_NAME + "_Step2Writer")
    protected ItemWriter<List<MeetingEntity>> step2Writer()
    {
        return chunk -> {
            List<MeetingEntity> meetings = chunk.getItems().stream().flatMap(List::stream).collect(Collectors.toList());
            meetingRepository.deleteAll(meetings);
        };
    }
}
