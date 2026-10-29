package lv.bootcamp.shelter.controller;

import lv.bootcamp.shelter.config.SecurityConfig;
import lv.bootcamp.shelter.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Already implemented — read this as the reference pattern for testing
 * secured endpoints with MockMvc (not one of your tickets).
 * <p>
 * DELETE /api/animals/{id} is restricted to ROLE_ADMIN (see SecurityConfig).
 * Use @WithMockUser to fake an authenticated principal for a test method;
 * omit it to simulate an anonymous (unauthenticated) request.
 * <p>
 * @WebMvcTest does not load SecurityConfig automatically (it only scans
 * controllers/filters, not regular @Configuration classes) — that's why it's
 * pulled in explicitly with @Import below.
 */
@WebMvcTest(AnimalController.class)
@Import(SecurityConfig.class)
class AnimalControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDeleteAnimal() throws Exception {
        doNothing().when(animalService).delete(1L);

        mockMvc.perform(delete("/api/animals/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotDeleteAnimal() throws Exception {
        mockMvc.perform(delete("/api/animals/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(delete("/api/animals/1"))
                .andExpect(status().isUnauthorized());
    }
}
