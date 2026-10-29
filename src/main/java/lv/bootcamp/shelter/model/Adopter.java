package lv.bootcamp.shelter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a shelter adopter.
 * Fully implemented and fully tested — use AdopterRepositoryTest as the
 * reference pattern for the Animal-side persistence tests (see ANIMAL-3).
 */
@Entity
@Table(name = "adopters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Adopter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private int age;

    private int currentPetCount;

    private int previousAdoptions;

    private boolean largeProperty;

    private boolean exoticPermit;
}
