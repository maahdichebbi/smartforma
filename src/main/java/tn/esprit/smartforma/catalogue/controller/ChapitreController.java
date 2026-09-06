package tn.esprit.smartforma.catalogue.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartforma.catalogue.dto.ChapitreDto;
import tn.esprit.smartforma.catalogue.entity.Chapitre;
import tn.esprit.smartforma.catalogue.service.ChapitreService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/formations/{formationId}/chapitres")
@RequiredArgsConstructor
public class ChapitreController {

    private final ChapitreService chapitreService;

    @GetMapping
    public List<Chapitre> findByFormation(@PathVariable Long formationId) {
        return chapitreService.findByFormation(formationId);
    }

    @GetMapping("/{id}")
    public Chapitre findById(@PathVariable Long formationId, @PathVariable Long id) {
        return chapitreService.findById(formationId, id);
    }

    @PostMapping
    public ResponseEntity<Chapitre> create(
            @PathVariable Long formationId,
            @Valid @RequestBody ChapitreDto dto
    ) {
        Chapitre created = chapitreService.create(formationId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Chapitre update(
            @PathVariable Long formationId,
            @PathVariable Long id,
            @Valid @RequestBody ChapitreDto dto
    ) {
        return chapitreService.update(formationId, id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long formationId, @PathVariable Long id) {
        chapitreService.delete(formationId, id);
        return ResponseEntity.noContent().build();
    }
}
