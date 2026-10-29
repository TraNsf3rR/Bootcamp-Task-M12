package lv.bootcamp.shelter.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingAuditLogger implements AuditLogger {

    @Override
    public void logRejection(Long adopterId, Long animalId, RejectionReason reason) {
        log.info("Adoption rejected: adopter {} / animal {} — reason {}", adopterId, animalId, reason);
    }

    @Override
    public void logApproval(Long adopterId, Long animalId, int priorityScore) {
        log.info("Adoption approved: adopter {} / animal {} — priority score {}",
                adopterId, animalId, priorityScore);
    }
}
