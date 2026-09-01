package com.bank.api.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No/invalid Authorization header on {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Guard against a duplicated "Bearer Bearer <token>" header, e.g. if the
        // raw token was pasted into Swagger's Authorize box WITH the "Bearer " prefix
        // already included (Swagger adds its own "Bearer " automatically).
        String token = authHeader.substring(7).trim();
        if (token.startsWith("Bearer ")) {
            log.warn("Authorization header appears to contain a duplicated 'Bearer ' prefix - " +
                    "check that only the raw token (no 'Bearer ' prefix) was entered in Swagger's Authorize dialog");
            token = token.substring(7).trim();
        }

        try {
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authenticated email={} authorities={} on {} {}",
                            email, userDetails.getAuthorities(), request.getMethod(), request.getRequestURI());
                } else {
                    log.warn("JWT failed validation (expired or email mismatch) for email={} on {} {}",
                            email, request.getMethod(), request.getRequestURI());
                }
            }
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            log.warn("JWT signature invalid (token not signed with this server's secret): {}", ex.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            log.warn("JWT subject does not match any existing user (was the user deleted?): {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT could not be parsed/validated: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}