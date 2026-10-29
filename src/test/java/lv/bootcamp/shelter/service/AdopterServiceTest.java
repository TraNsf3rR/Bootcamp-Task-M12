package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.dto.AdopterCreateRequest;
import lv.bootcamp.shelter.dto.AdopterResponse;
import lv.bootcamp.shelter.model.Adopter;
import lv.bootcamp.shelter.repository.AdopterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Reference example: fully implemented Mockito unit tests for AdopterService.
 * Mirror this pattern for AnimalServiceTest (ANIMAL-1 in the README).
 */
@ExtendWith(MockitoExtension.class)
class AdopterServiceTest {

    @Mock
    private AdopterRepository adopterRepository;

    @InjectMocks
    private AdopterService adopterService;

    @Test
    void register_shouldSaveAndReturnAdopter() {
        Adopter saved = new Adopter();
        saved.setId(1L);
        saved.setName("Anna");
        saved.setEmail("anna@example.com");
        saved.setAge(30);

        when(adopterRepository.save(any(Adopter.class))).thenReturn(saved);

        AdopterResponse response = adopterService.register(
                new AdopterCreateRequest("Anna", "anna@example.com", 30));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Anna");
        assertThat(response.email()).isEqualTo("anna@example.com");
    }

    @Test
    void findById_shouldThrowWhenAdopterNotFound() {
        when(adopterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adopterService.findById(99L))
                .isInstanceOf(AdopterNotFoundException.class)
                .hasMessageContaining("99");
    }
}
