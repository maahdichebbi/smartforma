package tn.esprit.smartforma.inscription.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.smartforma.catalogue.entity.Formation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a scheduled occurrence of a Formation.
 *
 * A Session answers the question: "When and where can a learner attend Formation X?"
 * One Formation can have multiple Sessions (e.g. January cohort, March cohort...).
 */
@Entity
@Table(name = "session")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"nombreInscrits", "disponible", "inscriptions"})
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The formation this session is for.
     * We show only the basic formation fields in JSON — no chapitres, no full categorie graph.
     */
    @NotNull(message = "La formation est obligatoire")
    @ManyToOne(optional = false)
    @JoinColumn(name = "formation_id", nullable = false)
    @JsonIgnoreProperties({"chapitres", "categorie"})
    private Formation formation;

    @NotNull(message = "La date de début est obligatoire")
    @Column(nullable = false)
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    @Column(nullable = false)
    private LocalDate dateFin;

    /** Optional start time (e.g. 09:00). Null means no specific time constraint. */
    @Column
    private LocalTime heureDebut;

    /** Optional end time (e.g. 17:00). Null means no specific time constraint. */
    @Column
    private LocalTime heureFin;

    @NotNull(message = "La capacité est obligatoire")
    @Min(value = 1, message = "La capacité doit être au moins 1")
    @Column(nullable = false)
    private Integer capacite;

    /**
     * Inscriptions are owned by Session.
     * Hidden from JSON to avoid infinite recursion (Session→Inscription→Session...).
     * Use the dedicated inscription endpoints to retrieve them.
     */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Inscription> inscriptions = new ArrayList<>();

    // ── Computed helper (not persisted) ──────────────────────────────────────

    /**
     * Returns the number of ACTIVE (non-cancelled) inscriptions for this session.
     * Used to check capacity without an extra database query when the entity is already loaded.
     */
    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getNombreInscrits() {
        return (int) inscriptions.stream()

                .filter(i -> i.getStatut() != Statut.ANNULEE)
                .count();
    }

    /** True if this session has not yet started (based on today's date). */
    @Transient
    public boolean isDisponible() {
        return LocalDate.now().isBefore(dateDebut);
    }
}
