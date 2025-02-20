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

import net.nordeck.ovc.backend.entity.NotificationEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID>
{

    Page<NotificationEntity> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @Transactional
    void deleteAllByMeetingId(UUID meetingId);

    @Transactional
    void deleteAllByUserId(String userId);

    List<NotificationEntity> findAllByUserId(String userId);

    List<NotificationEntity> findAllByCreatedAtBefore(ZonedDateTime limitDateTime, Limit limit);

}
