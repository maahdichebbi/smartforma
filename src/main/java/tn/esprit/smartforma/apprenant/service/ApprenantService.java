package tn.esprit.smartforma.apprenant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartforma.apprenant.dto.ApprenantDto;
import tn.esprit.smartforma.apprenant.entity.Apprenant;
import tn.esprit.smartforma.apprenant.repository.ApprenantRepository;
import tn.esprit.smartforma.exception.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprenantService {

    private final ApprenantRepository apprenantRepository;

    @Transactional(readOnly = true)
    public List<Apprenant> findAll() {
        return apprenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Apprenant findById(Long id) {
        return apprenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apprenant", id));
    }

    public Apprenant create(ApprenantDto dto) {
        // Business rule: email must be unique
        if (apprenantRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new IllegalArgumentException(
                    "Un apprenant avec l'email '" + dto.email() + "' existe déjà."
            );
        }

        Apprenant apprenant = new Apprenant();
        mapDtoToEntity(dto, apprenant);
        return apprenantRepository.save(apprenant);
    }

    public Apprenant update(Long id, ApprenantDto dto) {
        Apprenant apprenant = findById(id);

        // Business rule: new email must not conflict with another learner
        apprenantRepository.findByEmailIgnoreCase(dto.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(conflict -> {
                    throw new IllegalArgumentException(
                            "Un apprenant avec l'email '" + dto.email() + "' existe déjà."
                    );
                });

        mapDtoToEntity(dto, apprenant);
        return apprenantRepository.save(apprenant);
    }

    public void delete(Long id) {
        findById(id); // ensures it exists
        apprenantRepository.deleteById(id);
    }

    private void mapDtoToEntity(ApprenantDto dto, Apprenant apprenant) {
        apprenant.setNom(dto.nom());
        apprenant.setPrenom(dto.prenom());
        apprenant.setEmail(dto.email().toLowerCase().trim());
        apprenant.setCompetences(dto.competences());
        apprenant.setInterets(dto.interets());
        apprenant.setNiveau(dto.niveau() != null ? dto.niveau() : tn.esprit.smartforma.catalogue.entity.Niveau.DEBUTANT);
    }
}
