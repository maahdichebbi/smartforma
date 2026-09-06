package tn.esprit.smartforma.inscription.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for registering a learner in a session.
 * The sessionId comes from the URL path in the controller,
 * so only the apprenantId is needed in the request body.
 */
public record InscriptionDto(

        @NotNull(message = "L'identifiant de l'apprenant est obligatoire")
        Long apprenantId
) {}
