package net.nordeck.ovc.backend.service;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.nordeck.ovc.backend.Constants;
import net.nordeck.ovc.backend.dto.DTOUtils;
import net.nordeck.ovc.backend.dto.MeetingDTO;
import net.nordeck.ovc.backend.dto.MeetingParticipantDTO;
import net.nordeck.ovc.backend.dto.MeetingType;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static net.nordeck.ovc.backend.Constants.APP_ISSUER;
import static net.nordeck.ovc.backend.Constants.ERR_WRONG_MEETING_PASSWORD;

@Service
@Setter
@Getter
@NoArgsConstructor
public class JitsiServiceImpl implements JitsiService
{

    public static final String TOKEN_ALGORITHM = "HS256";
    public static final String NOT_DEFINED_YET = "# not defined yet #";
    public static final String CONFIG_SUBJECT = "config.localSubject";
    public static final String QUOTATION = "\"";
    public static final String INSTANT_MEETING = "Sofortmeeting";
    public static final String NULL = "null";

    @Value("${jitsi.jwt.secret}")
    private String secret;

    @Value("${jitsi.jwt.expiration-in-minutes}")
    private int expirationInMinutes;

    @Value("${jitsi.jwt.expiration-for-rooms-in-minutes}")
    private int expirationForRoomsInMinutes;

    @Value("${jitsi.jwt.not-before-in-minutes}")
    private int notBeforeInMinutes;

    @Value("${jitsi.domain}")
    private String domain = "https://jitsi.nordeck.net";

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String generateToken(MeetingParticipantDTO participant, String userEmail,
                                String displayName, UUID meetingId, String timeZone)
    {
        MeetingDTO meeting = findMeeting(meetingId);
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        String signingKey = Base64.getEncoder().encodeToString(secretBytes);
        Date issuedAt = new Date();
        Date notBefore = null;
        Date expirationAt;

        if (MeetingType.NORMAL.equals(meeting.getType()))
        {
            ZonedDateTime startTime = meeting.getStartTime().minusMinutes(notBeforeInMinutes);
            ZonedDateTime endTime = meeting.getEndTime().plusMinutes(expirationInMinutes);
            notBefore = Date.from(startTime.toInstant());
            expirationAt = Date.from(endTime.toInstant());

            if (!StringUtils.isEmpty(timeZone))
            {
                notBefore = Date.from(startTime.withZoneSameInstant(ZoneId.of(timeZone)).toInstant());
                expirationAt = Date.from(endTime.withZoneSameInstant(ZoneId.of(timeZone)).toInstant());
            }
        }
        else
        {
            expirationAt = Date.from(ZonedDateTime.now().plusDays(1).toInstant());
        }

        CustomClaim customClaim = new CustomClaim();
        Context context = new Context();
        customClaim.context = context;
        context.user = new User();
        context.user.name = displayName;
        context.user.email = userEmail;
        context.user.id = userEmail;
        context.room = new Room();
        context.room.lobby = false;

        String affiliation = Constants.MEMBER;
        boolean lobbyBypass = false;

        if (participant != null)
        {
            MeetingDTO meetingDTO = findMeeting(participant.getMeetingId());
            context.room.lobby = meetingDTO.isLobbyEnabled();

            switch (participant.getRole())
            {
                case MODERATOR, ORGANIZER -> {
                    affiliation = Constants.OWNER;
                    lobbyBypass = true;
                }
                case GUEST -> affiliation = Constants.MEMBER;
            }
        }
        context.user.affiliation = affiliation;
        context.user.lobby_bypass = lobbyBypass;

        if (StringUtils.isBlank(context.user.name) || NULL.equals(context.user.name))
        {
            int randomInt = Math.abs(new Random().nextInt(100000));
            context.user.name = Constants.GAST + " " + randomInt;
        }

        Map contextData = objectMapper.convertValue(customClaim, Map.class);

        JwtBuilder jwtBuilder = Jwts.builder()
                .claim("aud", APP_ISSUER)
                .claim("iss", APP_ISSUER)
                .claim("sub", "*")
                .claim("room", meetingId)
                .claims(contextData)
                .issuedAt(issuedAt)
                .setHeaderParam("typ", Header.JWT_TYPE)
                .setHeaderParam("alg", TOKEN_ALGORITHM)
                .expiration(expirationAt)
                .signWith(SignatureAlgorithm.HS256, signingKey);

        if (MeetingType.NORMAL.equals(meeting.getType()))
        {
            jwtBuilder = jwtBuilder.claim("nbf", notBefore);
        }
        return jwtBuilder.compact();
    }

    @Override
    public String generateLink(UUID meetingId, String password, String userEmail, String displayName, String timeZone)
    {
        String link = domain + "/" + meetingId;
        MeetingDTO meeting = findMeeting(meetingId);

        if (!meeting.getPassword().equals(password))
        {
            throw new AccessDeniedException(ERR_WRONG_MEETING_PASSWORD);
        }

        MeetingParticipantDTO participant = findParticipant(meeting, userEmail);
        String token;
        if (participant != null)
        {
            token = generateToken(participant, userEmail, displayName, meetingId, timeZone);
        }
        else
        {
            token = generateToken(null, userEmail, displayName, meetingId, timeZone);
        }

        link += "?jwt=" + token + "#" + CONFIG_SUBJECT + "=" + getRoomName(meeting);
        return link;
    }

    private String getRoomName(MeetingDTO meeting)
    {
        String roomName = meeting.getName();
        if (MeetingType.INSTANT.equals(meeting.getType()))
        {
            roomName = INSTANT_MEETING + " - " + meeting.getOwnerId();
        }
        roomName = UriUtils.encode(QUOTATION + roomName + QUOTATION, StandardCharsets.UTF_8);
        return roomName;
    }

    protected MeetingParticipantDTO findParticipant(MeetingDTO meeting, String userEmail)
    {
        List<MeetingParticipantDTO> participants = meeting.getParticipants()
                .stream()
                .filter(p -> p.getEmail().equalsIgnoreCase(userEmail))
                .toList();
        if (participants.isEmpty())
            return null;
        else
            return participants.get(0);
    }

    protected MeetingDTO findMeeting(UUID meetingId)
    {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElseThrow(
                () -> new EntityNotFoundException(String.format(Constants.NO_MEETING_FOUND_FOR_ID, meetingId))
        );
        return DTOUtils.buildFromEntity(meeting);
    }

    @Getter
    public static class CustomClaim
    {
        Context context;
    }

    @Getter
    public static class Context
    {
        User user;
        Room room;
    }

    @Getter
    public static class User
    {
        String avatar;
        String name;
        String email;
        String id;
        String affiliation;
        boolean lobby_bypass;
    }

    @Getter
    public static class Room
    {
        String password;
        boolean lobby;
    }

}