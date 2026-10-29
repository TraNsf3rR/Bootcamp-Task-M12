package lv.bootcamp.shelter.service;

/**
 * Thrown when a requested adopter does not exist.
 */
public class AdopterNotFoundException extends RuntimeException {

    public AdopterNotFoundException(Long id) {
        super("Adopter not found: " + id);
    }
}
