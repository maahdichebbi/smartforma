package tn.esprit.smartforma.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.esprit.smartforma.auth.entity.Compte;
import tn.esprit.smartforma.auth.repository.CompteRepository;
import tn.esprit.smartforma.auth.service.JwtService;
import tn.esprit.smartforma.exception.UnauthorizedException;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CompteRepository compteRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long compteId = Long.parseLong(jwtService.parseClaims(token).getSubject());
            Compte compte = compteRepository.findByIdWithApprenant(compteId)
                    .orElseThrow(() -> new UnauthorizedException("Jeton d'authentification invalide ou expiré."));

            if (!compte.isEnabled()) {
                throw new UnauthorizedException("Ce compte est désactivé.");
            }

            ComptePrincipal principal = new ComptePrincipal(compte);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UnauthorizedException | NumberFormatException ex) {
            SecurityContextHolder.clearContext();
            String message = ex instanceof UnauthorizedException
                    ? ex.getMessage()
                    : "Jeton d'authentification invalide ou expiré.";
            SecurityErrorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), message);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
