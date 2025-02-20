package net.nordeck.ovc.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.nordeck.ovc.backend.dto.MeetingParticipantDTO;
import net.nordeck.ovc.backend.dto.Role;
import net.nordeck.ovc.backend.TestUtils;
import net.nordeck.ovc.backend.entity.MeetingEntity;
import net.nordeck.ovc.backend.repository.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.nordeck.ovc.backend.Constants.ERR_WRONG_MEETING_PASSWORD;
import static net.nordeck.ovc.backend.service.JitsiServiceImpl.INSTANT_MEETING;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles(value = "test")
public class JitsiServiceImplTest
{

    private static final String JWT_SECRET = "oeRaYY7Wo24sDqKSX3IM9ASGmdGPmkTd9jo1QTy4b7P9Ze5";
    private static final String QUOTATION = "\"";

    @Mock
    MeetingRepository meetingRepository;

    @InjectMocks
    JitsiServiceImpl service;

    @BeforeEach
    void setUp()
    {
        meetingRepository = Mockito.mock(MeetingRepository.class);
        service = new JitsiServiceImpl();
        service.setMeetingRepository(meetingRepository);
        service.setDomain("https://jitsi.nordeck.net");
        service.setSecret(JWT_SECRET);
        service.setObjectMapper(new ObjectMapper());

        String zoneId = ZoneId.systemDefault().getId();
        if ("America/Sao_Paulo".equals(zoneId))
        {
            service.setExpirationInMinutes(300);
            service.setExpirationForRoomsInMinutes(600);
            service.setNotBeforeInMinutes(270);
        }
        else if ("Europe/Minsk".equals(zoneId))
        {
            service.setExpirationInMinutes(180);
            service.setExpirationForRoomsInMinutes(360);
            service.setNotBeforeInMinutes(200);
        }
        else if ("Europe/Kiev".equals(zoneId))
        {
            service.setExpirationInMinutes(120);
            service.setExpirationForRoomsInMinutes(300);
            service.setNotBeforeInMinutes(150);
        }
        else
        {
            service.setExpirationInMinutes(185);
            service.setExpirationForRoomsInMinutes(180);
            service.setNotBeforeInMinutes(125);
        }
    }

    @Test
    void testGenerateTokenForModerator()
    {
        MeetingEntity meetingEntity = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(meetingEntity.getId())).thenReturn(Optional.of(meetingEntity));
        MeetingParticipantDTO participant = TestUtils.getMeetingParticipantDTO();

        String token = service.generateToken(participant, null, "moderator name", meetingEntity.getId(), null);

        byte[] secretBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        String signingKey = Base64.getEncoder().encodeToString(secretBytes);
        Jws<Claims> jwt = Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);

        Map<String, Map> context = (Map<String, Map>) jwt.getPayload().get("context");
        Map<String, Object> user = context.get("user");
        Map<String, Object> room = context.get("room");

        assertAll(
                () -> assertTrue(jwt.getPayload().getAudience().contains("ovc-backend")),
                () -> assertTrue(jwt.getPayload().getIssuer().contains("ovc-backend")),
                () -> assertEquals(meetingEntity.getId().toString(), jwt.getPayload().get("room")),
                () -> assertEquals("*", jwt.getPayload().get("sub")),
                () -> assertEquals("owner", user.get("affiliation")),
                () -> assertEquals(true, user.get("lobby_bypass")),
                () -> assertEquals("moderator name", user.get("name")),
                () -> assertEquals(true, room.get("lobby"))
        );
    }

    @Test
    void testGenerateTokenForGuest()
    {
        MeetingEntity meetingEntity = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(any())).thenReturn(Optional.of(meetingEntity));
        MeetingParticipantDTO participant = TestUtils.getMeetingParticipantDTO();
        participant.setRole(Role.GUEST);

        String token = service.generateToken(participant, null, "guest name", meetingEntity.getId(), null);

        byte[] secretBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        String signingKey = Base64.getEncoder().encodeToString(secretBytes);
        Jws<Claims> jwt = Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);

        Map<String, Map> context = (Map<String, Map>) jwt.getPayload().get("context");
        Map<String, Object> user = context.get("user");
        Map<String, Object> room = context.get("room");

        assertAll(
                () -> assertEquals("member", user.get("affiliation")),
                () -> assertTrue(user.get("lobby_bypass").equals(false)),
                () -> assertEquals("guest name", user.get("name")),
                () -> assertTrue(room.get("lobby").equals(true))
        );
    }

    @Test
    void testGenerateTokenForAnonymous()
    {
        MeetingEntity meetingEntity = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(any())).thenReturn(Optional.of(meetingEntity));
        String token = service.generateToken(null, null, "anonymous", meetingEntity.getId(), null);

        byte[] secretBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        String signingKey = Base64.getEncoder().encodeToString(secretBytes);
        Jws<Claims> jwt = Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);

        Map<String, Map> context = (Map<String, Map>) jwt.getPayload().get("context");
        Map<String, Object> user = context.get("user");
        Map<String, Object> room = context.get("room");

        assertAll(
                () -> assertEquals("member", user.get("affiliation")),
                () -> assertEquals(false, user.get("lobby_bypass")),
                () -> assertTrue(user.get("name").toString().contains("anonymous")),
                () -> assertEquals(false, room.get("lobby"))
        );
    }

    @Test
    void testGetters()
    {
        assertEquals("https://jitsi.nordeck.net", service.getDomain());
        assertEquals(JWT_SECRET, service.getSecret());
        assertNotNull(service.getExpirationInMinutes());
        assertNotNull(service.getExpirationForRoomsInMinutes());
        assertNotNull(service.getNotBeforeInMinutes());
        assertNotNull(service.getMeetingRepository());
        assertNotNull(service.getObjectMapper());
    }

    @Test
    void testGenerateLink()
    {
        MeetingEntity meetingEntity = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(TestUtils.meetingId)).thenReturn(Optional.of(meetingEntity));
        String link = service.generateLink(TestUtils.meetingId, meetingEntity.getPassword(),
                                           TestUtils.OWNER_EMAIL, "DISPLAY_NAME", null);
        String encodeRoomName = UriUtils.encode(QUOTATION + meetingEntity.getName() +
                                                        QUOTATION, StandardCharsets.UTF_8);
        assertTrue(link.contains(service.getDomain() + "/" + meetingEntity.getId() +
                                         "?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"));
        assertTrue(link.contains("#config.localSubject=" + encodeRoomName));
    }

    @Test
    void testGenerateLinkForGuest()
    {
        MeetingEntity meetingEntity = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(TestUtils.meetingId)).thenReturn(Optional.of(meetingEntity));
        meetingEntity.setParticipants(List.of());
        String link = service.generateLink(TestUtils.meetingId, meetingEntity.getPassword(),
                                           TestUtils.OWNER_EMAIL, "DISPLAY_NAME", null);
        String encodeRoomName = UriUtils.encode(QUOTATION + TestUtils.getMeetingDTO().getName() +
                                                        QUOTATION, StandardCharsets.UTF_8);
        assertTrue(link.contains(service.getDomain() + "/" + TestUtils.getMeetingDTO().getId() +
                                         "?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"));
        assertTrue(link.contains("#config.localSubject=" + encodeRoomName));
    }

    @Test
    void testGenerateLinkWithoutParticipant()
    {
        MeetingEntity meetingEntity = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(TestUtils.meetingId)).thenReturn(Optional.of(meetingEntity));
        String link = service.generateLink(TestUtils.meetingId, meetingEntity.getPassword(),
                                           TestUtils.OWNER_EMAIL, "DISPLAY_NAME", null);

        String encodeRoomName = UriUtils.encode(QUOTATION + meetingEntity.getName() +
                                                        QUOTATION, StandardCharsets.UTF_8);

        assertTrue(link.contains(service.getDomain() + "/" + meetingEntity.getId() +
                                         "?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"));
        assertTrue(link.contains("#config.localSubject=" + encodeRoomName));
    }

    @Test
    void testGenerateLinkForInstantMeeting()
    {
        MeetingEntity meetingEntity = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(TestUtils.meetingId)).thenReturn(Optional.of(meetingEntity));
        meetingEntity.setInstantMeeting(true);

        String link = service.generateLink(TestUtils.meetingId, meetingEntity.getPassword(),
                                           TestUtils.OWNER_EMAIL, "DISPLAY_NAME", null);
        String encodeRoomName = UriUtils.encode(QUOTATION + INSTANT_MEETING + " - " +
                                                        TestUtils.OWNER_EMAIL + QUOTATION, StandardCharsets.UTF_8);

        assertTrue(link.contains(service.getDomain() + "/" + TestUtils.getMeetingDTO().getId() +
                                         "?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"));
        assertTrue(link.contains("#config.localSubject=" + encodeRoomName));
    }

    @Test
    void testGenerateLinkWithWrongPassword()
    {
        MeetingEntity meetingEntity = TestUtils.getMeetingEntity();
        when(meetingRepository.findById(TestUtils.meetingId)).thenReturn(Optional.of(meetingEntity));

        Exception ex = assertThrows(AccessDeniedException.class,
                                    () -> service.generateLink(TestUtils.meetingId, "WRONG_PASSWORD",
                                                  TestUtils.OWNER_EMAIL, "DISPLAY_NAME", null));
        assertTrue(ex.getMessage().contains(ERR_WRONG_MEETING_PASSWORD));
    }
}