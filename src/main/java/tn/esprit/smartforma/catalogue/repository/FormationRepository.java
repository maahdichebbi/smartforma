package tn.esprit.smartforma.catalogue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.entity.Niveau;

import java.util.List;

public interface FormationRepository extends JpaRepository<Formation, Long> {

    /** Search by title — case-insensitive partial match. */
    List<Formation> findByTitreContainingIgnoreCase(String titre);

    /** Filter by category ID. */
    List<Formation> findByCategorieId(Long categorieId);

    /** Filter by level. */
    List<Formation> findByNiveau(Niveau niveau);

    /** Filter by both category and level at the same time. */
    List<Formation> findByCategorieIdAndNiveau(Long categorieId, Niveau niveau);

    /** Check if a category still has formations (used before deleting a category). */
    boolean existsByCategorieId(Long categorieId);

    /**
     * Search/filter combined query: title keyword + optional category + optional level.
     * Parameters that are null are ignored, which lets us use one method for the search+filter endpoint.
     */
    @Query("""
            SELECT f FROM Formation f
            WHERE (:titre IS NULL OR LOWER(f.titre) LIKE LOWER(CONCAT('%', :titre, '%')))
              AND (:categorieId IS NULL OR f.categorie.id = :categorieId)
              AND (:niveau IS NULL OR f.niveau = :niveau)
            """)
    List<Formation> search(
            @Param("titre") String titre,
            @Param("categorieId") Long categorieId,
            @Param("niveau") Niveau niveau
    );
}
