package com.restaurant.app.modules.inventory.internal;

import com.restaurant.app.core.common.exception.InsufficientStockException;
import com.restaurant.app.modules.pos.api.OrderItemSummary;
import com.restaurant.app.modules.pos.api.OrderPaidEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryEventListenerTest {

    @Mock
    private KardexService kardexService;

    @InjectMocks
    private InventoryEventListener listener;

    private OrderPaidEvent event;

    @BeforeEach
    void setUp() {
        event = new OrderPaidEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "T-1001",
            List.of(new OrderItemSummary(UUID.randomUUID(), "Coca Cola", 1, BigDecimal.valueOf(10.00), BigDecimal.valueOf(10.00))),
            BigDecimal.valueOf(10.00),
            "CASH",
            Instant.now()
        );
    }

    @Test
    void shouldProcessOrderDeductionSuccessfully() {
        listener.on(event);
        verify(kardexService).processOrderDeduction(event);
    }

    @Test
    void shouldCatchAndHandleDomainValidationExceptionWithoutRethrowing() {
        doThrow(new InsufficientStockException("Stock insuficiente para el insumo 'Coca Cola 500ml'"))
            .when(kardexService).processOrderDeduction(any());

        // Debe capturarse limpiamente sin propagar la excepción hacia el async runner
        assertThatCode(() -> listener.on(event))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldRethrowUnexpectedSystemExceptions() {
        doThrow(new NullPointerException("Base de datos no disponible"))
            .when(kardexService).processOrderDeduction(any());

        assertThatThrownBy(() -> listener.on(event))
            .isInstanceOf(NullPointerException.class);
    }
}
