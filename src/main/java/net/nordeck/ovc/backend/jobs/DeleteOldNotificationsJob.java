package net.nordeck.ovc.backend.jobs;

/*
 * Copyright 2025 Nordeck IT + Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.nordeck.ovc.backend.entity.NotificationEntity;
import net.nordeck.ovc.backend.repository.NotificationRepository;
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

@Configuration
@ConditionalOnProperty(name = "jobs.notifications-delete-old.enabled")
public class DeleteOldNotificationsJob
{

    public static final String JOB_NAME = "DeleteOldNotificationsJob";

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${jobs.notifications-delete-old.age-in-days:60}")
    protected int ageInDays;

    @Value("${jobs.notifications-delete-old.chunkSize:200}")
    protected int chunkSize;

    @Scheduled(cron = "${jobs.notifications-delete-old.cron:0 0 1 * * *}", zone = "Europe/Berlin")
    @SchedulerLock(name = JOB_NAME)
    public void execute() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException
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
                .build();
    }

    /**
     * Step 1: Delete notifications with "create_at" before ageInDays
     */
    @Bean(name = "DeleteOldNotificationsJob_Step1")
    protected Step step1()
    {
        return new StepBuilder("DeleteOldNotificationsJob_Step1", jobRepository)
                .<List<NotificationEntity>, List<NotificationEntity>> chunk(1, transactionManager)
                .reader(step1Reader())
                .writer(step1Writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean(name = "DeleteOldNotificationsJob_Step1Reader")
    protected ItemReader<List<NotificationEntity>> step1Reader()
    {
        return () ->
        {
            ZonedDateTime dateBefore = ZonedDateTime.now().minusDays(ageInDays).truncatedTo(ChronoUnit.DAYS);
            List<NotificationEntity> notifications = notificationRepository.findAllByCreatedAtBefore(dateBefore, Limit.of(chunkSize));
            return notifications.isEmpty() ? null : notifications;
        };
    }

    @Bean(name = "DeleteOldNotificationsJob_Step1Writer")
    protected ItemWriter<List<NotificationEntity>> step1Writer()
    {
        return chunk ->
        {
            List<NotificationEntity> notifications = chunk.getItems().stream().flatMap(List::stream).toList();
            notificationRepository.deleteAll(notifications);
        };
    }
}
