package lv.bootcamp.shelter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lv.bootcamp.shelter.config.SecurityConfig;
import lv.bootcamp.shelter.dto.AdopterCreateRequest;
import lv.bootcamp.shelter.dto.AdopterResponse;
import lv.bootcamp.shelter.service.AdopterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Reference example: fully implemented MockMvc tests for AdopterController.
 * Mirror this pattern for AnimalControllerTest (ANIMAL-2 in the README).
 * <p>
 * @Import(SecurityConfig.class) is required here too: without it, this slice
 * falls back to Spring Boot's default "require auth for everything" security
 * instead of this project's actual (permissive, for these endpoints) rules.
 */
@WebMvcTest(AdopterController.class)
@Import(SecurityConfig.class)
class AdopterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdopterService adopterService;

    @Test
    void register_shouldReturn201WithCreatedAdopter() throws Exception {
        when(adopterService.register(any()))
                .thenReturn(new AdopterResponse(1L, "Anna", "anna@example.com", 30));

        mockMvc.perform(post("/api/adopters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdopterCreateRequest("Anna", "anna@example.com", 30))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Anna"));
    }

    @Test
    void register_shouldReturn400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/adopters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdopterCreateRequest("Anna", "not-an-email", 30))))
                .andExpect(status().isBadRequest());
    }
}
