package tn.esprit.smartforma.catalogue.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a training course in the catalogue.
 * A formation belongs to one category and contains ordered chapters.
 */
@Entity
@Table(name = "formation")
@Getter
@Setter
@NoArgsConstructor
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    @Column(nullable = false)
    private String titre;

    @NotBlank(message = "La description est obligatoire")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @NotNull(message = "Le niveau est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Niveau niveau;

    @NotNull(message = "La durée est obligatoire")
    @Min(value = 1, message = "La durée doit être au moins 1 heure")
    @Column(nullable = false)
    private Integer dureeHeures;

    /**
     * Optional comma-separated target skills or keywords (e.g. "java, spring boot, rest, microservices").
     * Used by the MLA recommendation engine for feature extraction and similarity matching.
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    /**
     * The category this formation belongs to. Required — a formation must always
     * be associated with a category.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("formations")
    private Categorie categorie;

    /**
     * Chapters are fully owned by the formation.
     * Deleting a formation deletes all its chapters (orphanRemoval = true).
     */
    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("formation")
    private List<Chapitre> chapitres = new ArrayList<>();
}
