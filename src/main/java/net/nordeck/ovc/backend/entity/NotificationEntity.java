package net.nordeck.ovc.backend.entity;

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

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "notification")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id")
    @Size(max = 255)
    private String userId;

    @Column(name = "message")
    @Size(max = 1024)
    private String message;

    @Column(name = "type")
    @Size(max = 64)
    private String type;

    @Column(name = "viewed")
    private boolean viewed;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "viewed_at")
    private ZonedDateTime viewedAt;

    @Column(name = "meeting_id")
    private UUID meetingId;

    @Column(name = "room_name")
    private String roomName;

    @Column(name = "room_deletion_due_date")
    private ZonedDateTime roomDeletionDueDate;

    @Column(name = "password_change_due_date")
    private ZonedDateTime passwordChangeDueDate;

}