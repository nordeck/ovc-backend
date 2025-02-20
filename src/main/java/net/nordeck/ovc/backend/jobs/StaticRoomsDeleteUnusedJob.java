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
@ConditionalOnProperty(name = "jobs.static-room-delete-unused.enabled")
public class StaticRoomsDeleteUnusedJob {

    @Autowired
    MeetingRepository meetingRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${jobs.static-room-delete-unused.daysLimit:90}")
    protected int daysLimit;

    @Value("${jobs.static-room-delete-unused.daysBefore:15}")
    protected int daysBefore;

    @Value("${jobs.static-room-delete-unused.chunkSize:200}")
    protected int chunkSize;

    @Scheduled(cron = "${jobs.static-room-delete-unused.cron:0 15 1 * * *}", zone = "Europe/Berlin")
    @SchedulerLock(name = "StaticRoomsDeleteUnusedJob")
    public void execute() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParameters jobParameters = new JobParametersBuilder()
                                        .addString("JobId", UUID.randomUUID().toString())
                                        .toJobParameters();
        jobLauncher.run(job(), jobParameters);
    }

    @Bean(name = "StaticRoomsDeleteUnusedJob")
    public Job job() {
        return new JobBuilder("StaticRoomsDeleteUnusedJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1())
                .on("*").to(step2())
                .on("*").to(step3())
                .end()
                .build();
    }

    /**
     * Step 1: Delete rooms with expired "last visit date"
     */
    @Bean(name = "StaticRoomsDeleteUnusedJob_Step1")
    protected Step step1() {
        return new StepBuilder("StaticRoomsDeleteUnusedJob_Step1", jobRepository)
                .<List<MeetingEntity>, List<MeetingEntity>> chunk(1, transactionManager)
                .reader(step1Reader())
                .writer(step1Writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * Step 2: reset the flag "delete candidate" for rooms with changed due date
     */
    @Bean(name = "StaticRoomsDeleteUnusedJob_Step2")
    protected Step step2() {
        return new StepBuilder("StaticRoomsDeleteUnusedJob_Step2", jobRepository)
                .<List<MeetingEntity>, List<MeetingEntity>> chunk(1, transactionManager)
                .reader(step2Reader())
                .writer(step2Writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    /**
     * Step 3: Find new delete candidates and set their flag
     */
    @Bean(name = "StaticRoomsDeleteUnusedJob_Step3")
    protected Step step3() {
        return new StepBuilder("StaticRoomsDeleteUnusedJob_Step3", jobRepository)
                .<List<MeetingEntity>, List<MeetingEntity>> chunk(1, transactionManager)
                .reader(step3Reader())
                .writer(step3Writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean(name = "StaticRoomsDeleteUnusedJob_Step1Reader")
    protected ItemReader<List<MeetingEntity>> step1Reader() {
        return () -> {
            ZonedDateTime limitDate = ZonedDateTime.now().minusDays(daysLimit);
            List<MeetingEntity> entities = meetingRepository.findStaticRoomsReadyForDeletion(limitDate, Limit.of(chunkSize));
            return entities.isEmpty() ? null : entities;
        };
    }

    @Bean(name = "StaticRoomsDeleteUnusedJob_Step1Writer")
    protected ItemWriter<List<MeetingEntity>> step1Writer() {
        return chunk -> {
            List rooms = chunk.getItems().stream().flatMap(List::stream).collect(Collectors.toList());
            meetingRepository.deleteAll(rooms);
        };
    }

    @Bean(name = "StaticRoomsDeleteUnusedJob_Step2Reader")
    protected ItemReader<List<MeetingEntity>> step2Reader() {
        return () -> {
            ZonedDateTime dateToCompare = ZonedDateTime.now().minusDays(daysBefore);
            List<MeetingEntity> entities = meetingRepository.findStaticRoomsResetDeleteCandidates(dateToCompare, Limit.of(chunkSize));
            return entities.isEmpty() ? null : entities;
        };
    }

    @Bean(name = "StaticRoomsDeleteUnusedJob_Step2Writer")
    protected ItemWriter<List<MeetingEntity>> step2Writer() {
        return chunk -> {
            List<MeetingEntity> rooms = chunk.getItems().stream().flatMap(List::stream).collect(Collectors.toList());
            for (MeetingEntity room : rooms) {
                room.setDeleteCandidate(false);
                room.setRoomDeletionDueDate(null);
            }
            meetingRepository.saveAll(rooms);
        };
    }

    @Bean(name = "StaticRoomsDeleteUnusedJob_Step3Reader")
    protected ItemReader<List<MeetingEntity>> step3Reader() {
        return () -> {
            ZonedDateTime dateToCompare = ZonedDateTime.now().minusDays(daysBefore);
            List<MeetingEntity> entities = meetingRepository.findStaticRoomsNewDeleteCandidates(dateToCompare, Limit.of(chunkSize));
            return entities.isEmpty() ? null : entities;
        };
    }

    @Bean(name = "StaticRoomsDeleteUnusedJob_Step3Writer")
    protected ItemWriter<List<MeetingEntity>> step3Writer() {
        return chunk -> {
            List<MeetingEntity> rooms = chunk.getItems().stream().flatMap(List::stream).collect(Collectors.toList());
            for (MeetingEntity room : rooms) {
                room.setDeleteCandidate(true);
                room.setRoomDeletionDueDate(ZonedDateTime.now().plusDays(daysBefore));
            }
            meetingRepository.saveAll(rooms);
            notificationService.createDeleteCandidateNotifications(rooms);
        };
    }
}