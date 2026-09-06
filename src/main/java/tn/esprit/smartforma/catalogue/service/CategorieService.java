package tn.esprit.smartforma.catalogue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartforma.catalogue.dto.CategorieDto;
import tn.esprit.smartforma.catalogue.entity.Categorie;
import tn.esprit.smartforma.catalogue.repository.CategorieRepository;
import tn.esprit.smartforma.catalogue.repository.FormationRepository;
import tn.esprit.smartforma.exception.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final FormationRepository formationRepository;

    // ── Read operations ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Categorie> findAll() {
        return categorieRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Categorie findById(Long id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
    }

    // ── Write operations ───────────────────────────────────────────────────────

    public Categorie create(CategorieDto dto) {
        // Business rule: category name must be unique
        if (categorieRepository.existsByNomIgnoreCase(dto.nom())) {
            throw new IllegalArgumentException(
                    "Une catégorie avec le nom '" + dto.nom() + "' existe déjà."
            );
        }

        Categorie categorie = new Categorie();
        categorie.setNom(dto.nom().trim());
        categorie.setDescription(dto.description());
        return categorieRepository.save(categorie);
    }

    public Categorie update(Long id, CategorieDto dto) {
        Categorie categorie = findById(id);

        // Business rule: new name must not conflict with another existing category
        categorieRepository.findByNomIgnoreCase(dto.nom())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(conflict -> {
                    throw new IllegalArgumentException(
                            "Une catégorie avec le nom '" + dto.nom() + "' existe déjà."
                    );
                });

        categorie.setNom(dto.nom().trim());
        categorie.setDescription(dto.description());
        return categorieRepository.save(categorie);
    }

    public void delete(Long id) {
        findById(id); // ensures it exists, throws 404 if not

        // Business rule: cannot delete a category that still has formations
        if (formationRepository.existsByCategorieId(id)) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer cette catégorie : elle contient des formations."
            );
        }

        categorieRepository.deleteById(id);
    }
}
