package lv.bootcamp.shelter.repository;

import lv.bootcamp.shelter.model.Adopter;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for Adopter entities.
 */
public interface AdopterRepository extends JpaRepository<Adopter, Long> {
}
