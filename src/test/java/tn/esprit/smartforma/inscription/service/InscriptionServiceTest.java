package tn.esprit.smartforma.inscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.apprenant.repository.ApprenantRepository;
import tn.esprit.smartforma.inscription.dto.InscriptionDto;
import tn.esprit.smartforma.inscription.entity.Inscription;
import tn.esprit.smartforma.inscription.entity.Session;
import tn.esprit.smartforma.inscription.entity.Statut;
import tn.esprit.smartforma.inscription.repository.InscriptionRepository;
import tn.esprit.smartforma.inscription.repository.SessionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InscriptionService business rules, waiting list, and automatic promotion.
 */
@ExtendWith(MockitoExtension.class)
class InscriptionServiceTest {

    @Mock
    private InscriptionRepository inscriptionRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ApprenantRepository apprenantRepository;

    @InjectMocks
    private InscriptionService inscriptionService;

    private Session session;
    private Apprenant apprenant1;
    private Apprenant apprenant2;
    private InscriptionDto dto1;

    @BeforeEach
    void setUp() {
        session = new Session();
        session.setId(1L);
        session.setDateDebut(LocalDate.now().plusDays(7));
        session.setDateFin(LocalDate.now().plusDays(14));
        session.setCapacite(2);

        apprenant1 = new Apprenant();
        apprenant1.setId(1L);
        apprenant1.setNom("Dupont");
        apprenant1.setPrenom("Alice");
        apprenant1.setEmail("alice@test.com");

        apprenant2 = new Apprenant();
        apprenant2.setId(2L);
        apprenant2.setNom("Martin");
        apprenant2.setPrenom("Bob");
        apprenant2.setEmail("bob@test.com");

        dto1 = new InscriptionDto(1L);
    }

    // ── 1. Successful direct registration ────────────────────────────────────

    @Test
    @DisplayName("1. Inscription réussie : places disponibles -> statut CONFIRMEE")
    void inscrire_success_directConfirmation() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(apprenantRepository.findById(1L)).thenReturn(Optional.of(apprenant1));
        when(inscriptionRepository.findByApprenantIdAndSessionId(1L, 1L)).thenReturn(Optional.empty());
        when(inscriptionRepository.countBySessionIdAndStatut(1L, Statut.CONFIRMEE)).thenReturn(0L);
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscription result = inscriptionService.inscrire(1L, dto1);

        assertThat(result).isNotNull();
        assertThat(result.getStatut()).isEqualTo(Statut.CONFIRMEE);
        assertThat(result.getPositionFile()).isNull();
    }

    // ── 2. Duplicate active registration rejected ─────────────────────────────

    @Test
    @DisplayName("2. Rejet doublon : l'apprenant a déjà une inscription CONFIRMEE")
    void inscrire_duplicateConfirmed_shouldThrow() {
        Inscription existing = new Inscription();
        existing.setStatut(Statut.CONFIRMEE);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(apprenantRepository.findById(1L)).thenReturn(Optional.of(apprenant1));
        when(inscriptionRepository.findByApprenantIdAndSessionId(1L, 1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> inscriptionService.inscrire(1L, dto1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà une inscription confirmée");
    }

    // ── 3. Full session sends learner to waiting list ────────────────────────

    @Test
    @DisplayName("3. Session complète : l'apprenant rejoint la liste d'attente (EN_ATTENTE, position #1)")
    void inscrire_sessionFull_joinsWaitingList() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(apprenantRepository.findById(1L)).thenReturn(Optional.of(apprenant1));
        when(inscriptionRepository.findByApprenantIdAndSessionId(1L, 1L)).thenReturn(Optional.empty());
        // 2 confirmed seats for capacity 2 -> full!
        when(inscriptionRepository.countBySessionIdAndStatut(1L, Statut.CONFIRMEE)).thenReturn(2L);
        when(inscriptionRepository.countBySessionIdAndStatut(1L, Statut.EN_ATTENTE)).thenReturn(0L);
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscription result = inscriptionService.inscrire(1L, dto1);

        assertThat(result.getStatut()).isEqualTo(Statut.EN_ATTENTE);
        assertThat(result.getPositionFile()).isEqualTo(1);
    }

    // ── 4. Duplicate waiting-list registration rejected ───────────────────────

    @Test
    @DisplayName("4. Rejet doublon : l'apprenant est déjà sur la liste d'attente")
    void inscrire_duplicateWaiting_shouldThrow() {
        Inscription existing = new Inscription();
        existing.setStatut(Statut.EN_ATTENTE);
        existing.setPositionFile(1);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(apprenantRepository.findById(1L)).thenReturn(Optional.of(apprenant1));
        when(inscriptionRepository.findByApprenantIdAndSessionId(1L, 1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> inscriptionService.inscrire(1L, dto1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà sur la liste d'attente");
    }

    // ── 5. Waiting-list FIFO ordering ─────────────────────────────────────────

    @Test
    @DisplayName("5. Ordonnancement FIFO : le second apprenant en attente obtient la position #2")
    void inscrire_waitingList_fifoOrdering() {
        InscriptionDto dto2 = new InscriptionDto(2L);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(apprenantRepository.findById(2L)).thenReturn(Optional.of(apprenant2));
        when(inscriptionRepository.findByApprenantIdAndSessionId(2L, 1L)).thenReturn(Optional.empty());
        when(inscriptionRepository.countBySessionIdAndStatut(1L, Statut.CONFIRMEE)).thenReturn(2L);
        // Already 1 learner in waiting queue
        when(inscriptionRepository.countBySessionIdAndStatut(1L, Statut.EN_ATTENTE)).thenReturn(1L);
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscription result = inscriptionService.inscrire(1L, dto2);

        assertThat(result.getStatut()).isEqualTo(Statut.EN_ATTENTE);
        assertThat(result.getPositionFile()).isEqualTo(2);
    }

    // ── 6. Cancellation releases a seat when no one is waiting ────────────────

    @Test
    @DisplayName("6. Annulation sans liste d'attente : statut ANNULEE, libère la place")
    void annuler_noWaiting_releasesSeat() {
        Inscription confirmed = new Inscription();
        confirmed.setId(10L);
        confirmed.setStatut(Statut.CONFIRMEE);
        confirmed.setSession(session);

        when(inscriptionRepository.findById(10L)).thenReturn(Optional.of(confirmed));
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscriptionRepository.findFirstBySessionIdAndStatutOrderByPositionFileAsc(1L, Statut.EN_ATTENTE))
                .thenReturn(Optional.empty());
        when(inscriptionRepository.findFirstBySessionIdAndStatutOrderByIdAsc(1L, Statut.EN_ATTENTE))
                .thenReturn(Optional.empty());

        Inscription result = inscriptionService.annuler(10L);

        assertThat(result.getStatut()).isEqualTo(Statut.ANNULEE);
        assertThat(result.getMessagePromotion()).contains("La place est désormais disponible");
    }

    // ── 7. First waiting learner is automatically promoted ────────────────────

    @Test
    @DisplayName("7. Annulation d'un inscrit actif : promotion automatique du premier en liste d'attente")
    void annuler_withWaiting_promotesFirst() {
        Inscription confirmed = new Inscription();
        confirmed.setId(10L);
        confirmed.setStatut(Statut.CONFIRMEE);
        confirmed.setSession(session);

        Inscription waiting = new Inscription();
        waiting.setId(20L);
        waiting.setStatut(Statut.EN_ATTENTE);
        waiting.setPositionFile(1);
        waiting.setApprenant(apprenant2);
        waiting.setSession(session);

        when(inscriptionRepository.findById(10L)).thenReturn(Optional.of(confirmed));
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscriptionRepository.findFirstBySessionIdAndStatutOrderByPositionFileAsc(1L, Statut.EN_ATTENTE))
                .thenReturn(Optional.of(waiting));
        when(inscriptionRepository.findBySessionIdAndStatutOrderByDateHeureInscriptionAsc(1L, Statut.EN_ATTENTE))
                .thenReturn(new ArrayList<>());

        Inscription result = inscriptionService.annuler(10L);

        assertThat(result.getStatut()).isEqualTo(Statut.ANNULEE);
        assertThat(waiting.getStatut()).isEqualTo(Statut.CONFIRMEE);
        assertThat(waiting.getPositionFile()).isNull();
        assertThat(result.getMessagePromotion()).contains("Bob Martin (position #1) a été automatiquement promu");
    }

    // ── 8. Waiting learner cancellation ───────────────────────────────────────

    @Test
    @DisplayName("8. Annulation d'une inscription en liste d'attente : retiré de la file sans promotion")
    void annuler_waitingLearner_removedFromQueue() {
        Inscription waiting = new Inscription();
        waiting.setId(30L);
        waiting.setStatut(Statut.EN_ATTENTE);
        waiting.setPositionFile(2);
        waiting.setSession(session);

        when(inscriptionRepository.findById(30L)).thenReturn(Optional.of(waiting));
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscriptionRepository.findBySessionIdAndStatutOrderByDateHeureInscriptionAsc(1L, Statut.EN_ATTENTE))
                .thenReturn(new ArrayList<>());

        Inscription result = inscriptionService.annuler(30L);

        assertThat(result.getStatut()).isEqualTo(Statut.ANNULEE);
        assertThat(result.getPositionFile()).isNull();
        assertThat(result.getMessagePromotion()).contains("liste d'attente annulée");
    }

    // ── 9. Started session rejects registration ───────────────────────────────

    @Test
    @DisplayName("9. Rejet : session déjà commencée")
    void inscrire_sessionStarted_shouldThrow() {
        session.setDateDebut(LocalDate.now().minusDays(1));

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(apprenantRepository.findById(1L)).thenReturn(Optional.of(apprenant1));

        assertThatThrownBy(() -> inscriptionService.inscrire(1L, dto1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà commencé");
    }

    // ── 10. Completed session rejects registration ────────────────────────────

    @Test
    @DisplayName("10. Rejet : session déjà terminée")
    void inscrire_sessionCompleted_shouldThrow() {
        session.setDateDebut(LocalDate.now().minusDays(10));
        session.setDateFin(LocalDate.now().minusDays(2));

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(apprenantRepository.findById(1L)).thenReturn(Optional.of(apprenant1));

        assertThatThrownBy(() -> inscriptionService.inscrire(1L, dto1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà commencé");
    }

    // ── 11. Cancelled learner can register again ──────────────────────────────

    @Test
    @DisplayName("11. Ré-inscription autorisée après annulation : réactive l'enregistrement existant")
    void inscrire_afterCancellation_reactivates() {
        Inscription cancelled = new Inscription();
        cancelled.setId(1L);
        cancelled.setStatut(Statut.ANNULEE);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(apprenantRepository.findById(1L)).thenReturn(Optional.of(apprenant1));
        when(inscriptionRepository.findByApprenantIdAndSessionId(1L, 1L)).thenReturn(Optional.of(cancelled));
        when(inscriptionRepository.countBySessionIdAndStatut(1L, Statut.CONFIRMEE)).thenReturn(0L);
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscription result = inscriptionService.inscrire(1L, dto1);

        assertThat(result.getStatut()).isEqualTo(Statut.CONFIRMEE);
        verify(inscriptionRepository, times(1)).save(cancelled);
    }

    // ── 12. Cannot cancel an already cancelled registration ───────────────────

    @Test
    @DisplayName("12. Rejet : tentative d'annuler une inscription déjà annulée")
    void annuler_alreadyCancelled_shouldThrow() {
        Inscription inscription = new Inscription();
        inscription.setId(1L);
        inscription.setStatut(Statut.ANNULEE);
        inscription.setSession(session);

        when(inscriptionRepository.findById(1L)).thenReturn(Optional.of(inscription));

        assertThatThrownBy(() -> inscriptionService.annuler(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà annulée");
    }
}
