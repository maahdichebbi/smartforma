package tn.esprit.smartforma.inscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartforma.inscription.entity.Session;

import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    /** All sessions for a given formation, ordered by start date. */
    List<Session> findByFormationIdOrderByDateDebutAsc(Long formationId);

    /** Sessions that haven't started yet — available for registration. */
    List<Session> findByDateDebutAfterOrderByDateDebutAsc(LocalDate today);

    /** Available sessions for a specific formation (start date is in the future). */
    List<Session> findByFormationIdAndDateDebutAfterOrderByDateDebutAsc(
            Long formationId, LocalDate today
    );

    /** Check if a formation has sessions (used to guard formation deletion if needed). */
    boolean existsByFormationId(Long formationId);

    /**
     * Count ACTIVE (non-cancelled) inscriptions for a session.
     * More efficient than loading all inscriptions into memory just to count.
     */
    @Query("""
            SELECT COUNT(i) FROM Inscription i
            WHERE i.session.id = :sessionId
              AND i.statut <> tn.esprit.smartforma.inscription.entity.Statut.ANNULEE
            """)
    long countActiveInscriptions(@Param("sessionId") Long sessionId);
}
