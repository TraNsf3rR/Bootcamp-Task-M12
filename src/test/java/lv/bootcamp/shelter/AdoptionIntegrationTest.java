package lv.bootcamp.shelter;

import jakarta.persistence.EntityManager;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.dto.AdoptionRequest;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AnimalRepository;
import lv.bootcamp.shelter.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest
@Transactional
class AdoptionIntegrationTest {

    @Autowired
    private AnimalService animalService;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private NotificationClient notificationClient;

    @Test
    void createThenAdopt_shouldPersistAnimalAndNotifyAdopter() {
        AnimalResponse created = animalService.create(
                new AnimalCreateRequest(
                        "Rex", AnimalType.DOG, "Labrador", 3, "Friendly", "integration-chip-123"));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(AnimalStatus.AVAILABLE);
        entityManager.flush();
        entityManager.clear();

        AnimalResponse adopted = animalService.adopt(
                new AdoptionRequest(created.id(), "Anna", "anna@example.com"));

        assertThat(adopted.id()).isEqualTo(created.id());
        assertThat(adopted.status()).isEqualTo(AnimalStatus.ADOPTED);
        entityManager.flush();
        entityManager.clear();

        var persistedAnimal = animalRepository.findById(created.id()).orElseThrow();
        assertThat(persistedAnimal.getStatus()).isEqualTo(AnimalStatus.ADOPTED);
        verify(notificationClient).sendAdoptionNotification(
                created.id(), "Rex", "anna@example.com");
        verifyNoMoreInteractions(notificationClient);
    }
}
