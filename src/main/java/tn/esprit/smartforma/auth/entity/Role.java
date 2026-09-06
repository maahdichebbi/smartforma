package tn.esprit.smartforma.auth.entity;

/**
 * Application roles. Public registration always creates LEARNER.
 * ADMIN accounts are seeded, never self-registered.
 */
public enum Role {
    ADMIN,
    LEARNER
}
