package lv.bootcamp.shelter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lv.bootcamp.shelter.dto.AdopterCreateRequest;
import lv.bootcamp.shelter.dto.AdopterResponse;
import lv.bootcamp.shelter.service.AdopterService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for adopter registration.
 * Fully implemented and fully tested — see AdopterControllerTest as the
 * reference pattern for the AnimalControllerTest tickets (ANIMAL-2).
 */
@RestController
@RequestMapping("/api/adopters")
@RequiredArgsConstructor
public class AdopterController {

    private final AdopterService adopterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdopterResponse register(@RequestBody @Valid AdopterCreateRequest request) {
        return adopterService.register(request);
    }

    @GetMapping("/{id}")
    public AdopterResponse findById(@PathVariable Long id) {
        return adopterService.findById(id);
    }
}
