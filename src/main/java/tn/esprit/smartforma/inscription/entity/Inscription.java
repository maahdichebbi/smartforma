package tn.esprit.smartforma.inscription.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.smartforma.apprenant.entity.Apprenant;

import java.time.LocalDate;

/**
 * Represents a learner's registration for a specific Session.
 *
 * Business rules enforced at the service layer:
 *   - A learner cannot register twice for the same session.
 *   - A learner cannot register if the session is full.
 *   - A learner cannot register for a session that has already started.
 *   - Cancelling an active registration releases a seat.
 */
@Entity
@Table(name = "inscription")
// Note: no DB-level unique constraint on (apprenant_id, session_id) intentionally.
// A learner who cancelled a registration must be able to re-register.
// The service layer checks for ACTIVE (non-cancelled) inscriptions only.
@Getter
@Setter
@NoArgsConstructor
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The learner who registered.
     * We show basic apprenant info but not their full inscription list (avoids recursion).
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "apprenant_id", nullable = false)
    @JsonIgnoreProperties({"inscriptions"})
    private Apprenant apprenant;

    /**
     * The session the learner registered for.
     * We show basic session info but not its inscriptions list (avoids recursion).
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnoreProperties({"inscriptions"})
    private Session session;

    /** Automatically set to today's date when the inscription is created. */
    @Column(nullable = false)
    private LocalDate dateInscription;

    /** Timestamp for precise FIFO ordering of waiting lists. */
    @Column
    private java.time.LocalDateTime dateHeureInscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut;

    /**
     * 1-based queue position when statut == EN_ATTENTE.
     * Null when statut is CONFIRMEE or ANNULEE.
     */
    @Column
    private Integer positionFile;

    /** Informational message returned in API responses (e.g. status details or promotion notification). */
    @Transient
    private String message;

    /** Informational message explaining automatic promotion when an active registration is cancelled. */
    @Transient
    private String messagePromotion;

    @PrePersist
    protected void onCreate() {
        if (this.dateInscription == null) {
            this.dateInscription = LocalDate.now();
        }
        if (this.dateHeureInscription == null) {
            this.dateHeureInscription = java.time.LocalDateTime.now();
        }
    }
}
