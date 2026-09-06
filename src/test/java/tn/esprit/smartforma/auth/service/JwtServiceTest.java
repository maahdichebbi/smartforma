package tn.esprit.smartforma.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.auth.entity.Compte;
import tn.esprit.smartforma.auth.entity.Role;
import tn.esprit.smartforma.exception.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "SmartFormaDevJwtSecretKeyThatIsAtLeast32CharsLong!",
            3_600_000L
    );

    @Test
    @DisplayName("Generated JWT contains compte id, email, role and apprenantId")
    void generateAndParseLearnerToken() {
        Apprenant apprenant = new Apprenant();
        apprenant.setId(42L);

        Compte compte = new Compte();
        compte.setId(7L);
        compte.setEmail("alice@test.com");
        compte.setRole(Role.LEARNER);
        compte.setApprenant(apprenant);

        String token = jwtService.generateToken(compte);
        var claims = jwtService.parseClaims(token);

        assertThat(token).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("email", String.class)).isEqualTo("alice@test.com");
        assertThat(claims.get("role", String.class)).isEqualTo("LEARNER");
        assertThat(((Number) claims.get("apprenantId")).longValue()).isEqualTo(42L);
    }

    @Test
    @DisplayName("ADMIN token has no apprenantId claim")
    void generateAdminTokenWithoutApprenant() {
        Compte admin = new Compte();
        admin.setId(1L);
        admin.setEmail("admin@smartforma.local");
        admin.setRole(Role.ADMIN);

        var claims = jwtService.parseClaims(jwtService.generateToken(admin));

        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.get("apprenantId")).isNull();
    }

    @Test
    @DisplayName("Tampered token is rejected")
    void parseRejectsInvalidToken() {
        assertThatThrownBy(() -> jwtService.parseClaims("not-a-jwt"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
