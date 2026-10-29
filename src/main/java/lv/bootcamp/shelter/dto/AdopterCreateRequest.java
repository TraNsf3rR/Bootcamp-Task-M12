package lv.bootcamp.shelter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for registering a new adopter.
 */
public record AdopterCreateRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @Min(0) int age
) {
}
