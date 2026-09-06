package tn.esprit.smartforma.apprenant.dto;
 
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import tn.esprit.smartforma.catalogue.entity.Niveau;

/**
 * DTO for creating and updating an Apprenant.
 */
public record ApprenantDto(

        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        /** Optional — comma-separated list of skills, e.g. "Java, SQL, Spring Boot" */
        String competences,

        /** Optional — comma-separated list of interests, e.g. "Backend, Microservices" */
        String interets,

        /** Optional — learner proficiency level (defaults to DEBUTANT if null) */
        Niveau niveau
) {}
