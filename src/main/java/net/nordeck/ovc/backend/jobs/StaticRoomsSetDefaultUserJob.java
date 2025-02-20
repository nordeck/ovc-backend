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

import jakarta.annotation.PostConstruct;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.nordeck.ovc.backend.dto.Role;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.entity.MeetingParticipantEntity;
import net.nordeck.ovc.backend.repository.MeetingParticipantRepository;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import net.nordeck.ovc.backend.service.KeycloakClientService;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(name = "jobs.static-room-default-user.enabled")
public class StaticRoomsSetDefaultUserJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticRoomsSetDefaultUserJob.class);

    private static final String ORGANIZER = Role.ORGANIZER.getValue();

    protected static final String STEP_NAME = "StaticRoomsSetDefaultUserJob_Step";

    protected static final String JOB_NAME = "StaticRoomsSetDefaultUserJob";

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    protected KeycloakClientService keycloakService;

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @Value("${jobs.static-room-default-user.chunkSize:200}")
    protected int chunkSize;

    @Value("${jobs.static-room-default-user.email:#{null}}")
    protected String defaultUserEmail;

    private UserRepresentation defaultUser;

    @PostConstruct
    public void init() {
        try {
            defaultUser = keycloakService.searchByEmail(defaultUserEmail);
        }
        catch(Exception e) {
            LOGGER.error("Could not properly initialize StaticRoomsSetDefaultUserJob job. " +
                    "Keycloak client instance has not been created. " + e.getMessage());
        }
    }

    @Scheduled(cron = "${jobs.static-room-default-user.cron:0 0 */6 * * *}", zone = "Europe/Berlin")
    @SchedulerLock(name = JOB_NAME)
    public void execute() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("JobId", UUID.randomUUID().toString())
                .toJobParameters();

        jobLauncher.run(job(), jobParameters);
    }

    @Bean(name = JOB_NAME)
    public Job job() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step())
                .build();
    }

    @Bean(name = STEP_NAME)
    protected Step step() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<List<MeetingEntity>, List<MeetingEntity>> chunk(1, transactionManager)
                .reader(reader())
                .writer(writer())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }

    @Bean(name = "StaticRoomsSetDefaultUserJob_StepReader")
    protected ItemReader<List<MeetingEntity>> reader() {
        return () -> {
            List<MeetingEntity> rooms = meetingRepository.findAllByStaticRoomIsTrueAndHasOrganizerIsFalse(Limit.of(chunkSize));
            return rooms.isEmpty() ? null : rooms;
        };
    }

    @Bean(name = "StaticRoomsSetDefaultUserJob_StepWriter")
    protected ItemWriter<List<MeetingEntity>> writer() {
        return items -> {
            List<MeetingParticipantEntity> participants = new ArrayList<>();
            List<MeetingEntity> entities = items.getItems().stream().flatMap(List::stream).collect(Collectors.toList());

            for(MeetingEntity meeting : entities) {
                MeetingParticipantEntity participant = new MeetingParticipantEntity();
                participant.setMeetingId(meeting.getId());
                participant.setRole(ORGANIZER);
                participant.setEmail(defaultUser == null ? defaultUserEmail : defaultUser.getEmail());
                participant.setUserId(defaultUser == null ? defaultUserEmail : defaultUser.getUsername());
                participants.add(participant);
                meeting.setHasOrganizer(true);
            }
            participantRepository.saveAll(participants);
            meetingRepository.saveAll(entities);
        };
    }
}
