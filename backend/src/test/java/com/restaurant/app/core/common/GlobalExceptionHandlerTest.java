package com.restaurant.app.core.common;

import com.restaurant.app.core.common.exception.BusinessRuleException;
import com.restaurant.app.core.common.exception.EntityNotFoundException;
import com.restaurant.app.core.common.exception.ResourceConflictException;
import com.restaurant.app.core.common.exception.ShiftRequiredException;
import com.restaurant.app.core.common.exception.UnauthorizedOperationException;
import com.restaurant.app.core.common.exception.ValidationException;
import com.restaurant.app.core.security.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("POST", "/api/v1/orders");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldHandleEntityNotFoundExceptionAs404ProblemDetail() {
        UUID id = UUID.randomUUID();
        EntityNotFoundException ex = new EntityNotFoundException("Sucursal", id);

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDomainException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Recurso no encontrado");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getDetail()).contains("Sucursal no encontrado con identificador: " + id);
        assertThat(response.getBody().getProperties()).containsEntry("errorCode", "RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }

    @Test
    void shouldHandleResourceConflictExceptionAs409ProblemDetail() {
        ResourceConflictException ex = new ResourceConflictException("El código ya se encuentra en uso");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDomainException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Conflicto de recurso");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getDetail()).isEqualTo("El código ya se encuentra en uso");
        assertThat(response.getBody().getProperties()).containsEntry("errorCode", "RESOURCE_CONFLICT");
    }

    @Test
    void shouldHandleShiftRequiredExceptionAs409ProblemDetail() {
        ShiftRequiredException ex = new ShiftRequiredException();

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDomainException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Turno de Caja Requerido");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getDetail()).isEqualTo("No es posible registrar ventas: la caja de la sucursal se encuentra cerrada.");
        assertThat(response.getBody().getType().toString()).isEqualTo("https://api.restaurant.com/errors/shift-required");
        assertThat(response.getBody().getProperties()).containsEntry("errorCode", "SHIFT_REQUIRED_ERROR");
    }

    @Test
    void shouldHandleBusinessRuleExceptionAs422ProblemDetail() {
        BusinessRuleException ex = new BusinessRuleException("El turno de caja ya se encuentra cerrado");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDomainException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Regla de negocio no procesable");
        assertThat(response.getBody().getStatus()).isEqualTo(422);
        assertThat(response.getBody().getDetail()).isEqualTo("El turno de caja ya se encuentra cerrado");
        assertThat(response.getBody().getProperties()).containsEntry("errorCode", "BUSINESS_RULE_VIOLATION");
    }

    @Test
    void shouldHandleValidationExceptionAs400ProblemDetail() {
        ValidationException ex = new ValidationException("El branchId es obligatorio");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDomainException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Solicitud inválida");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getDetail()).isEqualTo("El branchId es obligatorio");
        assertThat(response.getBody().getProperties()).containsEntry("errorCode", "VALIDATION_ERROR");
    }

    @Test
    void shouldHandleUnauthorizedOperationExceptionAs403ProblemDetail() {
        UnauthorizedOperationException ex = new UnauthorizedOperationException("No autorizado para acceder a esta sucursal");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDomainException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Operación no autorizada");
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getDetail()).isEqualTo("No autorizado para acceder a esta sucursal");
        assertThat(response.getBody().getProperties()).containsEntry("errorCode", "FORBIDDEN_OPERATION");
    }

    @Test
    void shouldIncludeTenantIdInProblemPropertiesWhenPresent() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.set(new com.restaurant.app.core.security.TenantContext(
            tenantId, UUID.randomUUID(), UUID.randomUUID(), "admin", "ADMIN_TENANT"
        ));

        EntityNotFoundException ex = new EntityNotFoundException("Plato no existe");
        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDomainException(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsEntry("tenantId", tenantId.toString());
    }

    @Test
    void shouldUnwrapDomainExceptionWhenCaughtInHandleUnhandled() {
        BusinessRuleException root = new BusinessRuleException("Stock insuficiente para el insumo");
        RuntimeException wrapped = new RuntimeException("Transaction error", root);

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleUnhandled(wrapped, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Regla de negocio no procesable");
        assertThat(response.getBody().getDetail()).isEqualTo("Stock insuficiente para el insumo");
        assertThat(response.getBody().getProperties()).containsEntry("errorCode", "BUSINESS_RULE_VIOLATION");
    }

    @Test
    void shouldHandleUnexpectedExceptionAs500ProblemDetail() {
        NullPointerException ex = new NullPointerException("Null pointer reference");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleUnhandled(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Error interno del servidor");
        assertThat(response.getBody().getProperties()).containsEntry("errorCode", "INTERNAL_SERVER_ERROR");
    }
}
