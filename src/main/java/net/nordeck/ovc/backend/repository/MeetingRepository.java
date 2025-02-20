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

package net.nordeck.ovc.backend.repository;

import net.nordeck.ovc.backend.entity.MeetingEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, UUID> {

    List<MeetingEntity> findByParentIdOrderByEndTimeAsc(UUID parentId);

    List<MeetingEntity> findByParentIdAndExcludedFalseOrderByEndTimeAsc(UUID parentId);

    @Deprecated
    List<MeetingEntity> findByParentIdAndExcludedTrueOrderByEndTimeAsc(UUID parentId);

    List<MeetingEntity> findAllByStaticRoomIsTrueAndHasOrganizerIsFalse(Limit limit);



    @Query(value = "SELECT m FROM MeetingEntity m LEFT JOIN m.participants p WHERE " +
            "m.instantMeeting = false AND " +
            "m.staticRoom = false AND " +
            "m.excluded = false AND " +
            "((m.frequency = 'ONCE') OR (m.frequency != 'ONCE' AND m.parentId is not null)) AND " +
            "m.startTime >= :startDateTime AND " +
            "m.startTime <= :endDateTime AND " +
            "UPPER(p.email) = UPPER(:userId)")
    Page<MeetingEntity> findAllNormalMeetings(ZonedDateTime startDateTime, ZonedDateTime endDateTime, String userId,
                                              Pageable pageable);

    @Query(value = "SELECT m FROM MeetingEntity m LEFT JOIN m.participants p WHERE " +
            "m.instantMeeting = false AND " +
            "m.staticRoom = true AND " +
            "m.excluded = false AND " +
            "UPPER(p.email) = UPPER(:userId)")
    Page<MeetingEntity> findAllStaticRooms(String userId, Pageable pageable);

    @Query(value = "SELECT m FROM MeetingEntity m LEFT JOIN m.participants p WHERE " +
            "m.instantMeeting = true AND " +
            "m.staticRoom = false AND " +
            "m.excluded = false AND " +
            "UPPER(p.email) = UPPER(:userId)")
    Page<MeetingEntity> findAllInstantMeetings(String userId, Pageable pageable);

    @Transactional
    void deleteAllByParentId(UUID parentId);



    @Query(value = "SELECT m FROM MeetingEntity m WHERE " +
            "m.lastPasswordChange <= :dueDate AND " +
            "m.staticRoom = true")
    List<MeetingEntity> findStaticRoomsReadyForPasswordChange(ZonedDateTime dueDate, Limit limit);

    @Query(value = "SELECT m FROM MeetingEntity m WHERE " +
            "m.passwordChangeCandidate = true AND " +
            "m.lastPasswordChange > :beforeDate AND " +
            "m.staticRoom = true")
    List<MeetingEntity> findStaticRoomsResetPasswordChangeCandidates(ZonedDateTime beforeDate, Limit limit);

    @Query(value = "SELECT m FROM MeetingEntity m WHERE " +
            "m.lastPasswordChange < :beforeDate AND " +
            "m.passwordChangeCandidate = false AND " +
            "m.staticRoom = true")
    List<MeetingEntity> findStaticRoomsNewPasswordChangeCandidates(ZonedDateTime beforeDate, Limit limit);


    @Query(value = "SELECT m FROM MeetingEntity m WHERE " +
            "m.lastVisitDate <= :dueDate AND " +
            "m.staticRoom = true")
    List<MeetingEntity> findStaticRoomsReadyForDeletion(ZonedDateTime dueDate, Limit limit);

    @Query(value = "SELECT m FROM MeetingEntity m WHERE " +
            "m.deleteCandidate = true AND " +
            "m.lastVisitDate > :beforeDate AND " +
            "m.staticRoom = true")
    List<MeetingEntity> findStaticRoomsResetDeleteCandidates(ZonedDateTime beforeDate, Limit limit);

    @Query(value = "SELECT m FROM MeetingEntity m WHERE " +
            "m.lastVisitDate < :beforeDate AND " +
            "m.deleteCandidate = false AND " +
            "m.staticRoom = true")
    List<MeetingEntity> findStaticRoomsNewDeleteCandidates(ZonedDateTime beforeDate, Limit limit);




    List<MeetingEntity> findByConferencePin(String conferencePin);
    boolean existsByConferencePin(String conferencePin);

    List<MeetingEntity> findAllByEndTimeBeforeAndStaticRoomIsFalse(ZonedDateTime dateTimeBefore, Limit limit);

    List<MeetingEntity> findAllByParentId(UUID parentId);

    List<MeetingEntity> findAllByParentIdAndExcludedIsFalse(UUID parentId);

}