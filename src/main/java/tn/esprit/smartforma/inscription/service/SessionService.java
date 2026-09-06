package tn.esprit.smartforma.inscription.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartforma.catalogue.entity.Formation;
import tn.esprit.smartforma.catalogue.repository.FormationRepository;
import tn.esprit.smartforma.exception.ResourceNotFoundException;
import tn.esprit.smartforma.inscription.dto.SessionDto;
import tn.esprit.smartforma.inscription.entity.Session;
import tn.esprit.smartforma.inscription.repository.SessionRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final FormationRepository formationRepository;

    // ── Read operations ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Session> findAll() {
        return sessionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Session findById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", id));
    }

    /** All sessions for a given formation. */
    @Transactional(readOnly = true)
    public List<Session> findByFormation(Long formationId) {
        if (!formationRepository.existsById(formationId)) {
            throw new ResourceNotFoundException("Formation", formationId);
        }
        return sessionRepository.findByFormationIdOrderByDateDebutAsc(formationId);
    }

    /** Sessions that have not yet started — open for registration. */
    @Transactional(readOnly = true)
    public List<Session> findAvailable() {
        return sessionRepository.findByDateDebutAfterOrderByDateDebutAsc(LocalDate.now());
    }

    /** Available sessions for a specific formation. */
    @Transactional(readOnly = true)
    public List<Session> findAvailableByFormation(Long formationId) {
        if (!formationRepository.existsById(formationId)) {
            throw new ResourceNotFoundException("Formation", formationId);
        }
        return sessionRepository.findByFormationIdAndDateDebutAfterOrderByDateDebutAsc(
                formationId, LocalDate.now()
        );
    }

    // ── Write operations ───────────────────────────────────────────────────────

    public Session create(SessionDto dto) {
        Formation formation = formationRepository.findById(dto.formationId())
                .orElseThrow(() -> new ResourceNotFoundException("Formation", dto.formationId()));

        validateSessionDates(dto, null);

        Session session = new Session();
        mapDtoToEntity(dto, session, formation);
        return sessionRepository.save(session);
    }

    public Session update(Long id, SessionDto dto) {
        Session session = findById(id);

        Formation formation = formationRepository.findById(dto.formationId())
                .orElseThrow(() -> new ResourceNotFoundException("Formation", dto.formationId()));

        validateSessionDates(dto, session);

        mapDtoToEntity(dto, session, formation);
        return sessionRepository.save(session);
    }

    public void delete(Long id) {
        Session session = findById(id);

        // Business rule: cannot delete a session that already has active inscriptions
        long activeCount = sessionRepository.countActiveInscriptions(id);
        if (activeCount > 0) {
            throw new IllegalArgumentException(
                    "Impossible de supprimer cette session : elle contient " + activeCount + " inscription(s) active(s)."
            );
        }

        sessionRepository.delete(session);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void validateSessionDates(SessionDto dto, Session existingSession) {
        // Business rule: end date must be after start date
        if (!dto.dateFin().isAfter(dto.dateDebut())) {
            throw new IllegalArgumentException(
                    "La date de fin doit être après la date de début."
            );
        }

        // Business rule: start date cannot be in the past (only for new sessions)
        if (existingSession == null && dto.dateDebut().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La date de début ne peut pas être dans le passé."
            );
        }

        // Business rule: if times are provided, end time must be after start time (same-day logic)
        if (dto.heureDebut() != null && dto.heureFin() != null) {
            if (!dto.heureFin().isAfter(dto.heureDebut())) {
                throw new IllegalArgumentException(
                        "L'heure de fin doit être après l'heure de début."
                );
            }
        }
    }

    private void mapDtoToEntity(SessionDto dto, Session session, Formation formation) {
        session.setFormation(formation);
        session.setDateDebut(dto.dateDebut());
        session.setDateFin(dto.dateFin());
        session.setHeureDebut(dto.heureDebut());
        session.setHeureFin(dto.heureFin());
        session.setCapacite(dto.capacite());
    }
}
