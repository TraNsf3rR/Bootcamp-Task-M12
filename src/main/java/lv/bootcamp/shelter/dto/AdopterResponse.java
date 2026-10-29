package lv.bootcamp.shelter.dto;

/**
 * Response representation of an adopter.
 */
public record AdopterResponse(
        Long id,
        String name,
        String email,
        int age
) {
}
