package tn.esprit.smartforma.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tn.esprit.smartforma.catalogue.entity.Niveau;

/**
 * Public registration payload. Role is never accepted from the client:
 * this always creates a LEARNER account.
 */
public record RegisterRequest(

        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password,

        String competences,

        String interets,

        Niveau niveau
) {}
