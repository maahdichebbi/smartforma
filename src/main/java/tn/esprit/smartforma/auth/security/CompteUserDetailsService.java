package tn.esprit.smartforma.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tn.esprit.smartforma.auth.repository.CompteRepository;

@Service
@RequiredArgsConstructor
public class CompteUserDetailsService implements UserDetailsService {

    private final CompteRepository compteRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return compteRepository.findByEmailIgnoreCaseWithApprenant(email)
                .map(ComptePrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Compte introuvable : " + email));
    }
}
