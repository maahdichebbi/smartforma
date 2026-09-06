package tn.esprit.smartforma.catalogue.dto;

import jakarta.validation.constraints.*;
import tn.esprit.smartforma.catalogue.entity.Niveau;

import java.math.BigDecimal;

/**
 * DTO for creating and updating a Formation.
 * The categorieId field is how the client references a category — the service resolves it to a Categorie entity.
 */
public record FormationDto(

        @NotBlank(message = "Le titre est obligatoire")
        String titre,

        @NotBlank(message = "La description est obligatoire")
        String description,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif")
        BigDecimal prix,

        @NotNull(message = "Le niveau est obligatoire")
        Niveau niveau,

        @NotNull(message = "La durée est obligatoire")
        @Min(value = 1, message = "La durée doit être au moins 1 heure")
        Integer dureeHeures,

        @NotNull(message = "La catégorie est obligatoire")
        Long categorieId,

        /** Optional comma-separated tags or keywords (e.g. "java, spring boot, rest") */
        String tags
) {}
