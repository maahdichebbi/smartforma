package tn.esprit.smartforma.catalogue.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating and updating a Categorie.
 * We use a Java record — immutable, no boilerplate needed.
 */
public record CategorieDto(

        @NotBlank(message = "Le nom de la catégorie est obligatoire")
        String nom,

        String description
) {}
