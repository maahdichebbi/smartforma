package tn.esprit.smartforma.inscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartforma.auth.security.CurrentUserService;
import tn.esprit.smartforma.inscription.dto.InscriptionDto;
import tn.esprit.smartforma.inscription.entity.Inscription;
import tn.esprit.smartforma.inscription.service.InscriptionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inscriptions")
@RequiredArgsConstructor
public class InscriptionController {

    private final InscriptionService inscriptionService;
    private final CurrentUserService currentUserService;

    /** List all inscriptions (admin view). */
    @GetMapping
    public List<Inscription> findAll() {
        return inscriptionService.findAll();
    }

    /** Lists registrations for the learner identified by the JWT. */
    @GetMapping("/me")
    public List<Inscription> findMine() {
        return inscriptionService.findByApprenant(currentUserService.requireApprenantId());
    }

    /** Get a single inscription by ID. */
    @GetMapping("/{id}")
    public Inscription findById(@PathVariable Long id) {
        Inscription inscription = inscriptionService.findById(id);
        currentUserService.assertCanAccessApprenant(inscription.getApprenant().getId());
        return inscription;
    }

    /**
     * List all inscriptions for a specific learner — "Mes inscriptions" page.
     * LEARNER may only query their own id; ADMIN may query any learner.
     */
    @GetMapping("/apprenant/{apprenantId}")
    public List<Inscription> findByApprenant(@PathVariable Long apprenantId) {
        currentUserService.assertCanAccessApprenant(apprenantId);
        return inscriptionService.findByApprenant(apprenantId);
    }

    /**
     * List all inscriptions for a specific session (admin view).
     * GET /api/v1/inscriptions/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    public List<Inscription> findBySession(@PathVariable Long sessionId) {
        return inscriptionService.findBySession(sessionId);
    }

    /**
     * Register the authenticated learner for a session.
     * The apprenant id is taken from the JWT, never from the request body.
     */
    @PostMapping("/session/{sessionId}")
    public ResponseEntity<Inscription> inscrire(@PathVariable Long sessionId) {
        Long apprenantId = currentUserService.requireApprenantId();
        Inscription created = inscriptionService.inscrire(sessionId, new InscriptionDto(apprenantId));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Cancel a registration — releases the seat.
     * PATCH is semantically correct here: we are partially updating (only statut changes).
     *
     * PATCH /api/v1/inscriptions/{id}/annuler
     */
    @PatchMapping("/{id}/annuler")
    public Inscription annuler(@PathVariable Long id) {
        Inscription inscription = inscriptionService.findById(id);
        currentUserService.assertCanAccessApprenant(inscription.getApprenant().getId());
        return inscriptionService.annuler(id);
    }

    /**
     * Confirm a registration (admin action).
     * PATCH /api/v1/inscriptions/{id}/confirmer
     */
    @PatchMapping("/{id}/confirmer")
    public Inscription confirmer(@PathVariable Long id) {
        return inscriptionService.confirmer(id);
    }

    /**
     * Get the waiting list queue for a session (FIFO).
     * GET /api/v1/inscriptions/session/{sessionId}/attente
     */
    @GetMapping("/session/{sessionId}/attente")
    public List<Inscription> getWaitingList(@PathVariable Long sessionId) {
        return inscriptionService.getWaitingList(sessionId);
    }

    /**
     * Get real-time stats for a session (capacity, confirmed, remaining, waiting).
     * GET /api/v1/inscriptions/session/{sessionId}/stats
     */
    @GetMapping("/session/{sessionId}/stats")
    public tn.esprit.smartforma.inscription.dto.SessionStatsDto getSessionStats(@PathVariable Long sessionId) {
        return inscriptionService.getSessionStats(sessionId);
    }
}
