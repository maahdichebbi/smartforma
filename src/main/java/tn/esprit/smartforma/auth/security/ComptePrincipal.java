package tn.esprit.smartforma.auth.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tn.esprit.smartforma.auth.entity.Compte;
import tn.esprit.smartforma.auth.entity.Role;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated principal stored in the SecurityContext after JWT validation.
 */
@Getter
public class ComptePrincipal implements UserDetails {

    private final Long compteId;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;
    private final Long apprenantId;

    public ComptePrincipal(Compte compte) {
        this.compteId = compte.getId();
        this.email = compte.getEmail();
        this.passwordHash = compte.getPasswordHash();
        this.role = compte.getRole();
        this.enabled = compte.isEnabled();
        this.apprenantId = compte.getApprenant() != null ? compte.getApprenant().getId() : null;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isLearner() {
        return role == Role.LEARNER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
