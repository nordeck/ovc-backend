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
@ConditionalOnProperty(name = "jobs.meetings-delete-old.enabled")
public class DeleteOldMeetingsJob {

    @Autowired
    MeetingRepository meetingRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${jobs.meetings-delete-old.age-in-days:60}")
    protected int ageInDays;

    @Value("${jobs.meetings-delete-old.chunkSize:200}")
    protected int chunkSize;

    @Scheduled(cron = "${jobs.meetings-delete-old.cron:0 35 1 * * *}", zone = "Europe/Berlin")
    @SchedulerLock(name = "DeleteOldMeetingsJob")
    public void execute() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("JobId", UUID.randomUUID().toString())
                .toJobParameters();
        jobLauncher.run(job(), jobParameters);
    }

    @Bean(name = "DeleteOldMeetingsJob")
    public Job job() {
        return new JobBuilder("DeleteOldMeetingsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1())
                .build();
    }

    /**
     * Step 1: Delete meetings with "end date" before ageInDays
     */
    @Bean(name = "DeleteOldMeetingsJob_Step1")
    protected Step step1() {
        return new StepBuilder("DeleteOldMeetingsJob_Step1", jobRepository)
                .<List<MeetingEntity>, List<MeetingEntity>> chunk(1, transactionManager)
                .reader(step1Reader())
                .writer(step1Writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean(name = "DeleteOldMeetingsJob_Step1Reader")
    protected ItemReader<List<MeetingEntity>> step1Reader() {
        return () -> {
            ZonedDateTime dateBefore = ZonedDateTime.now().minusDays(ageInDays).truncatedTo(ChronoUnit.DAYS);
            List<MeetingEntity> meetings = meetingRepository.findAllByEndTimeBeforeAndStaticRoomIsFalse(dateBefore, Limit.of(chunkSize));
            return meetings.isEmpty() ? null : meetings;
        };
    }

    @Bean(name = "DeleteOldMeetingsJob_Step1Writer")
    protected ItemWriter<List<MeetingEntity>> step1Writer() {
        return chunk -> {
            List meetings = chunk.getItems().stream().flatMap(List::stream).collect(Collectors.toList());
            meetingRepository.deleteAll(meetings);
        };
    }
}
