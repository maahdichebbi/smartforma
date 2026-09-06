package tn.esprit.smartforma.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartforma.auth.entity.Compte;

import java.util.Optional;

public interface CompteRepository extends JpaRepository<Compte, Long> {

    Optional<Compte> findByEmailIgnoreCase(String email);

    @Query("SELECT c FROM Compte c LEFT JOIN FETCH c.apprenant WHERE LOWER(c.email) = LOWER(:email)")
    Optional<Compte> findByEmailIgnoreCaseWithApprenant(@Param("email") String email);

    @Query("SELECT c FROM Compte c LEFT JOIN FETCH c.apprenant WHERE c.id = :id")
    Optional<Compte> findByIdWithApprenant(@Param("id") Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByApprenantId(Long apprenantId);
}
