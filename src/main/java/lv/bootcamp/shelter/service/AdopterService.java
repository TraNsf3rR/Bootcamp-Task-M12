package lv.bootcamp.shelter.service;

import lombok.RequiredArgsConstructor;
import lv.bootcamp.shelter.dto.AdopterCreateRequest;
import lv.bootcamp.shelter.dto.AdopterResponse;
import lv.bootcamp.shelter.model.Adopter;
import lv.bootcamp.shelter.repository.AdopterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for adopter registration.
 * Fully implemented and fully tested — see AdopterServiceTest as the
 * reference pattern for the AnimalServiceTest tickets (ANIMAL-1).
 */
@Service
@RequiredArgsConstructor
public class AdopterService {

    private final AdopterRepository adopterRepository;

    @Transactional
    public AdopterResponse register(AdopterCreateRequest request) {
        Adopter adopter = new Adopter();
        adopter.setName(request.name());
        adopter.setEmail(request.email());
        adopter.setAge(request.age());

        Adopter saved = adopterRepository.save(adopter);
        return toResponse(saved);
    }

    public AdopterResponse findById(Long id) {
        Adopter adopter = adopterRepository.findById(id)
                .orElseThrow(() -> new AdopterNotFoundException(id));
        return toResponse(adopter);
    }

    private AdopterResponse toResponse(Adopter adopter) {
        return new AdopterResponse(
                adopter.getId(), adopter.getName(), adopter.getEmail(), adopter.getAge());
    }
}
