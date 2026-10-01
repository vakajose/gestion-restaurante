package com.restaurant.app.modules.catalog.internal;

import com.restaurant.app.core.security.TenantContextHolder;
import com.restaurant.app.modules.catalog.api.BranchOverrideRequest;
import com.restaurant.app.modules.catalog.api.CategoryDto;
import com.restaurant.app.modules.catalog.api.CloneDishRequest;
import com.restaurant.app.modules.catalog.api.CreateCategoryRequest;
import com.restaurant.app.modules.catalog.api.CreateDishRequest;
import com.restaurant.app.modules.catalog.api.CreateIngredientRequest;
import com.restaurant.app.modules.catalog.api.DishDto;
import com.restaurant.app.modules.catalog.api.DishRecipeDto;
import com.restaurant.app.modules.catalog.api.DishRecipeItemRequest;
import com.restaurant.app.modules.catalog.api.IngredientDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
class CatalogController {

    private final CatalogServiceImpl catalogService;

    CatalogController(CatalogServiceImpl catalogService) {
        this.catalogService = catalogService;
    }

    // -------------------------------------------------------------------------
    // Categories
    // -------------------------------------------------------------------------

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        return ResponseEntity.ok(catalogService.getCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto created = catalogService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------------------------------------------------------------------------
    // Ingredients
    // -------------------------------------------------------------------------

    @GetMapping("/ingredients")
    public ResponseEntity<List<IngredientDto>> getIngredients() {
        return ResponseEntity.ok(catalogService.getIngredients());
    }

    @PostMapping("/ingredients")
    public ResponseEntity<IngredientDto> createIngredient(@Valid @RequestBody CreateIngredientRequest request) {
        IngredientDto created = catalogService.createIngredient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------------------------------------------------------------------------
    // Dishes
    // -------------------------------------------------------------------------

    @GetMapping("/dishes")
    public ResponseEntity<?> getDishes(@RequestParam(required = false) UUID branchId) {
        if (branchId != null) {
            UUID tenantId = TenantContextHolder.getTenantId();
            return ResponseEntity.ok(catalogService.getActiveMenuForBranch(tenantId, branchId));
        }
        return ResponseEntity.ok(catalogService.getDishes());
    }

    @PostMapping("/dishes")
    public ResponseEntity<DishDto> createDish(@Valid @RequestBody CreateDishRequest request) {
        DishDto created = catalogService.createDish(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/dishes/{dishId}")
    public ResponseEntity<DishDto> getDishById(@PathVariable UUID dishId) {
        return ResponseEntity.ok(catalogService.getDishById(dishId));
    }

    @PutMapping("/dishes/{dishId}")
    public ResponseEntity<DishDto> updateDish(
        @PathVariable UUID dishId,
        @Valid @RequestBody CreateDishRequest request
    ) {
        DishDto updated = catalogService.updateDish(dishId, request);
        return ResponseEntity.ok(updated);
    }

    // -------------------------------------------------------------------------
    // Recipes
    // -------------------------------------------------------------------------

    @GetMapping("/dishes/{dishId}/recipe")
    public ResponseEntity<DishRecipeDto> getDishRecipe(@PathVariable UUID dishId) {
        return ResponseEntity.ok(catalogService.getDishRecipe(dishId));
    }

    @PutMapping("/dishes/{dishId}/recipe")
    public ResponseEntity<DishRecipeDto> updateDishRecipe(
        @PathVariable UUID dishId,
        @RequestBody List<@Valid DishRecipeItemRequest> items
    ) {
        DishRecipeDto updated = catalogService.setDishRecipe(dishId, items);
        return ResponseEntity.ok(updated);
    }

    // -------------------------------------------------------------------------
    // Branch Overrides
    // -------------------------------------------------------------------------

    @PostMapping("/dishes/{dishId}/branch-override")
    public ResponseEntity<?> setBranchOverride(
        @PathVariable UUID dishId,
        @Valid @RequestBody BranchOverrideRequest request
    ) {
        return ResponseEntity.ok(catalogService.setBranchOverride(dishId, request));
    }

    // -------------------------------------------------------------------------
    // Dish Cloning
    // -------------------------------------------------------------------------

    @PostMapping("/dishes/{dishId}/clone")
    public ResponseEntity<DishDto> cloneDish(
        @PathVariable UUID dishId,
        @Valid @RequestBody CloneDishRequest request
    ) {
        DishDto cloned = catalogService.cloneDish(dishId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
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
