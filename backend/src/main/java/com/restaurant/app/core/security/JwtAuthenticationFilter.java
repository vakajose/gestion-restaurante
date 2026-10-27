package com.restaurant.app.core.security;

import com.restaurant.app.core.common.repository.BranchRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String BRANCH_ID_HEADER = "X-Branch-Id";

    private final JwtService jwtService;
    private final BranchRepository branchRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        BranchRepository branchRepository,
        ObjectMapper objectMapper
    ) {
        this.jwtService = jwtService;
        this.branchRepository = branchRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length()).trim();
                Optional<TenantContext> contextOpt = jwtService.extractTenantContext(token);

                if (contextOpt.isPresent()) {
                    TenantContext tenantContext = contextOpt.get();

                    // Procesamiento y validación contextual de la cabecera X-Branch-Id
                    String branchHeader = request.getHeader(BRANCH_ID_HEADER);
                    if (branchHeader != null && !branchHeader.isBlank()) {
                        UUID requestedBranchId;
                        try {
                            requestedBranchId = UUID.fromString(branchHeader.trim());
                        } catch (IllegalArgumentException e) {
                            sendProblemDetail(response, request, HttpStatus.BAD_REQUEST, "Formato de cabecera X-Branch-Id inválido");
                            return;
                        }

                        // Validar que la sucursal exista y pertenezca al tenant del token
                        boolean branchExistsInTenant = branchRepository
                            .findByTenantIdAndId(tenantContext.tenantId(), requestedBranchId)
                            .isPresent();

                        if (!branchExistsInTenant) {
                            sendProblemDetail(response, request, HttpStatus.FORBIDDEN, "La sucursal no existe o no pertenece a la empresa actual");
                            return;
                        }

                        // Validar que el usuario tenga autorización en dicha sucursal
                        if (!tenantContext.isBranchAuthorized(requestedBranchId)) {
                            sendProblemDetail(response, request, HttpStatus.FORBIDDEN, "Acceso no autorizado a la sucursal solicitada");
                            return;
                        }

                        // Actualizar contexto con la sucursal activa solicitada
                        tenantContext = tenantContext.withBranchId(requestedBranchId);
                    }

                    TenantContextHolder.set(tenantContext);

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + tenantContext.role()));
                    if (tenantContext.permissions() != null) {
                        for (String perm : tenantContext.permissions()) {
                            authorities.add(new SimpleGrantedAuthority(perm));
                        }
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        tenantContext.username(),
                        null,
                        authorities
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void sendProblemDetail(
        HttpServletResponse response,
        HttpServletRequest request,
        HttpStatus status,
        String detail
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
