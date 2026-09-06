package tn.esprit.smartforma.inscription.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartforma.inscription.dto.SessionDto;
import tn.esprit.smartforma.inscription.entity.Session;
import tn.esprit.smartforma.inscription.service.SessionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /** List all sessions. */
    @GetMapping
    public List<Session> findAll() {
        return sessionService.findAll();
    }

    /** Get a single session by ID. */
    @GetMapping("/{id}")
    public Session findById(@PathVariable Long id) {
        return sessionService.findById(id);
    }

    /**
     * List sessions that have not yet started — open for registration.
     * GET /api/v1/sessions/disponibles
     */
    @GetMapping("/disponibles")
    public List<Session> findAvailable() {
        return sessionService.findAvailable();
    }

    /**
     * List all sessions for a specific formation.
     * GET /api/v1/sessions/formation/{formationId}
     */
    @GetMapping("/formation/{formationId}")
    public List<Session> findByFormation(@PathVariable Long formationId) {
        return sessionService.findByFormation(formationId);
    }

    /**
     * List available (future) sessions for a specific formation.
     * GET /api/v1/sessions/formation/{formationId}/disponibles
     */
    @GetMapping("/formation/{formationId}/disponibles")
    public List<Session> findAvailableByFormation(@PathVariable Long formationId) {
        return sessionService.findAvailableByFormation(formationId);
    }

    /** Create a new session. */
    @PostMapping
    public ResponseEntity<Session> create(@Valid @RequestBody SessionDto dto) {
        Session created = sessionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Update an existing session. */
    @PutMapping("/{id}")
    public Session update(@PathVariable Long id, @Valid @RequestBody SessionDto dto) {
        return sessionService.update(id, dto);
    }

    /** Delete a session (only if it has no active inscriptions). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sessionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
