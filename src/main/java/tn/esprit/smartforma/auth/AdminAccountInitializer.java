package tn.esprit.smartforma.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.esprit.smartforma.auth.entity.Compte;
import tn.esprit.smartforma.auth.entity.Role;
import tn.esprit.smartforma.auth.repository.CompteRepository;

/**
 * Creates a single ADMIN compte on startup if it does not already exist.
 * There is no public admin registration endpoint.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer implements CommandLineRunner {

    private final CompteRepository compteRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${smartforma.admin.email}")
    private String adminEmail;

    @Value("${smartforma.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        String email = adminEmail.toLowerCase().trim();
        if (compteRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        Compte admin = new Compte();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        admin.setApprenant(null);
        compteRepository.save(admin);
        log.info("Compte ADMIN initialisé : {}", email);
    }
}
