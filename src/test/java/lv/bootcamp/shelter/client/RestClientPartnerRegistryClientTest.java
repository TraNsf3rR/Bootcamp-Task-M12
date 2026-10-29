package lv.bootcamp.shelter.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestClientPartnerRegistryClientTest {

    private MockWebServer mockWebServer;
    private RestClientPartnerRegistryClient partnerRegistryClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        partnerRegistryClient = new RestClientPartnerRegistryClient(
                RestClient.builder(), mockWebServer.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void lookup_shouldReturnPartnerResultWhenRegistryRespondsSuccessfully() throws InterruptedException {
        String microchipId = "chip-123";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"registered\":true,\"partnerShelterName\":\"Partner Shelter\"}"));

        PartnerRegistryResult result = partnerRegistryClient.lookup(microchipId);

        assertThat(result).isEqualTo(new PartnerRegistryResult(true, "Partner Shelter"));
        assertLookupRequest(microchipId);
    }

    @Test
    void lookup_shouldPropagatePartnerServerFailure() throws InterruptedException {
        String microchipId = "chip-123";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(503)
                .setBody("Partner registry unavailable"));

        assertThatThrownBy(() -> partnerRegistryClient.lookup(microchipId))
                .isInstanceOfSatisfying(HttpServerErrorException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertLookupRequest(microchipId);
    }

    private void assertLookupRequest(String microchipId) throws InterruptedException {
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/registry/" + microchipId);
    }
}
