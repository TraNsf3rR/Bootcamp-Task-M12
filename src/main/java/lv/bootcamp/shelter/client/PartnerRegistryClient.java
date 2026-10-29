package lv.bootcamp.shelter.client;

/**
 * External seam: checks a microchip ID against a partner shelter registry
 * before finalising an adoption, to catch animals already registered elsewhere.
 */
public interface PartnerRegistryClient {

    PartnerRegistryResult lookup(String microchipId);
}
