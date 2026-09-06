package tn.esprit.smartforma.catalogue.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartforma.catalogue.dto.CategorieDto;
import tn.esprit.smartforma.catalogue.entity.Categorie;
import tn.esprit.smartforma.catalogue.service.CategorieService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategorieController {

    private final CategorieService categorieService;

    @GetMapping
    public List<Categorie> findAll() {
        return categorieService.findAll();
    }

    @GetMapping("/{id}")
    public Categorie findById(@PathVariable Long id) {
        return categorieService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Categorie> create(@Valid @RequestBody CategorieDto dto) {
        Categorie created = categorieService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Categorie update(@PathVariable Long id, @Valid @RequestBody CategorieDto dto) {
        return categorieService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categorieService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
