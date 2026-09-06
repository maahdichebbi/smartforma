package tn.esprit.smartforma.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.apprenant.repository.ApprenantRepository;
import tn.esprit.smartforma.auth.dto.AuthResponse;
import tn.esprit.smartforma.auth.dto.LoginRequest;
import tn.esprit.smartforma.auth.dto.RegisterRequest;
import tn.esprit.smartforma.auth.entity.Compte;
import tn.esprit.smartforma.auth.entity.Role;
import tn.esprit.smartforma.auth.repository.CompteRepository;
import tn.esprit.smartforma.catalogue.entity.Niveau;
import tn.esprit.smartforma.exception.UnauthorizedException;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final CompteRepository compteRepository;
    private final ApprenantRepository apprenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (compteRepository.existsByEmailIgnoreCase(email) || apprenantRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Un compte avec l'email '" + email + "' existe déjà.");
        }

        Apprenant apprenant = new Apprenant();
        apprenant.setNom(request.nom().trim());
        apprenant.setPrenom(request.prenom().trim());
        apprenant.setEmail(email);
        apprenant.setCompetences(request.competences());
        apprenant.setInterets(request.interets());
        apprenant.setNiveau(request.niveau() != null ? request.niveau() : Niveau.DEBUTANT);
        apprenant = apprenantRepository.save(apprenant);

        Compte compte = new Compte();
        compte.setEmail(email);
        compte.setPasswordHash(passwordEncoder.encode(request.password()));
        compte.setRole(Role.LEARNER);
        compte.setEnabled(true);
        compte.setApprenant(apprenant);
        compte = compteRepository.save(compte);

        return toAuthResponse(compte);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        Compte compte = compteRepository.findByEmailIgnoreCaseWithApprenant(email)
                .orElseThrow(() -> new UnauthorizedException("Email ou mot de passe incorrect."));

        if (!compte.isEnabled()) {
            throw new UnauthorizedException("Ce compte est désactivé.");
        }

        if (!passwordEncoder.matches(request.password(), compte.getPasswordHash())) {
            throw new UnauthorizedException("Email ou mot de passe incorrect.");
        }

        return toAuthResponse(compte);
    }

    @Transactional(readOnly = true)
    public AuthResponse me(Long compteId) {
        Compte compte = compteRepository.findByIdWithApprenant(compteId)
                .orElseThrow(() -> new UnauthorizedException("Authentification requise."));
        if (!compte.isEnabled()) {
            throw new UnauthorizedException("Ce compte est désactivé.");
        }
        return toAuthResponse(compte);
    }

    private AuthResponse toAuthResponse(Compte compte) {
        Apprenant apprenant = compte.getApprenant();
        return AuthResponse.bearer(
                jwtService.generateToken(compte),
                compte.getId(),
                compte.getEmail(),
                compte.getRole(),
                apprenant != null ? apprenant.getId() : null,
                apprenant != null ? apprenant.getNom() : null,
                apprenant != null ? apprenant.getPrenom() : null
        );
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }
}
