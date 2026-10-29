package lv.bootcamp.shelter.audit;

/**
 * Audit seam for adoption-eligibility decisions — mocked in
 * AdoptionEligibilityServiceTest (COVERAGE-1).
 */
public interface AuditLogger {

    void logRejection(Long adopterId, Long animalId, RejectionReason reason);

    void logApproval(Long adopterId, Long animalId, int priorityScore);
}
