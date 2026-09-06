package tn.esprit.smartforma.catalogue.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartforma.catalogue.dto.ChapitreDto;
import tn.esprit.smartforma.catalogue.entity.Chapitre;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.repository.ChapitreRepository;
import tn.esprit.smartforma.catalogue.repository.FormationRepository;
import tn.esprit.smartforma.exception.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChapitreService {

    private final ChapitreRepository chapitreRepository;
    private final FormationRepository formationRepository;

    // ── Read operations ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Chapitre> findByFormation(Long formationId) {
        // Verifies the formation exists before returning chapters
        if (!formationRepository.existsById(formationId)) {
            throw new ResourceNotFoundException("Formation", formationId);
        }
        return chapitreRepository.findByFormationIdOrderByOrdreAsc(formationId);
    }

    @Transactional(readOnly = true)
    public Chapitre findById(Long formationId, Long chapitreId) {
        Chapitre chapitre = chapitreRepository.findById(chapitreId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapitre", chapitreId));

        // Business rule: the chapter must belong to the specified formation
        if (!chapitre.getFormation().getId().equals(formationId)) {
            throw new ResourceNotFoundException("Chapitre", chapitreId);
        }
        return chapitre;
    }

    // ── Write operations ───────────────────────────────────────────────────────

    public Chapitre create(Long formationId, ChapitreDto dto) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", formationId));

        // Business rule: order number must be unique within the formation
        if (chapitreRepository.existsByFormationIdAndOrdre(formationId, dto.ordre())) {
            throw new IllegalArgumentException(
                    "Un chapitre avec l'ordre " + dto.ordre() + " existe déjà dans cette formation."
            );
        }

        Chapitre chapitre = new Chapitre();
        chapitre.setTitre(dto.titre());
        chapitre.setDescription(dto.description());
        chapitre.setOrdre(dto.ordre());
        chapitre.setFormation(formation);
        return chapitreRepository.save(chapitre);
    }

    public Chapitre update(Long formationId, Long chapitreId, ChapitreDto dto) {
        Chapitre chapitre = findById(formationId, chapitreId);

        // Business rule: new order must not conflict with another chapter in the same formation
        boolean orderConflict = chapitreRepository
                .existsByFormationIdAndOrdre(formationId, dto.ordre());

        if (orderConflict && !chapitre.getOrdre().equals(dto.ordre())) {
            throw new IllegalArgumentException(
                    "Un chapitre avec l'ordre " + dto.ordre() + " existe déjà dans cette formation."
            );
        }

        chapitre.setTitre(dto.titre());
        chapitre.setDescription(dto.description());
        chapitre.setOrdre(dto.ordre());
        return chapitreRepository.save(chapitre);
    }

    public void delete(Long formationId, Long chapitreId) {
        Chapitre chapitre = findById(formationId, chapitreId);
        chapitreRepository.delete(chapitre);
    }
}
