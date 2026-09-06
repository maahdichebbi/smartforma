package tn.esprit.smartforma.inscription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating and updating a Session.
 *
 * Date validation note:
 *   We validate that dateFin is after dateDebut at the service level,
 *   because @AssertTrue and cross-field validation on records is not straightforward.
 *   The service produces a clear error message for this case.
 */
public record SessionDto(

        @NotNull(message = "La formation est obligatoire")
        Long formationId,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin,

        /** Optional — session start time (e.g. 09:00) */
        LocalTime heureDebut,

        /** Optional — session end time (e.g. 17:00) */
        LocalTime heureFin,

        @NotNull(message = "La capacité est obligatoire")
        @Min(value = 1, message = "La capacité doit être au moins 1")
        Integer capacite
) {}
