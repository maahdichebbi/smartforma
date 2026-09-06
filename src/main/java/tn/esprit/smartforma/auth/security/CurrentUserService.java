package tn.esprit.smartforma.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tn.esprit.smartforma.exception.ForbiddenException;
import tn.esprit.smartforma.exception.UnauthorizedException;

/**
 * Reads the authenticated Compte from the SecurityContext.
 * Learner-scoped operations must use requireApprenantId() rather than a client-supplied ID.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserService {

    public ComptePrincipal requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ComptePrincipal principal)) {
            throw new UnauthorizedException("Authentification requise.");
        }
        return principal;
    }

    public Long requireApprenantId() {
        ComptePrincipal principal = requireUser();
        if (principal.getApprenantId() == null) {
            throw new ForbiddenException("Cette action est réservée aux apprenants.");
        }
        return principal.getApprenantId();
    }

    /**
     * ADMIN may access any learner profile. LEARNER may access only their own.
     */
    public void assertCanAccessApprenant(Long apprenantId) {
        ComptePrincipal principal = requireUser();
        if (principal.isAdmin()) {
            return;
        }
        if (principal.getApprenantId() == null || !principal.getApprenantId().equals(apprenantId)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à accéder à cette ressource.");
        }
    }
}
