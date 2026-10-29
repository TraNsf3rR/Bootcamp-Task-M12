package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.Adopter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reference example: fully implemented @DataJpaTest for AdopterRepository,
 * including a uniqueness-constraint test. Mirror this pattern for
 * AnimalRepositoryTest and AdoptionRecordRepositoryTest (ANIMAL-3 in the README).
 */
@DataJpaTest
class AdopterRepositoryTest {

    @Autowired
    private AdopterRepository adopterRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void save_shouldPersistAdopterAndGenerateId() {
        Adopter adopter = newAdopter("Anna", "anna@example.com");

        Adopter saved = adopterRepository.save(adopter);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Anna");
    }

    @Test
    void save_shouldFailWhenEmailIsDuplicated() {
        entityManager.persist(newAdopter("Anna", "anna@example.com"));
        entityManager.flush();

        assertThatThrownBy(() -> {
            adopterRepository.save(newAdopter("Anna Duplicate", "anna@example.com"));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Adopter newAdopter(String name, String email) {
        Adopter adopter = new Adopter();
        adopter.setName(name);
        adopter.setEmail(email);
        adopter.setAge(25);
        return adopter;
    }
}
