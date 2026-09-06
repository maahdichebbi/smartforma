package tn.esprit.smartforma.apprenant.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.smartforma.catalogue.entity.Niveau;

/**
 * Represents a learner (apprenant) in the platform.
 *
 * Intentionally minimal for now — contains only what is needed for:
 *   1. Registering for sessions (Inscription module)
 *   2. MLA recommendations later (competences + interets fields)
 *
 * Login credentials live on Compte (AUTH), not on this entity.
 * Do NOT confuse this with a full "Gestion des Utilisateurs" module.
 */
@Entity
@Table(name = "apprenant")
@Getter
@Setter
@NoArgsConstructor
public class Apprenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Column(nullable = false)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Column(nullable = false)
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Learner seniority / proficiency level (DEBUTANT, INTERMEDIAIRE, AVANCE).
     * Used by the MLA recommendation engine to compute level compatibility.
     * Defaults to DEBUTANT.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Niveau niveau = Niveau.DEBUTANT;

    /**
     * Comma-separated list of skills (e.g. "Java,SQL,Spring Boot").
     * Intentionally simple — will be used by the MLA module later.
     * Example: "Java, SQL, OOP"
     */
    @Column(columnDefinition = "TEXT")
    private String competences;

    /**
     * Comma-separated list of interests (e.g. "Backend,Web Development,Microservices").
     * Intentionally simple — will be used by the MLA module later.
     */
    @Column(columnDefinition = "TEXT")
    private String interets;
}
