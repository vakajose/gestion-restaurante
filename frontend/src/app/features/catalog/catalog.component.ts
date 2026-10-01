import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogService } from '../../core/services/catalog.service';
import { AuthService } from '../../core/services/auth.service';
import {
  BranchOverrideRequest,
  CategoryDto,
  CloneDishRequest,
  CreateCategoryRequest,
  CreateDishRequest,
  CreateIngredientRequest,
  DishPriceDto,
  DishRecipeDto,
  DishRecipeItemRequest,
  IngredientDto,
  UnitOfMeasure,
} from '../../core/models/catalog.models';

export type CatalogTab = 'dishes' | 'ingredients' | 'categories';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalog.component.html',
})
export class CatalogComponent implements OnInit {
  private readonly catalogService = inject(CatalogService);
  readonly authService = inject(AuthService);

  // Pestaña activa
  readonly activeTab = signal<CatalogTab>('dishes');

  // Estados de datos
  readonly dishes = signal<DishPriceDto[]>([]);
  readonly categories = signal<CategoryDto[]>([]);
  readonly ingredients = signal<IngredientDto[]>([]);

  // Filtros y búsqueda
  readonly searchDishQuery = signal<string>('');
  readonly selectedCategoryFilter = signal<string>('');
  readonly filterByBranch = signal<boolean>(true);
  readonly searchIngredientQuery = signal<string>('');

  // Estados de carga y feedback
  readonly isLoading = signal<boolean>(false);
  readonly isActionLoading = signal<boolean>(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  // Estados para modales
  readonly showCreateDishModal = signal<boolean>(false);
  readonly showRecipeModal = signal<boolean>(false);
  readonly showOverrideModal = signal<boolean>(false);
  readonly showCloneModal = signal<boolean>(false);
  readonly showCreateIngredientModal = signal<boolean>(false);
  readonly showCreateCategoryModal = signal<boolean>(false);

  // Datos contextuales de modales
  readonly selectedDish = signal<DishPriceDto | null>(null);
  readonly currentRecipe = signal<DishRecipeDto | null>(null);
  readonly editingRecipeItems = signal<{ ingredientId: string; quantity: number }[]>([]);

  // Formularios para creación de entidades
  newDishForm: CreateDishRequest = {
    code: '',
    name: '',
    categoryId: '',
    salePrice: 0,
    description: '',
    isActive: true,
  };

  newIngredientForm: CreateIngredientRequest = {
    name: '',
    unitOfMeasure: 'KG',
    minStockAlert: 1,
  };

  newCategoryForm: CreateCategoryRequest = {
    name: '',
    icon: '🍽️',
    sortOrder: 1,
  };

  overrideForm: { isAvailable: boolean; priceOverride: number | null } = {
    isAvailable: true,
    priceOverride: null,
  };

  cloneForm: {
    newCode: string;
    newName: string;
    newSalePrice: number | null;
  } = {
    newCode: '',
    newName: '',
    newSalePrice: null,
  };

  newRecipeIngredientId = '';
  newRecipeQuantity = 1;

  readonly availableUnits: UnitOfMeasure[] = ['KG', 'GRAM', 'LITER', 'ML', 'UNIT'];

  // Platos filtrados computados reactivamente
  readonly filteredDishes = computed(() => {
    const query = this.searchDishQuery().toLowerCase().trim();
    const catFilter = this.selectedCategoryFilter();

    return this.dishes().filter((dish) => {
      const matchesQuery =
        !query ||
        dish.name.toLowerCase().includes(query) ||
        dish.code.toLowerCase().includes(query);
      const matchesCategory = !catFilter || dish.categoryId === catFilter;
      return matchesQuery && matchesCategory;
    });
  });

  // Ingredientes filtrados reactivamente
  readonly filteredIngredients = computed(() => {
    const query = this.searchIngredientQuery().toLowerCase().trim();
    return this.ingredients().filter((ing) => {
      return (
        !query ||
        ing.name.toLowerCase().includes(query) ||
        ing.unitOfMeasure.toLowerCase().includes(query)
      );
    });
  });

  ngOnInit(): void {
    this.loadInitialData();
  }

  setTab(tab: CatalogTab): void {
    this.activeTab.set(tab);
    this.clearAlerts();
  }

  loadInitialData(): void {
    this.isLoading.set(true);
    this.clearAlerts();

    // Cargar categorías e insumos para poblar selectores
    this.catalogService.getCategories().subscribe({
      next: (cats) => {
        this.categories.set(cats);
        if (cats.length > 0 && !this.newDishForm.categoryId) {
          this.newDishForm.categoryId = cats[0].id;
        }
      },
      error: () => this.showError('Error al cargar las categorías'),
    });

    this.catalogService.getIngredients().subscribe({
      next: (ings) => this.ingredients.set(ings),
      error: () => this.showError('Error al cargar los ingredientes'),
    });

    this.refreshDishes();
  }

  refreshDishes(): void {
    this.isLoading.set(true);
    const branchId = this.filterByBranch()
      ? this.authService.currentUser()?.branchId || undefined
      : undefined;

    this.catalogService.getDishes(branchId).subscribe({
      next: (dishes) => {
        this.dishes.set(dishes);
        this.isLoading.set(false);
      },
      error: () => {
        this.showError('Error al consultar el catálogo de platos');
        this.isLoading.set(false);
      },
    });
  }

  refreshIngredients(): void {
    this.catalogService.getIngredients().subscribe({
      next: (ings) => this.ingredients.set(ings),
      error: () => this.showError('Error al actualizar ingredientes'),
    });
  }

  refreshCategories(): void {
    this.catalogService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: () => this.showError('Error al actualizar categorías'),
    });
  }

  // ---------------------------------------------------------------------------
  // Modal Crear Plato
  // ---------------------------------------------------------------------------
  openCreateDishModal(): void {
    this.newDishForm = {
      code: '',
      name: '',
      categoryId: this.categories()[0]?.id || '',
      salePrice: 0,
      description: '',
      isActive: true,
    };
    this.showCreateDishModal.set(true);
  }

  closeCreateDishModal(): void {
    this.showCreateDishModal.set(false);
  }

  saveDish(): void {
    if (!this.newDishForm.code.trim() || !this.newDishForm.name.trim() || !this.newDishForm.categoryId) {
      this.showError('Complete el código, nombre y categoría del plato.');
      return;
    }
    if (this.newDishForm.salePrice < 0) {
      this.showError('El precio de venta no puede ser negativo.');
      return;
    }

    this.isActionLoading.set(true);
    this.catalogService.createDish(this.newDishForm).subscribe({
      next: (created) => {
        this.isActionLoading.set(false);
        this.closeCreateDishModal();
        this.showSuccess(`Plato "${created.name}" registrado exitosamente.`);
        this.refreshDishes();
      },
      error: (err) => {
        this.isActionLoading.set(false);
        const detail = err?.error?.detail || 'No se pudo crear el plato.';
        this.showError(detail);
      },
    });
  }

  // ---------------------------------------------------------------------------
  // Receta BOM (Bill of Materials)
  // ---------------------------------------------------------------------------
  openRecipeModal(dish: DishPriceDto): void {
    this.selectedDish.set(dish);
    this.isActionLoading.set(true);
    this.currentRecipe.set(null);
    this.showRecipeModal.set(true);

    this.catalogService.getDishRecipe(dish.dishId).subscribe({
      next: (recipe) => {
        this.currentRecipe.set(recipe);
        this.editingRecipeItems.set(
          recipe.items.map((it) => ({
            ingredientId: it.ingredientId,
            quantity: it.quantity,
          }))
        );
        this.isActionLoading.set(false);
      },
      error: () => {
        // Puede que no tenga receta aún
        this.currentRecipe.set({
          dishId: dish.dishId,
          dishName: dish.name,
          items: [],
        });
        this.editingRecipeItems.set([]);
        this.isActionLoading.set(false);
      },
    });
  }

  closeRecipeModal(): void {
    this.showRecipeModal.set(false);
    this.selectedDish.set(null);
    this.currentRecipe.set(null);
  }

  addRecipeItem(): void {
    if (!this.newRecipeIngredientId || this.newRecipeQuantity <= 0) {
      this.showError('Seleccione un ingrediente y cantidad válida');
      return;
    }

    const current = [...this.editingRecipeItems()];
    const existingIndex = current.findIndex((i) => i.ingredientId === this.newRecipeIngredientId);

    if (existingIndex >= 0) {
      current[existingIndex].quantity = this.newRecipeQuantity;
    } else {
      current.push({
        ingredientId: this.newRecipeIngredientId,
        quantity: this.newRecipeQuantity,
      });
    }

    this.editingRecipeItems.set(current);
    this.newRecipeQuantity = 1;
  }

  removeRecipeItem(index: number): void {
    const current = [...this.editingRecipeItems()];
    current.splice(index, 1);
    this.editingRecipeItems.set(current);
  }

  saveRecipe(): void {
    const dish = this.selectedDish();
    if (!dish) return;

    this.isActionLoading.set(true);
    const items: DishRecipeItemRequest[] = this.editingRecipeItems().map((it) => ({
      ingredientId: it.ingredientId,
      quantity: Number(it.quantity),
    }));

    this.catalogService.updateDishRecipe(dish.dishId, items).subscribe({
      next: (updated) => {
        this.isActionLoading.set(false);
        this.closeRecipeModal();
        this.showSuccess(`Receta para "${dish.name}" actualizada con ${updated.items.length} insumos.`);
      },
      error: (err) => {
        this.isActionLoading.set(false);
        const detail = err?.error?.detail || 'Error al actualizar la receta técnica.';
        this.showError(detail);
      },
    });
  }

  // ---------------------------------------------------------------------------
  // Sobreescritura por Sucursal (Branch Override)
  // ---------------------------------------------------------------------------
  openOverrideModal(dish: DishPriceDto): void {
    this.selectedDish.set(dish);
    this.overrideForm = {
      isAvailable: dish.isAvailable,
      priceOverride: dish.hasOverride ? dish.effectivePrice : null,
    };
    this.showOverrideModal.set(true);
  }

  closeOverrideModal(): void {
    this.showOverrideModal.set(false);
    this.selectedDish.set(null);
  }

  saveBranchOverride(): void {
    const dish = this.selectedDish();
    const branchId = this.authService.currentUser()?.branchId;

    if (!dish) return;
    if (!branchId) {
      this.showError('No se encontró una sucursal activa en la sesión del usuario.');
      return;
    }

    this.isActionLoading.set(true);
    const req: BranchOverrideRequest = {
      branchId,
      isAvailable: this.overrideForm.isAvailable,
      priceOverride: this.overrideForm.priceOverride !== null ? Number(this.overrideForm.priceOverride) : null,
    };

    this.catalogService.setBranchOverride(dish.dishId, req).subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.closeOverrideModal();
        this.showSuccess(`Sobreescritura de sucursal aplicada para "${dish.name}".`);
        this.refreshDishes();
      },
      error: (err) => {
        this.isActionLoading.set(false);
        const detail = err?.error?.detail || 'Error al aplicar sobreescritura de sucursal.';
        this.showError(detail);
      },
    });
  }

  // ---------------------------------------------------------------------------
  // Clonación de Plato (Branch Clone con Receta Diferenciada)
  // ---------------------------------------------------------------------------
  openCloneModal(dish: DishPriceDto): void {
    this.selectedDish.set(dish);
    this.cloneForm = {
      newCode: `${dish.code}-SUC`,
      newName: `${dish.name} (Especial)`,
      newSalePrice: dish.effectivePrice,
    };
    this.showCloneModal.set(true);
  }

  closeCloneModal(): void {
    this.showCloneModal.set(false);
    this.selectedDish.set(null);
  }

  saveCloneDish(): void {
    const dish = this.selectedDish();
    const branchId = this.authService.currentUser()?.branchId || null;

    if (!dish) return;
    if (!this.cloneForm.newCode.trim() || !this.cloneForm.newName.trim()) {
      this.showError('Código y nombre del clon son obligatorios.');
      return;
    }

    this.isActionLoading.set(true);
    const req: CloneDishRequest = {
      targetBranchId: branchId,
      newCode: this.cloneForm.newCode.trim(),
      newName: this.cloneForm.newName.trim(),
      newSalePrice: this.cloneForm.newSalePrice !== null ? Number(this.cloneForm.newSalePrice) : null,
    };

    this.catalogService.cloneDish(dish.dishId, req).subscribe({
      next: (cloned) => {
        this.isActionLoading.set(false);
        this.closeCloneModal();
        this.showSuccess(`Plato clonado como "${cloned.name}" con trazabilidad a "${dish.name}".`);
        this.refreshDishes();
      },
      error: (err) => {
        this.isActionLoading.set(false);
        const detail = err?.error?.detail || 'Error al clonar el plato.';
        this.showError(detail);
      },
    });
  }

  // ---------------------------------------------------------------------------
  // Insumos y Categorías
  // ---------------------------------------------------------------------------
  openCreateIngredientModal(): void {
    this.newIngredientForm = {
      name: '',
      unitOfMeasure: 'KG',
      minStockAlert: 1,
    };
    this.showCreateIngredientModal.set(true);
  }

  closeCreateIngredientModal(): void {
    this.showCreateIngredientModal.set(false);
  }

  saveIngredient(): void {
    if (!this.newIngredientForm.name.trim()) {
      this.showError('El nombre del insumo es obligatorio.');
      return;
    }
    if (this.newIngredientForm.minStockAlert < 0) {
      this.showError('El stock de alerta no puede ser negativo.');
      return;
    }

    this.isActionLoading.set(true);
    this.catalogService.createIngredient(this.newIngredientForm).subscribe({
      next: (created) => {
        this.isActionLoading.set(false);
        this.closeCreateIngredientModal();
        this.showSuccess(`Insumo "${created.name}" registrado correctamente.`);
        this.refreshIngredients();
      },
      error: (err) => {
        this.isActionLoading.set(false);
        const detail = err?.error?.detail || 'Error al registrar insumo.';
        this.showError(detail);
      },
    });
  }

  openCreateCategoryModal(): void {
    this.newCategoryForm = {
      name: '',
      icon: '🍽️',
      sortOrder: this.categories().length + 1,
    };
    this.showCreateCategoryModal.set(true);
  }

  closeCreateCategoryModal(): void {
    this.showCreateCategoryModal.set(false);
  }

  saveCategory(): void {
    if (!this.newCategoryForm.name.trim()) {
      this.showError('El nombre de la categoría es obligatorio.');
      return;
    }

    this.isActionLoading.set(true);
    this.catalogService.createCategory(this.newCategoryForm).subscribe({
      next: (created) => {
        this.isActionLoading.set(false);
        this.closeCreateCategoryModal();
        this.showSuccess(`Categoría "${created.name}" creada exitosamente.`);
        this.refreshCategories();
      },
      error: (err) => {
        this.isActionLoading.set(false);
        const detail = err?.error?.detail || 'Error al crear la categoría.';
        this.showError(detail);
      },
    });
  }

  // Helpers de búsqueda y formato
  getIngredientName(id: string): string {
    const ing = this.ingredients().find((i) => i.id === id);
    return ing ? ing.name : 'Insumo desconocido';
  }

  getIngredientUnit(id: string): string {
    const ing = this.ingredients().find((i) => i.id === id);
    return ing ? ing.unitOfMeasure : '';
  }

  showSuccess(msg: string): void {
    this.errorMessage.set(null);
    this.successMessage.set(msg);
  }

  showError(msg: string): void {
    this.successMessage.set(null);
    this.errorMessage.set(msg);
  }

  clearAlerts(): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
  }
}
