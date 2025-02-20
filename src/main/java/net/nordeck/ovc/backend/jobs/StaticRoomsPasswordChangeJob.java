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
import net.nordeck.ovc.backend.service.MeetingService;
import net.nordeck.ovc.backend.service.NotificationService;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(name = "jobs.static-room-password-change.enabled")
public class StaticRoomsPasswordChangeJob
{

    @Autowired
    protected MeetingRepository meetingRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${jobs.static-room-password-change.chunkSize:200}")
    protected int chunkSize;

    @Value("${jobs.static-room-password-change.daysLimit:30}")
    protected int daysLimit;

    @Value("${jobs.static-room-password-change.daysBefore:5}")
    protected int daysBefore;

    @Value("${jobs.static-room-password-change.passwordLength:8}")
    protected int passwordLength;

    @Scheduled(cron = "${jobs.static-room-password-change.cron:0 0 1 * * *}", zone = "Europe/Berlin")
    @SchedulerLock(name = "StaticRoomsPasswordChangeJob")
    public void execute() throws JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException, JobInstanceAlreadyCompleteException
    {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("JobId", UUID.randomUUID().toString())
                .toJobParameters();
        jobLauncher.run(job(), jobParameters);
    }

    @Bean(name = "StaticRoomsPasswordChangeJob")
    public Job job()
    {
        return new JobBuilder("StaticRoomsPasswordChangeJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1())
                .on("*").to(step2())
                .on("*").to(step3())
                .end()
                .build();
    }

    /**
     * Step 1: change passwords of rooms with expired "last password change" date
     */
    @Bean(name = "StaticRoomsPasswordChangeJob_Step1")
    protected Step step1()
    {
        return new StepBuilder("StaticRoomsPasswordChangeJob_Step1", jobRepository)
                .<List<MeetingEntity>, List<MeetingEntity>>chunk(1, transactionManager)
                .reader(step1Reader())
                .writer(step1Writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * Step 2: reset the flag "password change candidate" for rooms with changed due date
     */
    @Bean(name = "StaticRoomsPasswordChangeJob_Step2")
    protected Step step2()
    {
        return new StepBuilder("StaticRoomsPasswordChangeJob_Step2", jobRepository)
                .<List<MeetingEntity>, List<MeetingEntity>>chunk(1, transactionManager)
                .reader(step2Reader())
                .writer(step2Writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * Step 3: Find new password change candidates, set their flag and due date
     */
    @Bean(name = "StaticRoomsPasswordChangeJob_Step3")
    protected Step step3()
    {
        return new StepBuilder("StaticRoomsPasswordChangeJob_Step3", jobRepository)
                .<List<MeetingEntity>, List<MeetingEntity>>chunk(1, transactionManager)
                .reader(step3Reader())
                .writer(step3Writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean(name = "StaticRoomsPasswordChangeJob_Step1Reader")
    protected ItemReader<List<MeetingEntity>> step1Reader()
    {
        return () -> {
            ZonedDateTime dateLimit = ZonedDateTime.now().minusDays(daysLimit);
            List<MeetingEntity> rooms = meetingRepository.findStaticRoomsReadyForPasswordChange(dateLimit, Limit.of(chunkSize));
            return rooms.isEmpty() ? null : rooms;
        };
    }

    @Bean(name = "StaticRoomsPasswordChangeJob_Step1Writer")
    protected ItemWriter<List<MeetingEntity>> step1Writer()
    {
        return chunk -> {
            List<MeetingEntity> rooms = chunk.getItems().stream().flatMap(List::stream).collect(Collectors.toList());
            for (MeetingEntity r : rooms)
            {
                r.setPassword(MeetingService.generatePassword(passwordLength));
                r.setLastPasswordChange(ZonedDateTime.now());
                r.setPasswordChangeCandidate(false);
                r.setPasswordChangeDueDate(ZonedDateTime.now());
            }
            meetingRepository.saveAll(rooms);
            notificationService.createPasswordChangedNotifications(rooms);
        };
    }

    @Bean(name = "StaticRoomsPasswordChangeJob_Step2Reader")
    protected ItemReader<List<MeetingEntity>> step2Reader()
    {
        return () -> {
            ZonedDateTime dateBefore = ZonedDateTime.now().minusDays(daysBefore);
            List<MeetingEntity> rooms = meetingRepository.findStaticRoomsResetPasswordChangeCandidates(dateBefore, Limit.of(chunkSize));
            return rooms.isEmpty() ? null : rooms;
        };
    }

    @Bean(name = "StaticRoomsPasswordChangeJob_Step2Writer")
    protected ItemWriter<List<MeetingEntity>> step2Writer()
    {
        return chunk -> {
            List<MeetingEntity> rooms = chunk.getItems().stream().flatMap(List::stream).collect(Collectors.toList());
            for (MeetingEntity r : rooms)
            {
                r.setPasswordChangeCandidate(false);
                r.setPasswordChangeDueDate(null);
            }
            meetingRepository.saveAll(rooms);
        };
    }

    @Bean(name = "StaticRoomsPasswordChangeJob_Step3Reader")
    protected ItemReader<List<MeetingEntity>> step3Reader()
    {
        return () -> {
            ZonedDateTime dateBefore = ZonedDateTime.now().minusDays(daysBefore);
            List<MeetingEntity> rooms = meetingRepository.findStaticRoomsNewPasswordChangeCandidates(dateBefore, Limit.of(chunkSize));
            return rooms.isEmpty() ? null : rooms;
        };
    }

    @Bean(name = "StaticRoomsPasswordChangeJob_Step3Writer")
    protected ItemWriter<List<MeetingEntity>> step3Writer()
    {
        return chunk -> {
            List<MeetingEntity> rooms = chunk.getItems().stream().flatMap(List::stream).collect(Collectors.toList());
            for (MeetingEntity r : rooms)
            {
                r.setPasswordChangeCandidate(true);
                r.setPasswordChangeDueDate(ZonedDateTime.now().plusDays(daysBefore));
            }
            meetingRepository.saveAll(rooms);
            notificationService.createPasswordChangeCandidateNotifications(rooms);
        };
    }

}