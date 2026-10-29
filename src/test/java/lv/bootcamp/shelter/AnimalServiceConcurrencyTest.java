package lv.bootcamp.shelter;

import lv.bootcamp.shelter.dto.AnimalCreateRequest;
import lv.bootcamp.shelter.dto.AnimalResponse;
import lv.bootcamp.shelter.model.Animal;
import lv.bootcamp.shelter.model.AnimalStatus;
import lv.bootcamp.shelter.model.AnimalType;
import lv.bootcamp.shelter.repository.AnimalRepository;
import lv.bootcamp.shelter.service.AnimalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AnimalServiceConcurrencyTest {

    @Autowired
    private AnimalService animalService;

    @MockitoSpyBean
    private AnimalRepository animalRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String testMicrochipId;

    @AfterEach
    void removeCreatedAnimal() {
        if (testMicrochipId == null) {
            return;
        }

        animalRepository.findAll().stream()
                .filter(animal -> testMicrochipId.equals(animal.getMicrochipId()))
                .map(Animal::getId)
                .forEach(animalRepository::deleteById);
    }

    @Test
    void concurrentCreate_shouldRelyOnDatabaseConstraintWhenBothChecksPass() throws Exception {
        testMicrochipId = "race-" + UUID.randomUUID();
        AnimalCreateRequest request = new AnimalCreateRequest(
                "Rex", AnimalType.DOG, "Labrador", 3, "Friendly", testMicrochipId);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch bothChecksReturnedFalse = new CountDownLatch(2);
        // Read real database state, then hold both false results until both transactions have checked.
        doAnswer(invocation -> {
            String requestedMicrochipId = invocation.getArgument(0);
            Integer count = jdbcTemplate.queryForObject(
                    "select count(*) from animals where microchip_id = ?",
                    Integer.class,
                    requestedMicrochipId);
            boolean exists = count != null && count > 0;
            if (!exists) {
                bothChecksReturnedFalse.countDown();
                if (!bothChecksReturnedFalse.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Both create calls did not reach the microchip check");
                }
            }
            return exists;
        }).when(animalRepository).existsByMicrochipId(testMicrochipId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<CreationOutcome> first = executor.submit(
                    () -> createAfterGate(startGate, request));
            Future<CreationOutcome> second = executor.submit(
                    () -> createAfterGate(startGate, request));
            startGate.countDown();

            List<CreationOutcome> outcomes = List.of(
                    first.get(30, TimeUnit.SECONDS),
                    second.get(30, TimeUnit.SECONDS));

            verify(animalRepository, times(2)).existsByMicrochipId(testMicrochipId);
            assertThat(bothChecksReturnedFalse.getCount())
                    .as("Both existence checks should return false before either save; outcomes: %s", outcomes)
                    .isZero();
            assertThat(outcomes.stream().filter(outcome -> outcome.response() != null).count())
                    .isEqualTo(1);
            assertThat(outcomes.stream().filter(outcome -> outcome.exception() != null).count())
                    .isEqualTo(1);

            CreationOutcome success = outcomes.stream()
                    .filter(outcome -> outcome.response() != null)
                    .findFirst()
                    .orElseThrow();
            CreationOutcome failure = outcomes.stream()
                    .filter(outcome -> outcome.exception() != null)
                    .findFirst()
                    .orElseThrow();
            assertThat(success.response().status()).isEqualTo(AnimalStatus.AVAILABLE);
            assertThat(failure.exception()).isInstanceOf(DataIntegrityViolationException.class);

            List<Animal> matchingAnimals = animalRepository.findAll().stream()
                    .filter(animal -> testMicrochipId.equals(animal.getMicrochipId()))
                    .toList();
            assertThat(matchingAnimals).hasSize(1);
        } finally {
            startGate.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private CreationOutcome createAfterGate(
            CountDownLatch startGate, AnimalCreateRequest request) throws InterruptedException {
        startGate.await();
        try {
            return new CreationOutcome(animalService.create(request), null);
        } catch (RuntimeException exception) {
            return new CreationOutcome(null, exception);
        }
    }

    private record CreationOutcome(AnimalResponse response, RuntimeException exception) {
    }
}
