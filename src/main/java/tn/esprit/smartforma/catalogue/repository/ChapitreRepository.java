package tn.esprit.smartforma.catalogue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.smartforma.catalogue.entity.Chapitre;

import java.util.List;

public interface ChapitreRepository extends JpaRepository<Chapitre, Long> {

    /** Returns all chapters of a formation ordered by their 'ordre' field. */
    List<Chapitre> findByFormationIdOrderByOrdreAsc(Long formationId);

    /** Checks if a given order number is already used in a formation (for duplicate order validation). */
    boolean existsByFormationIdAndOrdre(Long formationId, Integer ordre);
}
