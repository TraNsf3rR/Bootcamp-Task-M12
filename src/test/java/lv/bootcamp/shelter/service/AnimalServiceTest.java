package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.dto.AdoptionRequest;
import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ticket: ANIMAL-1 (see README).
 * Three tests are already implemented as a worked example (also see
 * AdopterServiceTest for the same pattern) — add the remaining tests yourself.
 */
@ExtendWith(MockitoExtension.class)
class AnimalServiceTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @Captor
    private ArgumentCaptor<List<Long>> reservedIdsCaptor;

    @InjectMocks
    private AnimalService animalService;

    @Test
    void create_shouldSaveAnimalWithAvailableStatus() {
        Animal saved = new Animal();
        saved.setId(1L);
        saved.setName("Rex");
        saved.setType(AnimalType.DOG);
        saved.setStatus(AnimalStatus.AVAILABLE);

        when(animalRepository.save(any(Animal.class))).thenReturn(saved);

        AnimalResponse response = animalService.create(
                new AnimalCreateRequest("Rex", AnimalType.DOG, "Labrador", 3, "Friendly", null));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Rex");
        assertThat(response.status()).isEqualTo(AnimalStatus.AVAILABLE);
    }

    @Test
    void findById_shouldThrowWhenAnimalNotFound() {
        when(animalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animalService.findById(99L))
                .isInstanceOf(AnimalNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void adopt_shouldThrowWhenAnimalAlreadyAdopted() {
        Animal adopted = new Animal();
        adopted.setId(1L);
        adopted.setStatus(AnimalStatus.ADOPTED);

        when(animalRepository.findById(1L)).thenReturn(Optional.of(adopted));

        assertThatThrownBy(() -> animalService.adopt(
                new AdoptionRequest(1L, "Anna", "anna@example.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adopt_shouldMarkAnimalAdoptedAndSendNotification() {
        Animal animal = new Animal();
        animal.setId(42L);
        animal.setName("Rex");
        animal.setType(AnimalType.DOG);
        animal.setBreed("Labrador");
        animal.setAge(3);
        animal.setDescription("Friendly");
        animal.setStatus(AnimalStatus.AVAILABLE);
        when(animalRepository.findById(42L)).thenReturn(Optional.of(animal));
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AnimalResponse response = animalService.adopt(
                new AdoptionRequest(42L, "Anna", "anna@example.com"));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("Rex");
        assertThat(response.status()).isEqualTo(AnimalStatus.ADOPTED);
        verify(animalRepository).save(animal);
        verify(notificationClient).sendAdoptionNotification(42L, "Rex", "anna@example.com");
    }

    @Test
    void reserveMultiple_shouldNotifyExactlyReservedAnimalIds() {
        Animal firstAnimal = new Animal();
        firstAnimal.setId(1L);
        firstAnimal.setName("Rex");
        firstAnimal.setType(AnimalType.DOG);
        firstAnimal.setStatus(AnimalStatus.AVAILABLE);

        Animal secondAnimal = new Animal();
        secondAnimal.setId(2L);
        secondAnimal.setName("Mittens");
        secondAnimal.setType(AnimalType.CAT);
        secondAnimal.setStatus(AnimalStatus.AVAILABLE);

        when(animalRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(firstAnimal, secondAnimal));
        when(animalRepository.save(any(Animal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<AnimalResponse> responses = animalService.reserveMultiple(List.of(1L, 2L));

        assertThat(responses).hasSize(2);
        assertThat(firstAnimal.getStatus()).isEqualTo(AnimalStatus.RESERVED);
        assertThat(secondAnimal.getStatus()).isEqualTo(AnimalStatus.RESERVED);
        verify(notificationClient).sendBulkStatusNotification(reservedIdsCaptor.capture(), eq("RESERVED"));
        assertThat(reservedIdsCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void create_shouldRejectDuplicateMicrochipWithoutSaving() {
        String microchipId = "chip-123";
        when(animalRepository.existsByMicrochipId(microchipId)).thenReturn(true);

        assertThatThrownBy(() -> animalService.create(
                new AnimalCreateRequest("Rex", AnimalType.DOG, "Labrador", 3, "Friendly", microchipId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(microchipId);

        verify(animalRepository).existsByMicrochipId(microchipId);
        verify(animalRepository, never()).save(any(Animal.class));
    }
}
