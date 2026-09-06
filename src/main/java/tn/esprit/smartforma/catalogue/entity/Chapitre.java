package tn.esprit.smartforma.catalogue.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents one chapter (section) inside a formation.
 * Chapters are ordered by their 'ordre' field and have no meaning outside their formation.
 */
@Entity
@Table(name = "chapitre")
@Getter
@Setter
@NoArgsConstructor
public class Chapitre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le titre du chapitre est obligatoire")
    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "L'ordre du chapitre est obligatoire")
    @Min(value = 1, message = "L'ordre doit être au moins 1")
    @Column(nullable = false)
    private Integer ordre;

    /**
     * The formation this chapter belongs to.
     * Cannot be null — a chapter has no meaning without its formation.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "formation_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("chapitres")
    private Formation formation;
}
