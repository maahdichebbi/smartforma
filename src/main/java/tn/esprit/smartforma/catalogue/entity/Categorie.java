package tn.esprit.smartforma.catalogue.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a training category (e.g. "IT", "Management", "Design").
 * A category groups related formations together.
 */
@Entity
@Table(name = "categorie")
@Getter
@Setter
@NoArgsConstructor
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * A category can contain many formations.
     * We do NOT cascade delete — deleting a category that still has formations
     * is rejected at the service level with a clear error message.
     */
    @OneToMany(mappedBy = "categorie", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Formation> formations = new ArrayList<>();
}
