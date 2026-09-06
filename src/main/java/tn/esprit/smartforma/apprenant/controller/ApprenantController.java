package tn.esprit.smartforma.apprenant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartforma.apprenant.dto.ApprenantDto;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.apprenant.service.ApprenantService;
import tn.esprit.smartforma.auth.security.CurrentUserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/apprenants")
@RequiredArgsConstructor
public class ApprenantController {

    private final ApprenantService apprenantService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<Apprenant> findAll() {
        return apprenantService.findAll();
    }

    /** Returns the learner profile linked to the authenticated account. */
    @GetMapping("/me")
    public Apprenant me() {
        return apprenantService.findById(currentUserService.requireApprenantId());
    }

    /** Updates the learner profile linked to the authenticated account. */
    @PutMapping("/me")
    public Apprenant updateMe(@Valid @RequestBody ApprenantDto dto) {
        return apprenantService.update(currentUserService.requireApprenantId(), dto);
    }

    @GetMapping("/{id}")
    public Apprenant findById(@PathVariable Long id) {
        currentUserService.assertCanAccessApprenant(id);
        return apprenantService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Apprenant> create(@Valid @RequestBody ApprenantDto dto) {
        Apprenant created = apprenantService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Apprenant update(@PathVariable Long id, @Valid @RequestBody ApprenantDto dto) {
        currentUserService.assertCanAccessApprenant(id);
        return apprenantService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        apprenantService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
