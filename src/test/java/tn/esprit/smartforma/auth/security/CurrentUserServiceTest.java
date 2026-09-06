package tn.esprit.smartforma.auth.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.auth.entity.Compte;
import tn.esprit.smartforma.auth.entity.Role;
import tn.esprit.smartforma.exception.ForbiddenException;
import tn.esprit.smartforma.exception.UnauthorizedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserServiceTest {

    private final CurrentUserService currentUserService = new CurrentUserService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Learner can only access their own apprenant id")
    void learnerCannotAccessAnotherProfile() {
        setAuthentication(learnerPrincipal(10L));

        assertThat(currentUserService.requireApprenantId()).isEqualTo(10L);
        currentUserService.assertCanAccessApprenant(10L);

        assertThatThrownBy(() -> currentUserService.assertCanAccessApprenant(99L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Admin can access any apprenant id")
    void adminCanAccessAnyLearner() {
        setAuthentication(adminPrincipal());

        currentUserService.assertCanAccessApprenant(99L);
        assertThatThrownBy(currentUserService::requireApprenantId)
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Missing authentication is rejected")
    void missingAuthentication() {
        assertThatThrownBy(currentUserService::requireUser)
                .isInstanceOf(UnauthorizedException.class);
    }

    private void setAuthentication(ComptePrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private ComptePrincipal learnerPrincipal(Long apprenantId) {
        Apprenant apprenant = new Apprenant();
        apprenant.setId(apprenantId);

        Compte compte = new Compte();
        compte.setId(1L);
        compte.setEmail("alice@test.com");
        compte.setPasswordHash("hash");
        compte.setRole(Role.LEARNER);
        compte.setEnabled(true);
        compte.setApprenant(apprenant);
        return new ComptePrincipal(compte);
    }

    private ComptePrincipal adminPrincipal() {
        Compte compte = new Compte();
        compte.setId(2L);
        compte.setEmail("admin@smartforma.local");
        compte.setPasswordHash("hash");
        compte.setRole(Role.ADMIN);
        compte.setEnabled(true);
        return new ComptePrincipal(compte);
    }
}
