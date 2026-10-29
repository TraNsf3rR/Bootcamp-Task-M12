package lv.bootcamp.shelter.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Production implementation backed by a real HTTP call via RestClient.
 * Tested with MockWebServer instead of mocking this class directly — see
 * RestClientPartnerRegistryClientTest (EXT-1 in the README).
 */
@Component
public class RestClientPartnerRegistryClient implements PartnerRegistryClient {

    private final RestClient restClient;

    public RestClientPartnerRegistryClient(
            RestClient.Builder builder,
            @Value("${shelter.partner-registry.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public PartnerRegistryResult lookup(String microchipId) {
        return restClient.get()
                .uri("/registry/{microchipId}", microchipId)
                .retrieve()
                .body(PartnerRegistryResult.class);
    }
}
