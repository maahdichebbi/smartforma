package tn.esprit.smartforma.inscription.entity;

/**
 * Represents the status of a registration (inscription).
 *
 * EN_ATTENTE  — Default status when the learner first registers.
 *               The seat is reserved immediately (counts against capacity).
 *
 * CONFIRMEE   — An administrator has confirmed the registration.
 *
 * ANNULEE     — The registration has been cancelled (by learner or admin).
 *               A cancelled registration releases the reserved seat.
 */
public enum Statut {
    EN_ATTENTE,
    CONFIRMEE,
    ANNULEE
}
