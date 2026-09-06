package tn.esprit.smartforma.apprenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.smartforma.apprenant.entity.Apprenant;

import java.util.Optional;

public interface ApprenantRepository extends JpaRepository<Apprenant, Long> {

    /** Used to enforce the unique email business rule before saving. */
    boolean existsByEmailIgnoreCase(String email);

    /** Used to check email uniqueness during update (excluding the current record). */
    Optional<Apprenant> findByEmailIgnoreCase(String email);
}
