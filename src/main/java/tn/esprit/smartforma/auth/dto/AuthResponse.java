package tn.esprit.smartforma.auth.dto;

import tn.esprit.smartforma.auth.entity.Role;

public record AuthResponse(
        String token,
        String tokenType,
        Long compteId,
        String email,
        Role role,
        Long apprenantId,
        String nom,
        String prenom
) {
    public static AuthResponse bearer(
            String token,
            Long compteId,
            String email,
            Role role,
            Long apprenantId,
            String nom,
            String prenom
    ) {
        return new AuthResponse(token, "Bearer", compteId, email, role, apprenantId, nom, prenom);
    }
}
