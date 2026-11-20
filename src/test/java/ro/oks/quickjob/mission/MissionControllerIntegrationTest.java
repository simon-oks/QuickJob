package ro.oks.quickjob.mission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.oks.quickjob.BaseIntegrationTest;
import ro.oks.quickjob.auth.JwtService;
import ro.oks.quickjob.mission.dto.CreateMissionRequest;
import ro.oks.quickjob.user.User;
import ro.oks.quickjob.user.UserRepository;

@AutoConfigureMockMvc
class MissionControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    MissionRepository missionRepository;

    private String clientToken;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        missionRepository.deleteAll();
        userRepository.deleteAll();

        clientId = UUID.randomUUID();
        User client = new User(clientId, "+22890000001", "Test Client");
        client.setPhoneVerified(true);
        userRepository.save(client);

        clientToken = jwtService.generateAccessToken(clientId, "CLIENT,WORKER");
    }

    @Test
    void create_mission_returns_201() throws Exception {
        var request = new CreateMissionRequest(
                MissionCategory.DELIVERY,
                "Livraison colis",
                "Livrer un colis au marché",
                6.1319, 1.2228,
                "Grand Marché, Lomé",
                OffsetDateTime.now().plusHours(1),
                60,
                1500L,
                "XOF",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/missions")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.title").value("Livraison colis"))
                .andExpect(jsonPath("$.category").value("DELIVERY"));
    }

    @Test
    void create_mission_without_auth_returns_401() throws Exception {
        var request = new CreateMissionRequest(
                MissionCategory.DELIVERY,
                "Livraison colis",
                "Livrer un colis",
                6.1319, 1.2228,
                "Grand Marché, Lomé",
                OffsetDateTime.now().plusHours(1),
                60,
                1500L,
                "XOF",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_mission_with_invalid_duration_returns_400() throws Exception {
        var request = new CreateMissionRequest(
                MissionCategory.DELIVERY,
                "Livraison colis",
                "Description",
                6.1319, 1.2228,
                "Grand Marché, Lomé",
                OffsetDateTime.now().plusHours(1),
                30,
                1500L,
                "XOF",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/missions")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSION_INVALID_DURATION"));
    }

    @Test
    void find_nearby_missions() throws Exception {
        // Crée une mission d'abord
        var request = new CreateMissionRequest(
                MissionCategory.CLEANING,
                "Nettoyage bureau",
                "Nettoyage complet",
                6.1319, 1.2228,
                "Grand Marché, Lomé",
                OffsetDateTime.now().plusHours(2),
                90,
                2000L,
                "XOF",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/missions")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Recherche à proximité
        mockMvc.perform(get("/api/v1/missions/nearby")
                        .header("Authorization", "Bearer " + clientToken)
                        .param("lat", "6.1300")
                        .param("lon", "1.2200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Nettoyage bureau"))
                .andExpect(jsonPath("$.content[0].latitude").doesNotExist())
                .andExpect(jsonPath("$.content[0].longitude").doesNotExist());
    }

    @Test
    void find_nearby_missions_outside_radius_returns_empty() throws Exception {
        var request = new CreateMissionRequest(
                MissionCategory.DELIVERY,
                "Mission loin",
                "Loin",
                6.1319, 1.2228,
                "Grand Marché, Lomé",
                OffsetDateTime.now().plusHours(1),
                60,
                1500L,
                "XOF",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/missions")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Recherche depuis Accra (~180 km de Lomé), rayon 1 km
        mockMvc.perform(get("/api/v1/missions/nearby")
                        .header("Authorization", "Bearer " + clientToken)
                        .param("lat", "5.6037")
                        .param("lon", "-0.1870")
                        .param("radius", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }
}
