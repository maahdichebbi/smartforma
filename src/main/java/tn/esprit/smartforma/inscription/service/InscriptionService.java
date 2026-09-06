package tn.esprit.smartforma.inscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.apprenant.repository.ApprenantRepository;
import tn.esprit.smartforma.exception.ResourceNotFoundException;
import tn.esprit.smartforma.inscription.dto.InscriptionDto;
import tn.esprit.smartforma.inscription.dto.SessionStatsDto;
import tn.esprit.smartforma.inscription.entity.Inscription;
import tn.esprit.smartforma.inscription.entity.Session;
import tn.esprit.smartforma.inscription.entity.Statut;
import tn.esprit.smartforma.inscription.repository.InscriptionRepository;
import tn.esprit.smartforma.inscription.repository.SessionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class InscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final SessionRepository sessionRepository;
    private final ApprenantRepository apprenantRepository;

    // ── Read operations ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Inscription> findAll() {
        return inscriptionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Inscription findById(Long id) {
        return inscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription", id));
    }

    /** All registrations for a given learner, most recent first. */
    @Transactional(readOnly = true)
    public List<Inscription> findByApprenant(Long apprenantId) {
        if (!apprenantRepository.existsById(apprenantId)) {
            throw new ResourceNotFoundException("Apprenant", apprenantId);
        }
        return inscriptionRepository.findByApprenantIdOrderByDateInscriptionDesc(apprenantId);
    }

    /** All registrations for a given session. */
    @Transactional(readOnly = true)
    public List<Inscription> findBySession(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        return inscriptionRepository.findBySessionId(sessionId);
    }

    /** Waiting list queue for a session, ordered FIFO by position. */
    @Transactional(readOnly = true)
    public List<Inscription> getWaitingList(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        return inscriptionRepository.findBySessionIdAndStatutOrderByPositionFileAsc(sessionId, Statut.EN_ATTENTE);
    }

    /** Session stats: capacity, confirmed seats, remaining places, and waiting queue count. */
    @Transactional(readOnly = true)
    public SessionStatsDto getSessionStats(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        long confirmed = inscriptionRepository.countBySessionIdAndStatut(sessionId, Statut.CONFIRMEE);
        long waiting = inscriptionRepository.countBySessionIdAndStatut(sessionId, Statut.EN_ATTENTE);
        long remaining = Math.max(0, session.getCapacite() - confirmed);
        boolean isFull = confirmed >= session.getCapacite();

        return new SessionStatsDto(
                sessionId,
                session.getCapacite(),
                confirmed,
                remaining,
                waiting,
                isFull
        );
    }

    // ── Core business operation: register / join waiting list ─────────────────

    /**
     * Registers a learner for a session.
     *
     * Business Rules:
     *   1. Session and Apprenant must exist.
     *   2. Session must not have already started or ended.
     *   3. Learner cannot register if already CONFIRMEE or EN_ATTENTE.
     *   4. If seats available: status = CONFIRMEE.
     *   5. If session is full: status = EN_ATTENTE (joins waiting list at next FIFO position).
     */
    public Inscription inscrire(Long sessionId, InscriptionDto dto) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        Apprenant apprenant = apprenantRepository.findById(dto.apprenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant", dto.apprenantId()));

        // Rule: cannot register for started or completed sessions
        if (!LocalDate.now().isBefore(session.getDateDebut())) {
            throw new IllegalArgumentException(
                    "Impossible de s'inscrire : la session a déjà commencé le " + session.getDateDebut() + "."
            );
        }

        // Rule: duplicate registration checks
        Optional<Inscription> existingOpt = inscriptionRepository
                .findByApprenantIdAndSessionId(dto.apprenantId(), sessionId);

        if (existingOpt.isPresent()) {
            Inscription existing = existingOpt.get();
            if (existing.getStatut() == Statut.CONFIRMEE) {
                throw new IllegalArgumentException(
                        "Cet apprenant a déjà une inscription confirmée à cette session."
                );
            }
            if (existing.getStatut() == Statut.EN_ATTENTE) {
                throw new IllegalArgumentException(
                        "Cet apprenant est déjà sur la liste d'attente de cette session (position #" + existing.getPositionFile() + ")."
                );
            }
            // If previous inscription was ANNULEE, we re-activate it below
        }

        // Capacity check: count only CONFIRMEE registrations
        long confirmedCount = inscriptionRepository.countBySessionIdAndStatut(sessionId, Statut.CONFIRMEE);
        boolean seatsAvailable = confirmedCount < session.getCapacite();

        Inscription inscription = existingOpt
                .filter(e -> e.getStatut() == Statut.ANNULEE)
                .orElseGet(Inscription::new);

        inscription.setApprenant(apprenant);
        inscription.setSession(session);
        inscription.setDateInscription(LocalDate.now());
        inscription.setDateHeureInscription(LocalDateTime.now());

        if (seatsAvailable) {
            inscription.setStatut(Statut.CONFIRMEE);
            inscription.setPositionFile(null);
            inscription.setMessage("Inscription confirmée avec succès.");
        } else {
            // Session is full — place on waiting list (FIFO)
            long waitingCount = inscriptionRepository.countBySessionIdAndStatut(sessionId, Statut.EN_ATTENTE);
            int nextPos = (int) waitingCount + 1;
            inscription.setStatut(Statut.EN_ATTENTE);
            inscription.setPositionFile(nextPos);
            inscription.setMessage(
                    "Session complète (" + session.getCapacite() + "/" + session.getCapacite()
                    + " places). Vous avez été placé sur la liste d'attente en position #" + nextPos + "."
            );
        }

        return inscriptionRepository.save(inscription);
    }

    // ── Core business operation: cancel + automatic promotion ─────────────────

    /**
     * Cancels a registration.
     *
     * If the cancelled registration was CONFIRMEE:
     *   - Seat is released.
     *   - The first learner on the waiting list (FIFO, position #1) is automatically promoted to CONFIRMEE.
     *   - Remaining waiting list queue positions are recalculated.
     *
     * If the cancelled registration was EN_ATTENTE:
     *   - Removed from the waiting list.
     *   - Remaining waiting list queue positions are recalculated.
     */
    public Inscription annuler(Long inscriptionId) {
        Inscription inscription = findById(inscriptionId);

        if (inscription.getStatut() == Statut.ANNULEE) {
            throw new IllegalArgumentException("Cette inscription est déjà annulée.");
        }

        if (!LocalDate.now().isBefore(inscription.getSession().getDateDebut())) {
            throw new IllegalArgumentException(
                    "Impossible d'annuler : la session a déjà commencé le "
                    + inscription.getSession().getDateDebut() + "."
            );
        }

        Statut previousStatut = inscription.getStatut();
        Long sessionId = inscription.getSession().getId();

        inscription.setStatut(Statut.ANNULEE);
        inscription.setPositionFile(null);
        Inscription savedCancelled = inscriptionRepository.save(inscription);

        if (previousStatut == Statut.CONFIRMEE) {
            // A confirmed seat was released! Automatically promote the first eligible waiting learner.
            Optional<Inscription> firstWaitingOpt = inscriptionRepository
                    .findFirstBySessionIdAndStatutOrderByPositionFileAsc(sessionId, Statut.EN_ATTENTE);

            if (firstWaitingOpt.isEmpty()) {
                firstWaitingOpt = inscriptionRepository
                        .findFirstBySessionIdAndStatutOrderByIdAsc(sessionId, Statut.EN_ATTENTE);
            }

            if (firstWaitingOpt.isPresent()) {
                Inscription waiting = firstWaitingOpt.get();
                waiting.setStatut(Statut.CONFIRMEE);
                waiting.setPositionFile(null);
                inscriptionRepository.save(waiting);

                // Update remaining queue positions
                recalculateWaitingPositions(sessionId);

                String promoMsg = "L'apprenant " + waiting.getApprenant().getPrenom() + " "
                        + waiting.getApprenant().getNom() + " (position #1) a été automatiquement promu au statut CONFIRMEE.";
                savedCancelled.setMessagePromotion(promoMsg);
            } else {
                savedCancelled.setMessagePromotion("Inscription annulée. La place est désormais disponible.");
            }
        } else if (previousStatut == Statut.EN_ATTENTE) {
            // A waiting learner left the queue — shift remaining learners up
            recalculateWaitingPositions(sessionId);
            savedCancelled.setMessagePromotion("Inscription en liste d'attente annulée.");
        }

        return savedCancelled;
    }

    // ── Admin operation: manual confirm ───────────────────────────────────────

    /**
     * Confirms an inscription (admin action).
     * If learner is in EN_ATTENTE, verifies capacity before confirming.
     */
    public Inscription confirmer(Long inscriptionId) {
        Inscription inscription = findById(inscriptionId);

        if (inscription.getStatut() == Statut.ANNULEE) {
            throw new IllegalArgumentException("Impossible de confirmer une inscription annulée.");
        }

        if (inscription.getStatut() == Statut.CONFIRMEE) {
            throw new IllegalArgumentException("Cette inscription est déjà confirmée.");
        }

        Long sessionId = inscription.getSession().getId();
        long confirmed = inscriptionRepository.countBySessionIdAndStatut(sessionId, Statut.CONFIRMEE);

        if (confirmed >= inscription.getSession().getCapacite()) {
            throw new IllegalArgumentException(
                    "Impossible de confirmer : la session est complète ("
                    + inscription.getSession().getCapacite() + "/"
                    + inscription.getSession().getCapacite() + " places occupées)."
            );
        }

        inscription.setStatut(Statut.CONFIRMEE);
        inscription.setPositionFile(null);
        Inscription saved = inscriptionRepository.save(inscription);

        recalculateWaitingPositions(sessionId);
        return saved;
    }

    // ── Helper: Recalculate waiting positions (FIFO) ───────────────────────────

    public void recalculateWaitingPositions(Long sessionId) {
        List<Inscription> waitingList = inscriptionRepository
                .findBySessionIdAndStatutOrderByDateHeureInscriptionAsc(sessionId, Statut.EN_ATTENTE);

        int pos = 1;
        for (Inscription w : waitingList) {
            w.setPositionFile(pos++);
            inscriptionRepository.save(w);
        }
    }
}
