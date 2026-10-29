# Shelter Testing — Practical Task (M12: Advanced Testing)

Suggested time budget: **~3 hours total**. Work through the tickets below in
order — later ones assume you've seen the patterns from earlier ones and
from the already-implemented reference examples.

## Domain

A shelter application: animals, adopters, adoption, a partner-shelter
registry check, and an adoption-eligibility policy service. Same domain used
in the lecture, extended with a couple of new pieces for this task.

## Before you start — already fully implemented (read these first)

- **`AdopterService` / `AdopterController` / `AdopterRepository`** and their
  tests (`AdopterServiceTest`, `AdopterControllerTest`, `AdopterRepositoryTest`)
  — a complete, passing example of every test style used below (Mockito,
  MockMvc, `@DataJpaTest`, including a uniqueness-constraint test). Mirror
  this pattern for your tickets.
- **`AnimalControllerSecurityTest`** — role-based endpoint security with
  MockMvc. Covered conceptually in the lecture; not something you need to
  write yourself.
- **`AnimalPageControllerTest`** — testing a view-returning `@Controller`
  (Thymeleaf). Also provided as a finished example.

## Your tickets

### ANIMAL-1 — Service tests for `AnimalService` (Mockito)

**File:** add to the existing `AnimalServiceTest` (package `lv.bootcamp.shelter.service`).

Three tests are already implemented. Three more behaviours of `AnimalService`
still need coverage:

- Adopting an available animal must change its status and trigger an
  external adoption notification with the correct details.
- Reserving multiple animals must notify an external system with exactly
  the IDs that were reserved — no more, no fewer.
- Creating an animal whose microchip ID is already registered must be
  rejected, and no save should be attempted.

Decide which Mockito tools fit each test (stubbing, `verify()`, argument
capturing) — `AdopterServiceTest` shows the general pattern, not the exact
answer.

**Acceptance criteria:** each test fails if the corresponding behaviour is
broken, and passes against the current implementation.

### ANIMAL-2 — REST controller tests for `AnimalController` (MockMvc)

**File:** add to the existing `AnimalControllerTest` (package `lv.bootcamp.shelter.controller`).

One test is already implemented. Two more controller behaviours still need
coverage:

- Listing animals returns the shelter's current animals as JSON.
- Creating an animal with an invalid request body is rejected before it
  ever reaches the service.

**Acceptance criteria:** each test fails if the corresponding behaviour is
broken.

### ANIMAL-3 — Persistence tests (`@DataJpaTest`): constraint + join query

**Files:**
- add to the existing `AnimalRepositoryTest` (package `lv.bootcamp.shelter.repository`)
- create a new `AdoptionRecordRepositoryTest` from scratch in the same
  package, `@DataJpaTest`-based, autowiring `AdoptionRecordRepository` and
  `TestEntityManager` — mirror the setup style used in `AnimalRepositoryTest`.

Two persistence-layer guarantees still need verifying:

- `Animal.microchipId` must be unique at the database level — saving a
  duplicate must be rejected, not silently allowed.
- `AdoptionRecordRepository.findRecentAdoptionsByAnimalType` must actually
  filter by animal type and date, not just return everything in the table.

`AdopterRepositoryTest` shows the constraint-testing pattern used elsewhere
in this project. Persisting an `AdoptionRecord` alongside the `Animal` it
references (a `@ManyToOne` relationship) isn't demonstrated anywhere else in
this repo — Slide 16 of the module deck only shows `entityManager.persist()`
for a single, standalone entity.

**Acceptance criteria:** the constraint test asserts on the specific
exception type, not just "an exception was thrown"; the join-query test
includes data that should *not* match, so a test that returns everything
would still fail.

### EXT-1 — External partner registry client (`RestClient` + `MockWebServer`)

**File:** create a new `RestClientPartnerRegistryClientTest` from scratch in
package `lv.bootcamp.shelter.client`.

`RestClientPartnerRegistryClient` makes a real HTTP call to a partner
registry. Verify it behaves correctly both when the partner responds
successfully and when the partner fails. You'll need to start/stop a
`MockWebServer` around each test and construct the client against its URL —
Slide 18 of the module deck has a worked example of this exact setup with a
different client.

**Acceptance criteria:** both tests exercise the real client against a real
local HTTP server (not a mock of the client itself) — that's the whole
point of `MockWebServer` vs. `@MockitoBean`.

### COVERAGE-1 — Mutation coverage on `AdoptionEligibilityService`

**File:** create a new `AdoptionEligibilityServiceTest` from scratch in
package `lv.bootcamp.shelter.service` — Mockito-based (`@ExtendWith(MockitoExtension.class)`),
mocking `AdopterRepository`, `AnimalRepository`, `NotificationClient`, and
`AuditLogger`, with `AdoptionEligibilityService` as the `@InjectMocks`
target. `AnimalServiceTest` shows the same wiring shape.

This service has no tests yet. It has several branches: adopter not found,
animal not found, animal not available, underage adopter, pet-limit
reached, exotic-permit required, and a priority-score calculation with
multiple score-affecting conditions.

Write tests to cover as many of these branches as you can, then run PiTest
scoped to this one class:

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Open `target/pit-reports/index.html` — green = killed mutant, red = survived.

**Acceptance criteria:** there is no fixed mutation-score target — the goal
is to *look at which mutants survive* and add a test that kills them,
especially around the boundary conditions (`>=` vs `>`, age `18`, pet-limit
`3`/`5`).

## Bonus (optional — not required to finish the task)

- **Prove that `AnimalService.create()`'s microchip check is not race-safe.**
  `AnimalService.create()` rejects a new animal if
  `AnimalRepository.existsByMicrochipId(...)` already returns true for its
  microchip — a common "is this enough?" pattern. It looks sufficient
  because it works every time you call it once at a time. A classic
  interview question: "how would you test that two simultaneous requests
  actually conflict?" Fire two overlapping `POST /api/animals` requests (or
  two overlapping `animalService.create()` calls) with the same
  `microchipId` and show that **both** calls can observe
  `existsByMicrochipId(...) == false` before either has saved — i.e. the
  service-level check alone would let both through. Then confirm what
  actually stops the duplicate: exactly one call succeeds and exactly one
  row with that `microchipId` exists afterwards, enforced by the database
  constraint, not the service check. Note: `@DataJpaTest` runs each test in
  one rolled-back transaction on one connection, which hides real
  concurrent-commit behaviour — this needs `@SpringBootTest` instead
  (without wrapping the test itself in `@Transactional`), so each concurrent
  call gets its own real transaction and connection.
- Create a new `AdoptionIntegrationTest` from scratch in package
  `lv.bootcamp.shelter`, `@SpringBootTest` + `@Transactional`, mocking only
  `NotificationClient` — cover the full create → adopt flow. This pattern
  isn't demonstrated anywhere else in this repo — see Slides 19–20 of the
  module deck for a worked example.
- Add a success-path test to `AnimalControllerTest` (`POST /api/animals`
  with a valid body, assert 201 and the response JSON) as the counterpart
  to ANIMAL-2's validation test. You'll need to autowire `ObjectMapper` to
  serialize the request body — `AdopterControllerTest` shows this pattern.
- Add a second, differently-filtered query to `AdoptionRecordRepositoryTest`
  (e.g. adoptions filtered by adopter email domain).

## Running tests

```bash
mvn test
```

## Running PiTest (scoped to one class)

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Report: `target/pit-reports/index.html`. The plugin is scoped (via
`targetClasses`/`targetTests` in `pom.xml`) to `AdoptionEligibilityService`
only — it is not bound to `mvn test`, so it never slows down your normal
test run.

## Project structure

```
src/main/java/lv/bootcamp/shelter/
├── audit/
│   ├── AuditLogger.java                 (interface — mocked in COVERAGE-1)
│   ├── LoggingAuditLogger.java
│   └── RejectionReason.java
├── client/
│   ├── NotificationClient.java          (interface — external seam)
│   ├── LoggingNotificationClient.java   (default impl)
│   ├── PartnerRegistryClient.java       (interface — external seam, EXT-1)
│   ├── PartnerRegistryResult.java
│   └── RestClientPartnerRegistryClient.java
├── config/
│   └── SecurityConfig.java
├── controller/
│   ├── AdopterController.java           (fully implemented + tested)
│   ├── AnimalController.java            (REST endpoints — ANIMAL-2)
│   ├── AnimalPageController.java        (view controller — fully tested)
│   └── GlobalExceptionHandler.java
├── dto/
│   ├── AdopterCreateRequest.java / AdopterResponse.java
│   └── AdoptionRequest.java / AnimalCreateRequest.java / AnimalResponse.java
├── model/
│   ├── Adopter.java                     (JPA entity)
│   ├── Animal.java                      (JPA entity — has microchipId, ANIMAL-3)
│   ├── AdoptionRecord.java              (JPA entity — join target, ANIMAL-3)
│   ├── AdoptionResult.java
│   ├── AnimalStatus.java / AnimalType.java
├── repository/
│   ├── AdopterRepository.java
│   ├── AnimalRepository.java            (ANIMAL-3)
│   └── AdoptionRecordRepository.java    (join query — ANIMAL-3)
├── service/
│   ├── AdopterService.java              (fully implemented + tested)
│   ├── AnimalService.java               (business logic — ANIMAL-1)
│   ├── AdoptionEligibilityService.java  (branch-heavy — COVERAGE-1)
│   ├── RejectionReasons.java
│   └── AnimalNotFoundException.java / AdopterNotFoundException.java
└── ShelterTestingApplication.java

src/test/java/lv/bootcamp/shelter/
├── client/
│   └── RestClientPartnerRegistryClientTest.java   (EXT-1 — create this)
├── controller/
│   ├── AdopterControllerTest.java                 (reference example)
│   ├── AnimalControllerTest.java                   (ANIMAL-2 — add to this)
│   ├── AnimalControllerSecurityTest.java           (reference example)
│   └── AnimalPageControllerTest.java               (reference example)
├── repository/
│   ├── AdopterRepositoryTest.java                  (reference example)
│   ├── AnimalRepositoryTest.java                   (ANIMAL-3 — add to this)
│   └── AdoptionRecordRepositoryTest.java           (ANIMAL-3 — create this)
├── service/
│   ├── AdopterServiceTest.java                     (reference example)
│   ├── AnimalServiceTest.java                      (ANIMAL-1 — add to this)
│   └── AdoptionEligibilityServiceTest.java         (COVERAGE-1 — create this)
└── AdoptionIntegrationTest.java                    (bonus — create this)
```

## Prerequisites

- Java 21
- Maven 3.9+
