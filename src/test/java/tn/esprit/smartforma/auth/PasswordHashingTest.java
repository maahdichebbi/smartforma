package tn.esprit.smartforma.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashingTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("BCrypt hashes a password and matches the original, not the raw value")
    void bcryptHashesAndMatches() {
        String raw = "MotDePasseFort123!";

        String hash = encoder.encode(raw);

        assertThat(hash).isNotBlank();
        assertThat(hash).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, hash)).isTrue();
        assertThat(encoder.matches("wrong-password", hash)).isFalse();
    }
}
