package lv.bootcamp.shelter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Historical record of a completed adoption.
 * Used for the join-query exercise in AdoptionRecordRepositoryTest (ANIMAL-3) —
 * joins back to Animal via a many-to-one relation.
 */
@Entity
@Table(name = "adoption_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdoptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(nullable = false)
    private String adopterEmail;

    @Column(nullable = false)
    private LocalDateTime adoptedAt;
}
