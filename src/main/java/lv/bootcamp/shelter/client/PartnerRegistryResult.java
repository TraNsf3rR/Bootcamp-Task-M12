package lv.bootcamp.shelter.client;

/**
 * Result of a partner registry lookup for a given microchip ID.
 */
public record PartnerRegistryResult(boolean registered, String partnerShelterName) {
}
