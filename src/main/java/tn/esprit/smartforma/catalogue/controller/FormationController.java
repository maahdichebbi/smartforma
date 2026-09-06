package tn.esprit.smartforma.catalogue.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartforma.catalogue.dto.FormationDto;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.service.FormationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/formations")
@RequiredArgsConstructor
public class FormationController {

    private final FormationService formationService;

    @GetMapping
    public List<Formation> findAll() {
        return formationService.findAll();
    }

    @GetMapping("/{id}")
    public Formation findById(@PathVariable Long id) {
        return formationService.findById(id);
    }

    /**
     * Unified search + filter endpoint.
     * All query params are optional — any combination works.
     *
     * Examples:
     *   GET /api/v1/formations/search?titre=spring
     *   GET /api/v1/formations/search?categorieId=1
     *   GET /api/v1/formations/search?niveau=AVANCE
     *   GET /api/v1/formations/search?titre=java&niveau=INTERMEDIAIRE
     */
    @GetMapping("/search")
    public List<Formation> search(
            @RequestParam(required = false) String titre,
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false) String niveau
    ) {
        return formationService.search(titre, categorieId, niveau);
    }

    @PostMapping
    public ResponseEntity<Formation> create(@Valid @RequestBody FormationDto dto) {
        Formation created = formationService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Formation update(@PathVariable Long id, @Valid @RequestBody FormationDto dto) {
        return formationService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        formationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
