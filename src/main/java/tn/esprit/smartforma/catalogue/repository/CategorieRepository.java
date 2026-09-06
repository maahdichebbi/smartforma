package tn.esprit.smartforma.catalogue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.smartforma.catalogue.entity.Categorie;

import java.util.Optional;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {

    /** Used to enforce the uniqueness business rule before saving. */
    boolean existsByNomIgnoreCase(String nom);

    /** Used to check for duplicate name during update (excluding the current record). */
    Optional<Categorie> findByNomIgnoreCase(String nom);
}
