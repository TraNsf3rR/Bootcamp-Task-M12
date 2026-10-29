package lv.bootcamp.shelter.service;

import lv.bootcamp.shelter.audit.AuditLogger;
import lv.bootcamp.shelter.audit.RejectionReason;
import lv.bootcamp.shelter.client.NotificationClient;
import lv.bootcamp.shelter.model.Adopter;
import lv.bootcamp.shelter.model.AdoptionResult;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AdopterRepository;
import lv.bootcamp.shelter.repository.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdoptionEligibilityServiceTest {

    private static final Long ADOPTER_ID = 10L;
    private static final Long ANIMAL_ID = 20L;

    @Mock
    private AdopterRepository adopterRepository;

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private AdoptionEligibilityService adoptionEligibilityService;

    @Test
    void evaluateAdoption_shouldRejectWhenAdopterDoesNotExist() {
        when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.empty());

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertRejected(result, RejectionReasons.ADOPTER_NOT_FOUND);
        verifyNoInteractions(animalRepository, notificationClient, auditLogger);
    }

    @Test
    void evaluateAdoption_shouldRejectWhenAnimalDoesNotExist() {
        stubAdopter(newAdopter(30, 0, 0, false, false));
        when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.empty());

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertRejected(result, RejectionReasons.ANIMAL_NOT_FOUND);
        verifyNoInteractions(notificationClient, auditLogger);
    }

    @Test
    void evaluateAdoption_shouldRejectWhenAnimalIsNotAvailable() {
        stubAdopter(newAdopter(30, 0, 0, false, false));
        stubAnimal(newAnimal(AnimalType.DOG, AnimalStatus.RESERVED, 3));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertRejected(result, RejectionReasons.ANIMAL_NOT_AVAILABLE);
        verifyNoInteractions(notificationClient, auditLogger);
    }

    @Test
    void evaluateAdoption_shouldRejectAndAuditUnderageAdopter() {
        stubAdopter(newAdopter(17, 0, 0, false, false));
        stubAnimal(newAnimal(AnimalType.DOG, AnimalStatus.AVAILABLE, 3));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertRejected(result, RejectionReasons.UNDERAGE);
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.UNDERAGE);
        verifyNoMoreInteractions(auditLogger);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldApproveAtAgeAndPriorityBoundaries() {
        stubAdopter(newAdopter(18, 0, 3, false, false));
        stubAnimal(newAnimal(AnimalType.DOG, AnimalStatus.AVAILABLE, 7));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isEqualTo(10);
        verify(auditLogger).logApproval(ADOPTER_ID, ANIMAL_ID, 10);
        verify(notificationClient).sendApprovalNotification("anna@example.com", "Rex");
        verifyNoMoreInteractions(auditLogger, notificationClient);
    }

    @Test
    void evaluateAdoption_shouldAllowRegularAdopterBelowPetLimitAndClampNegativeScore() {
        stubAdopter(newAdopter(30, 2, 0, false, false));
        stubAnimal(newAnimal(AnimalType.DOG, AnimalStatus.AVAILABLE, null));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isZero();
        verify(auditLogger).logApproval(ADOPTER_ID, ANIMAL_ID, 0);
        verify(notificationClient).sendApprovalNotification("anna@example.com", "Rex");
        verifyNoMoreInteractions(auditLogger, notificationClient);
    }

    @Test
    void evaluateAdoption_shouldRejectRegularAdopterAtPetLimit() {
        stubAdopter(newAdopter(30, 3, 0, false, false));
        stubAnimal(newAnimal(AnimalType.DOG, AnimalStatus.AVAILABLE, 3));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertRejected(result, RejectionReasons.PET_LIMIT_REACHED);
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.PET_LIMIT_REACHED);
        verifyNoMoreInteractions(auditLogger);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldRejectLargePropertyAdopterAtPetLimit() {
        stubAdopter(newAdopter(30, 5, 0, true, false));
        stubAnimal(newAnimal(AnimalType.DOG, AnimalStatus.AVAILABLE, 3));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertRejected(result, RejectionReasons.PET_LIMIT_REACHED);
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.PET_LIMIT_REACHED);
        verifyNoMoreInteractions(auditLogger);
        verifyNoInteractions(notificationClient);
    }

    @Test
    void evaluateAdoption_shouldCalculatePriorityScoreForLargePropertyAndExperiencedAdopter() {
        stubAdopter(newAdopter(30, 4, 4, true, true));
        stubAnimal(newAnimal(AnimalType.RABBIT, AnimalStatus.AVAILABLE, 8));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertThat(result.approved()).isTrue();
        assertThat(result.priorityScore()).isEqualTo(42);
        verify(auditLogger).logApproval(ADOPTER_ID, ANIMAL_ID, 42);
        verify(notificationClient).sendApprovalNotification("anna@example.com", "Rex");
        verifyNoMoreInteractions(auditLogger, notificationClient);
    }

    @ParameterizedTest
    @EnumSource(value = AnimalType.class, names = {"BIRD", "RABBIT"})
    void evaluateAdoption_shouldRejectExoticAnimalWithoutPermit(AnimalType animalType) {
        stubAdopter(newAdopter(30, 0, 0, false, false));
        stubAnimal(newAnimal(animalType, AnimalStatus.AVAILABLE, 3));

        AdoptionResult result = adoptionEligibilityService.evaluateAdoption(ADOPTER_ID, ANIMAL_ID);

        assertRejected(result, RejectionReasons.EXOTIC_PERMIT_REQUIRED);
        verify(auditLogger).logRejection(ADOPTER_ID, ANIMAL_ID, RejectionReason.NO_EXOTIC_PERMIT);
        verifyNoMoreInteractions(auditLogger);
        verifyNoInteractions(notificationClient);
    }

    private void stubAdopter(Adopter adopter) {
        when(adopterRepository.findById(ADOPTER_ID)).thenReturn(Optional.of(adopter));
    }

    private void stubAnimal(Animal animal) {
        when(animalRepository.findById(ANIMAL_ID)).thenReturn(Optional.of(animal));
    }

    private Adopter newAdopter(
            int age, int currentPetCount, int previousAdoptions, boolean largeProperty, boolean exoticPermit) {
        Adopter adopter = new Adopter();
        adopter.setId(ADOPTER_ID);
        adopter.setName("Anna");
        adopter.setEmail("anna@example.com");
        adopter.setAge(age);
        adopter.setCurrentPetCount(currentPetCount);
        adopter.setPreviousAdoptions(previousAdoptions);
        adopter.setLargeProperty(largeProperty);
        adopter.setExoticPermit(exoticPermit);
        return adopter;
    }

    private Animal newAnimal(AnimalType type, AnimalStatus status, Integer age) {
        Animal animal = new Animal();
        animal.setId(ANIMAL_ID);
        animal.setName("Rex");
        animal.setType(type);
        animal.setStatus(status);
        animal.setAge(age);
        return animal;
    }

    private void assertRejected(AdoptionResult result, String reason) {
        assertThat(result.approved()).isFalse();
        assertThat(result.reason()).isEqualTo(reason);
        assertThat(result.priorityScore()).isZero();
    }
}
