package com.restaurant.app.modules.pos.internal;

import com.restaurant.app.modules.pos.api.CreateOrderRequest;
import com.restaurant.app.modules.pos.api.OrderDto;
import com.restaurant.app.modules.pos.api.SyncOrdersRequest;
import com.restaurant.app.modules.pos.api.SyncOrdersResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pos/orders")
class OrderController {

    private final OrderServiceImpl orderService;

    OrderController(OrderServiceImpl orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
        @RequestHeader(value = "X-Client-Transaction-Id", required = false) UUID headerClientTxId,
        @Valid @RequestBody CreateOrderRequest request
    ) {
        UUID effectiveTxId = headerClientTxId != null ? headerClientTxId : request.clientTransactionId();
        CreateOrderRequest effectiveRequest = (effectiveTxId != null && !effectiveTxId.equals(request.clientTransactionId()))
            || (request.clientTransactionId() == null && headerClientTxId != null)
            ? new CreateOrderRequest(
                effectiveTxId,
                request.branchId(),
                request.cashShiftId(),
                request.paymentMethod(),
                request.notes(),
                request.items()
            )
            : request;

        OrderServiceImpl.CreateOrderResult result = orderService.createOrder(effectiveRequest);
        HttpStatus status = result.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.order());
    }

    @PostMapping("/sync")
    public ResponseEntity<SyncOrdersResponse> syncOrders(@Valid @RequestBody SyncOrdersRequest request) {
        SyncOrdersResponse response = orderService.syncOrders(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getOrders(
        @RequestParam(value = "branchId", required = false) UUID branchId,
        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<OrderDto> orders = orderService.getOrders(branchId, date);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable("orderId") UUID orderId) {
        OrderDto order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }

    // -------------------------------------------------------------------------
    // Exception Handlers (RFC 7807 ProblemDetail)
    // -------------------------------------------------------------------------

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoSuchElementException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Error de validación en la solicitud");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation Failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
}
