package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.AdoptionRecord;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AdoptionRecordRepositoryTest {

    @Autowired
    private AdoptionRecordRepository adoptionRecordRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findRecentAdoptionsByAnimalType_shouldFilterByTypeAndDate() {
        LocalDateTime since = LocalDateTime.of(2025, 1, 1, 12, 0);
        Animal dog = newAnimal("Rex", AnimalType.DOG);
        Animal cat = newAnimal("Mittens", AnimalType.CAT);
        entityManager.persist(dog);
        entityManager.persist(cat);
        entityManager.flush();

        entityManager.persist(newAdoptionRecord(dog, "recent-dog@example.com", since.plusDays(1)));
        entityManager.persist(newAdoptionRecord(dog, "cutoff-dog@example.com", since));
        entityManager.persist(newAdoptionRecord(dog, "old-dog@example.com", since.minusDays(1)));
        entityManager.persist(newAdoptionRecord(cat, "recent-cat@example.com", since.plusDays(1)));
        entityManager.flush();

        var recentAdoptions = adoptionRecordRepository.findRecentAdoptionsByAnimalType(
                AnimalType.DOG, since);

        assertThat(recentAdoptions)
                .extracting(AdoptionRecord::getAdopterEmail)
                .containsExactlyInAnyOrder("recent-dog@example.com", "cutoff-dog@example.com");
    }

    @Test
    void findAdoptionsByAdopterEmailDomain_shouldReturnOnlyExactDomainMatches() {
        Animal dog = newAnimal("Rex", AnimalType.DOG);
        entityManager.persist(dog);
        entityManager.flush();

        LocalDateTime adoptedAt = LocalDateTime.of(2025, 1, 1, 12, 0);
        entityManager.persist(newAdoptionRecord(dog, "anna@example.com", adoptedAt));
        entityManager.persist(newAdoptionRecord(dog, "bob@example.com", adoptedAt.plusDays(1)));
        entityManager.persist(newAdoptionRecord(dog, "carol@example.net", adoptedAt.plusDays(2)));
        entityManager.persist(newAdoptionRecord(dog, "dave@notexample.com", adoptedAt.plusDays(3)));
        entityManager.persist(newAdoptionRecord(dog, "eve@sub.example.com", adoptedAt.plusDays(4)));
        entityManager.flush();

        var adoptions = adoptionRecordRepository.findAdoptionsByAdopterEmailDomain("example.com");

        assertThat(adoptions)
                .extracting(AdoptionRecord::getAdopterEmail)
                .containsExactlyInAnyOrder("anna@example.com", "bob@example.com");
    }

    private Animal newAnimal(String name, AnimalType type) {
        Animal animal = new Animal();
        animal.setName(name);
        animal.setType(type);
        animal.setStatus(AnimalStatus.ADOPTED);
        return animal;
    }

    private AdoptionRecord newAdoptionRecord(
            Animal animal, String adopterEmail, LocalDateTime adoptedAt) {
        AdoptionRecord adoptionRecord = new AdoptionRecord();
        adoptionRecord.setAnimal(animal);
        adoptionRecord.setAdopterEmail(adopterEmail);
        adoptionRecord.setAdoptedAt(adoptedAt);
        return adoptionRecord;
    }
}
