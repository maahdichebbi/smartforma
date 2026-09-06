package tn.esprit.smartforma.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.smartforma.apprenant.entity.Apprenant;

/**
 * Authenticated account. Separate from {@link Apprenant} so ADMIN can exist
 * without a learner profile, and learner profile fields stay pedagogical.
 */
@Entity
@Table(name = "compte")
@Getter
@Setter
@NoArgsConstructor
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Present for LEARNER accounts; null for ADMIN.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apprenant_id", unique = true)
    private Apprenant apprenant;
}
