package tn.esprit.smartforma.catalogue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating and updating a Chapitre.
 * The formationId comes from the URL path, not from the request body.
 */
public record ChapitreDto(

        @NotBlank(message = "Le titre du chapitre est obligatoire")
        String titre,

        String description,

        @NotNull(message = "L'ordre du chapitre est obligatoire")
        @Min(value = 1, message = "L'ordre doit être au moins 1")
        Integer ordre
) {}
