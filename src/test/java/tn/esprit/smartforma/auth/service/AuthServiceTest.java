package tn.esprit.smartforma.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private ApprenantRepository apprenantRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("SmartFormaDevJwtSecretKeyThatIsAtLeast32CharsLong!", 3_600_000L);
        authService = new AuthService(compteRepository, apprenantRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Public registration creates a LEARNER compte linked to an Apprenant")
    void registerCreatesLearnerAccount() {
        when(compteRepository.existsByEmailIgnoreCase("alice@test.com")).thenReturn(false);
        when(apprenantRepository.existsByEmailIgnoreCase("alice@test.com")).thenReturn(false);
        when(apprenantRepository.save(any(Apprenant.class))).thenAnswer(invocation -> {
            Apprenant a = invocation.getArgument(0);
            a.setId(10L);
            return a;
        });
        when(compteRepository.save(any(Compte.class))).thenAnswer(invocation -> {
            Compte c = invocation.getArgument(0);
            c.setId(5L);
            return c;
        });

        RegisterRequest request = new RegisterRequest(
                "Dupont", "Alice", "Alice@test.com", "password123",
                "Java", "Backend", Niveau.INTERMEDIAIRE
        );

        AuthResponse response = authService.register(request);

        assertThat(response.role()).isEqualTo(Role.LEARNER);
        assertThat(response.email()).isEqualTo("alice@test.com");
        assertThat(response.compteId()).isEqualTo(5L);
        assertThat(response.apprenantId()).isEqualTo(10L);
        assertThat(response.nom()).isEqualTo("Dupont");
        assertThat(response.prenom()).isEqualTo("Alice");
        assertThat(response.token()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");

        var claims = jwtService.parseClaims(response.token());
        assertThat(claims.get("role", String.class)).isEqualTo("LEARNER");
        assertThat(claims.getSubject()).isEqualTo("5");
    }

    @Test
    @DisplayName("Registration rejects a duplicate email")
    void registerRejectsDuplicateEmail() {
        when(compteRepository.existsByEmailIgnoreCase("alice@test.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "Dupont", "Alice", "alice@test.com", "password123",
                null, null, null
        );

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    @DisplayName("Login returns a JWT for a valid LEARNER")
    void loginSuccess() {
        Apprenant apprenant = new Apprenant();
        apprenant.setId(10L);
        apprenant.setNom("Dupont");
        apprenant.setPrenom("Alice");

        Compte compte = new Compte();
        compte.setId(5L);
        compte.setEmail("alice@test.com");
        compte.setPasswordHash(passwordEncoder.encode("password123"));
        compte.setRole(Role.LEARNER);
        compte.setEnabled(true);
        compte.setApprenant(apprenant);

        when(compteRepository.findByEmailIgnoreCaseWithApprenant("alice@test.com"))
                .thenReturn(Optional.of(compte));

        AuthResponse response = authService.login(new LoginRequest("alice@test.com", "password123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.role()).isEqualTo(Role.LEARNER);
        assertThat(response.apprenantId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Login fails with wrong password")
    void loginRejectsBadPassword() {
        Compte compte = new Compte();
        compte.setId(5L);
        compte.setEmail("alice@test.com");
        compte.setPasswordHash(passwordEncoder.encode("password123"));
        compte.setRole(Role.LEARNER);
        compte.setEnabled(true);

        when(compteRepository.findByEmailIgnoreCaseWithApprenant("alice@test.com"))
                .thenReturn(Optional.of(compte));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@test.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Login fails for an unknown email")
    void loginRejectsUnknownEmail() {
        when(compteRepository.findByEmailIgnoreCaseWithApprenant("nobody@test.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@test.com", "password123")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
