package com.restaurant.app.modules.backoffice.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro exclusivo para rutas de plataforma (/api/v1/backoffice/**, /actuator/**).
 * No interactúa con TenantContextHolder ni permite cabecera X-Branch-Id.
 */
class PlatformJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final PlatformJwtService platformJwtService;

    PlatformJwtAuthenticationFilter(PlatformJwtService platformJwtService) {
        this.platformJwtService = platformJwtService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            Optional<PlatformPrincipal> principalOpt = platformJwtService.extractPrincipal(token);

            if (principalOpt.isPresent()) {
                PlatformPrincipal principal = principalOpt.get();
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
