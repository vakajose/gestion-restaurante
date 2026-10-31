package com.restaurant.app.core.common;

import com.restaurant.app.core.common.exception.DomainException;
import com.restaurant.app.core.security.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex, HttpServletRequest request) {
        log.warn("Regla de negocio/Validación en [{}] ({}): {}",
            request.getRequestURI(), ex.getErrorCode(), ex.getMessage());

        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());

        String title = ex.getTitle() != null ? ex.getTitle() : switch (status) {
            case NOT_FOUND -> "Recurso no encontrado";
            case CONFLICT -> "Conflicto de recurso";
            case UNPROCESSABLE_ENTITY -> "Regla de negocio no procesable";
            case BAD_REQUEST -> "Solicitud inválida";
            case FORBIDDEN -> "Operación no autorizada";
            default -> "Error de dominio";
        };

        problem.setTitle(title);
        problem.setType(ex.getTypeUri() != null ? ex.getTypeUri() : URI.create("https://restaurant.com/errors/domain"));
        problem.setInstance(URI.create(request.getRequestURI()));

        attachMetadata(problem, ex.getErrorCode());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Solicitud inválida (IllegalArgument) en [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Solicitud inválida");
        problem.setType(URI.create("https://restaurant.com/errors/bad-request"));
        problem.setInstance(URI.create(request.getRequestURI()));

        attachMetadata(problem, "BAD_REQUEST");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Conflicto de estado (IllegalState) en [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflicto de estado");
        problem.setType(URI.create("https://restaurant.com/errors/conflict"));
        problem.setInstance(URI.create(request.getRequestURI()));

        attachMetadata(problem, "CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Autenticación fallida en [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Autenticación fallida");
        problem.setType(URI.create("https://restaurant.com/errors/unauthorized"));
        problem.setInstance(URI.create(request.getRequestURI()));

        attachMetadata(problem, "AUTHENTICATION_FAILED");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado en [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Acceso denegado");
        problem.setType(URI.create("https://restaurant.com/errors/forbidden"));
        problem.setInstance(URI.create(request.getRequestURI()));

        attachMetadata(problem, "ACCESS_DENIED");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> invalidParams = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            invalidParams.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String detail = invalidParams.isEmpty()
            ? "Error de validación en la solicitud"
            : "Uno o más campos presentan inconsistencias: " + invalidParams;

        log.warn("Error de validación en [{}] ({} campos inválidos): {}",
            request.getRequestURI(), invalidParams.size(), invalidParams);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Error de validación");
        problem.setType(URI.create("https://restaurant.com/errors/validation"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("invalidParams", invalidParams);

        attachMetadata(problem, "VALIDATION_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnhandled(Exception ex, HttpServletRequest request) {
        Throwable rootCause = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(ex);
        if (rootCause instanceof DomainException domainEx) {
            return handleDomainException(domainEx, request);
        }
        if (rootCause instanceof AccessDeniedException accessEx) {
            return handleAccessDenied(accessEx, request);
        }
        if (rootCause instanceof BadCredentialsException credsEx) {
            return handleBadCredentials(credsEx, request);
        }
        if (rootCause instanceof IllegalArgumentException illegalArg) {
            return handleIllegalArgument(illegalArg, request);
        }
        if (rootCause instanceof IllegalStateException illegalState) {
            return handleIllegalState(illegalState, request);
        }

        log.error("Excepción no controlada en [{}]: ", request.getRequestURI(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ha ocurrido un error interno inesperado en el servidor"
        );
        problem.setTitle("Error interno del servidor");
        problem.setType(URI.create("https://restaurant.com/errors/internal-error"));
        problem.setInstance(URI.create(request.getRequestURI()));

        attachMetadata(problem, "INTERNAL_SERVER_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private void attachMetadata(ProblemDetail problem, String errorCode) {
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now().toString());

        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            problem.setProperty("tenantId", tenantId.toString());
        }
    }
}
