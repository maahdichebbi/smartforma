package tn.esprit.smartforma.inscription.dto;

public record SessionStatsDto(
        Long sessionId,
        int capacite,
        long placesOccupees,
        long placesDisponibles,
        long nombreEnAttente,
        boolean estComplete
) {}
