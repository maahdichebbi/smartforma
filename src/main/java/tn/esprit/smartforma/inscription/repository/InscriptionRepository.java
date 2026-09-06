package tn.esprit.smartforma.inscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.smartforma.inscription.entity.Inscription;
import tn.esprit.smartforma.inscription.entity.Statut;

import java.util.List;
import java.util.Optional;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

    /** All inscriptions for a given learner, ordered by inscription date descending. */
    List<Inscription> findByApprenantIdOrderByDateInscriptionDesc(Long apprenantId);

    /** All inscriptions for a given session. */
    List<Inscription> findBySessionId(Long sessionId);

    /** Find all inscriptions for a session with a specific status, ordered by queue position. */
    List<Inscription> findBySessionIdAndStatutOrderByPositionFileAsc(Long sessionId, Statut statut);

    /** Find all inscriptions for a session with a specific status, ordered by timestamp (FIFO). */
    List<Inscription> findBySessionIdAndStatutOrderByDateHeureInscriptionAsc(Long sessionId, Statut statut);

    /** Check if a learner already has an inscription (any status) for a session. */
    boolean existsByApprenantIdAndSessionId(Long apprenantId, Long sessionId);

    /** Find a learner's specific inscription for a session. */
    Optional<Inscription> findByApprenantIdAndSessionId(Long apprenantId, Long sessionId);

    /** Count inscriptions for a session with a specific status (e.g. CONFIRMEE or EN_ATTENTE). */
    long countBySessionIdAndStatut(Long sessionId, Statut statut);

    /** Count inscriptions for a session with status not equal to specified status. */
    long countBySessionIdAndStatutNot(Long sessionId, Statut statut);

    /** Find the first waiting learner in the queue for a session (FIFO). */
    Optional<Inscription> findFirstBySessionIdAndStatutOrderByPositionFileAsc(Long sessionId, Statut statut);

    /** Fallback find first waiting learner by ID (creation order) if position is unset. */
    Optional<Inscription> findFirstBySessionIdAndStatutOrderByIdAsc(Long sessionId, Statut statut);
}
