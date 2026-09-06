package tn.esprit.smartforma.catalogue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartforma.catalogue.dto.FormationDto;
import tn.esprit.smartforma.catalogue.entity.Categorie;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.entity.Niveau;
import tn.esprit.smartforma.catalogue.repository.CategorieRepository;
import tn.esprit.smartforma.catalogue.repository.FormationRepository;
import tn.esprit.smartforma.exception.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FormationService {

    private final FormationRepository formationRepository;
    private final CategorieRepository categorieRepository;

    // ── Read operations ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Formation> findAll() {
        return formationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Formation findById(Long id) {
        return formationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", id));
    }

    /**
     * Unified search/filter endpoint.
     * Any parameter can be null — null parameters are ignored in the query.
     *
     * @param titre      partial title keyword (optional)
     * @param categorieId filter by category (optional)
     * @param niveau     filter by level (optional)
     */
    @Transactional(readOnly = true)
    public List<Formation> search(String titre, Long categorieId, String niveau) {
        Niveau niveauEnum = null;
        if (niveau != null && !niveau.isBlank()) {
            try {
                niveauEnum = Niveau.valueOf(niveau.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Niveau invalide : '" + niveau + "'. Valeurs acceptées : DEBUTANT, INTERMEDIAIRE, AVANCE."
                );
            }
        }
        return formationRepository.search(titre, categorieId, niveauEnum);
    }

    // ── Write operations ───────────────────────────────────────────────────────

    public Formation create(FormationDto dto) {
        Categorie categorie = categorieRepository.findById(dto.categorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", dto.categorieId()));

        Formation formation = new Formation();
        mapDtoToEntity(dto, formation, categorie);
        return formationRepository.save(formation);
    }

    public Formation update(Long id, FormationDto dto) {
        Formation formation = findById(id);

        Categorie categorie = categorieRepository.findById(dto.categorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", dto.categorieId()));

        mapDtoToEntity(dto, formation, categorie);
        return formationRepository.save(formation);
    }

    public void delete(Long id) {
        findById(id); // ensures it exists, throws 404 if not
        // Chapters are deleted automatically via cascade=ALL + orphanRemoval=true
        formationRepository.deleteById(id);
    }

    // ── Private helper ─────────────────────────────────────────────────────────

    private void mapDtoToEntity(FormationDto dto, Formation formation, Categorie categorie) {
        formation.setTitre(dto.titre());
        formation.setDescription(dto.description());
        formation.setPrix(dto.prix());
        formation.setNiveau(dto.niveau());
        formation.setDureeHeures(dto.dureeHeures());
        formation.setCategorie(categorie);
        formation.setTags(dto.tags());
    }
}
