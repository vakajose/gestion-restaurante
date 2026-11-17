import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../core/services/catalog.service';
import { AuthService } from '../../core/services/auth.service';
import { BranchContextService } from '../../core/services/branch-context.service';
import { resolveMediaUrl } from '../../core/config/runtime-env';
import { EmojiPickerComponent, ImageCropperModalComponent } from '../../shared/components/ui';
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
  imports: [CommonModule, FormsModule, RouterLink, ImageCropperModalComponent, EmojiPickerComponent],
  templateUrl: './catalog.component.html',
})
export class CatalogComponent implements OnInit {
  private readonly catalogService = inject(CatalogService);
  readonly authService = inject(AuthService);
  readonly branchContextService = inject(BranchContextService);
  readonly resolveMediaUrl = resolveMediaUrl;

  // Contexto multi-sucursal (TASK-632)
  readonly branches = this.branchContextService.branches;
  readonly activeBranchId = computed(
    () => this.branchContextService.activeBranchId() || this.authService.currentUser()?.branchId || null
  );
  readonly activeBranchName = computed(
    () => this.branchContextService.activeBranch()?.name || 'Sucursal Activa'
  );
  readonly isMultiBranchUser = computed(() => this.branches().length >= 2);
  private lastLoadedBranchId: string | null | undefined = undefined;

  // Pestaña activa
  readonly activeTab = signal<CatalogTab>('dishes');

  // Estados de datos
  readonly dishes = signal<DishPriceDto[]>([]);
  readonly categories = signal<CategoryDto[]>([]);
  readonly ingredients = signal<IngredientDto[]>([]);

  // Filtros y búsqueda
  readonly searchDishQuery = signal<string>('');
  readonly selectedCategoryFilter = signal<string>('');
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

  // Estados de Recorte Nativo WebP (ADR-0021 / PB-036 / TASK-506)
  readonly isCropperOpen = signal<boolean>(false);
  readonly selectedImageFileForCropper = signal<File | null>(null);
  readonly targetDishForCropper = signal<DishPriceDto | null>(null);
  readonly newDishImagePreview = signal<string | null>(null);
  readonly pendingNewDishImageBlob = signal<Blob | null>(null);

  // Estados de Asistente SKU y Prefijos de Categoría (ADR-0022 / PB-048 / TASK-507)
  readonly isCodeManuallyEdited = signal<boolean>(false);
  readonly isPrefixManuallyEdited = signal<boolean>(false);
  readonly isSuggestingCode = signal<boolean>(false);
  readonly prefixConflictError = signal<string | null>(null);

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
    codePrefix: '',
    icon: '🍽️',
    sortOrder: 1,
  };

  overrideForm: { isAvailable: boolean; priceOverride: number | null } = {
    isAvailable: true,
    priceOverride: null,
  };

  cloneForm: {
    targetBranchId: string;
    newCode: string;
    newName: string;
    newSalePrice: number | null;
  } = {
    targetBranchId: '',
    newCode: '',
    newName: '',
    newSalePrice: null,
  };

  constructor() {
    effect(() => {
      const branchId = this.activeBranchId();
      if (this.lastLoadedBranchId !== undefined && branchId !== this.lastLoadedBranchId) {
        this.lastLoadedBranchId = branchId;
        this.refreshDishes();
      }
    });
  }

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
    const branchId = this.activeBranchId() || undefined;
    this.lastLoadedBranchId = branchId ?? null;

    this.catalogService.getDishes(branchId, true).subscribe({
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
  // Modal Crear / Editar Plato & Recorte de Imágenes (ADR-0021 / PB-036)
  // ---------------------------------------------------------------------------
  readonly editingDishId = signal<string | null>(null);
  readonly editingCategoryId = signal<string | null>(null);
  readonly categoryPendingDelete = signal<CategoryDto | null>(null);

  openCreateDishModal(): void {
    this.removeNewDishImage();
    this.editingDishId.set(null);
    const initialCatId = this.categories()[0]?.id || '';
    this.newDishForm = {
      code: '',
      name: '',
      categoryId: initialCatId,
      branchId: this.activeBranchId(),
      salePrice: 0,
      description: '',
      isActive: true,
    };
    this.isCodeManuallyEdited.set(false);
    this.showCreateDishModal.set(true);
    if (initialCatId) {
      this.suggestCodeForCategory(initialCatId);
    }
  }

  openEditDishModal(dish: DishPriceDto): void {
    this.removeNewDishImage();
    this.editingDishId.set(dish.dishId);
    const matchedCategory = this.categories().find((c) => c.name === dish.categoryName);
    this.newDishForm = {
      code: dish.code || '',
      name: dish.name || '',
      categoryId: matchedCategory?.id || this.categories()[0]?.id || '',
      branchId: dish.branchId !== undefined ? dish.branchId : this.activeBranchId(),
      salePrice: dish.basePrice ?? 0,
      description: dish.description ?? '',
      isActive: dish.isAvailable ?? true,
    };
    this.isCodeManuallyEdited.set(true);
    this.showCreateDishModal.set(true);
  }

  onDishCategoryChange(categoryId: string): void {
    if (!this.isCodeManuallyEdited() || !this.newDishForm.code.trim()) {
      this.suggestCodeForCategory(categoryId);
    }
  }

  onDishCodeManualInput(): void {
    this.isCodeManuallyEdited.set(true);
  }

  suggestCodeManually(): void {
    if (this.newDishForm.categoryId) {
      this.suggestCodeForCategory(this.newDishForm.categoryId, true);
    }
  }

  suggestCodeForCategory(categoryId: string, force = false): void {
    if (!categoryId) return;
    this.isSuggestingCode.set(true);
    this.catalogService.suggestNextDishCode(categoryId).subscribe({
      next: (res) => {
        this.isSuggestingCode.set(false);
        if (force || !this.isCodeManuallyEdited() || !this.newDishForm.code.trim()) {
          this.newDishForm.code = res.nextCode;
        }
      },
      error: () => {
        this.isSuggestingCode.set(false);
      },
    });
  }

  closeCreateDishModal(): void {
    this.removeNewDishImage();
    this.editingDishId.set(null);
    this.showCreateDishModal.set(false);
  }

  onImageFileSelected(event: Event, dish?: DishPriceDto): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (dish) {
        this.targetDishForCropper.set(dish);
      } else {
        this.targetDishForCropper.set(null);
      }
      this.selectedImageFileForCropper.set(file);
      this.isCropperOpen.set(true);
      input.value = '';
    }
  }

  onCroppedImage(blob: Blob): void {
    const target = this.targetDishForCropper();
    if (target) {
      // Subida directa al plato existente
      this.isActionLoading.set(true);
      this.catalogService.uploadDishImage(target.dishId, blob).subscribe({
        next: () => {
          this.isActionLoading.set(false);
          this.isCropperOpen.set(false);
          this.selectedImageFileForCropper.set(null);
          this.targetDishForCropper.set(null);
          this.showSuccess(`Fotografía de "${target.name}" actualizada exitosamente.`);
          this.refreshDishes();
        },
        error: (err) => {
          this.isActionLoading.set(false);
          const detail = err?.error?.detail || 'Error al subir la imagen del plato.';
          this.showError(detail);
        },
      });
    } else {
      // Imagen asociada al formulario de nuevo plato
      if (this.newDishImagePreview()) {
        URL.revokeObjectURL(this.newDishImagePreview()!);
      }
      const previewUrl = URL.createObjectURL(blob);
      this.newDishImagePreview.set(previewUrl);
      this.pendingNewDishImageBlob.set(blob);
      this.isCropperOpen.set(false);
      this.selectedImageFileForCropper.set(null);
    }
  }

  onCropperCancel(): void {
    this.isCropperOpen.set(false);
    this.selectedImageFileForCropper.set(null);
    this.targetDishForCropper.set(null);
  }

  removeNewDishImage(): void {
    if (this.newDishImagePreview()) {
      URL.revokeObjectURL(this.newDishImagePreview()!);
    }
    this.newDishImagePreview.set(null);
    this.pendingNewDishImageBlob.set(null);
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
    const editId = this.editingDishId();
    const request$ = editId
      ? this.catalogService.updateDish(editId, this.newDishForm)
      : this.catalogService.createDish(this.newDishForm);
    const verb = editId ? 'actualizado' : 'registrado';

    request$.subscribe({
      next: (saved) => {
        const pendingBlob = this.pendingNewDishImageBlob();
        if (pendingBlob) {
          this.catalogService.uploadDishImage(saved.id, pendingBlob).subscribe({
            next: () => {
              this.isActionLoading.set(false);
              this.closeCreateDishModal();
              this.showSuccess(`Plato "${saved.name}" ${verb} con fotografía exitosamente.`);
              this.refreshDishes();
            },
            error: () => {
              this.isActionLoading.set(false);
              this.closeCreateDishModal();
              this.showSuccess(`Plato "${saved.name}" ${verb}, pero ocurrió un error al subir la foto.`);
              this.refreshDishes();
            },
          });
        } else {
          this.isActionLoading.set(false);
          this.closeCreateDishModal();
          this.showSuccess(`Plato "${saved.name}" ${verb} exitosamente.`);
          this.refreshDishes();
        }
      },
      error: (err) => {
        this.isActionLoading.set(false);
        const detail = err?.error?.detail || (editId ? 'No se pudo actualizar el plato.' : 'No se pudo crear el plato.');
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
    const branchId = this.activeBranchId();

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
  // Clonación de Plato (Hacia Otra Sucursal o Duplicado en la Misma)
  // ---------------------------------------------------------------------------
  openCloneModal(dish: DishPriceDto): void {
    this.selectedDish.set(dish);
    const currentBranchId = this.activeBranchId() || '';
    const otherBranch = this.branches().find((b) => b.id !== currentBranchId);
    const defaultTarget = otherBranch ? otherBranch.id : currentBranchId;
    const isSameBranch = defaultTarget === currentBranchId;

    this.cloneForm = {
      targetBranchId: defaultTarget,
      newCode: isSameBranch ? `${dish.code}-COPIA` : dish.code,
      newName: isSameBranch ? `${dish.name} (Copia)` : dish.name,
      newSalePrice: dish.effectivePrice,
    };
    this.showCloneModal.set(true);
  }

  onCloneTargetBranchChange(targetBranchId: string): void {
    const dish = this.selectedDish();
    if (!dish) return;
    const currentBranchId = this.activeBranchId() || '';
    const isSameBranch = targetBranchId === currentBranchId;
    this.cloneForm.newCode = isSameBranch ? `${dish.code}-COPIA` : dish.code;
    this.cloneForm.newName = isSameBranch ? `${dish.name} (Copia)` : dish.name;
  }

  closeCloneModal(): void {
    this.showCloneModal.set(false);
    this.selectedDish.set(null);
  }

  saveCloneDish(): void {
    const dish = this.selectedDish();
    const currentBranchId = this.activeBranchId() || null;
    const targetBranchId = this.cloneForm.targetBranchId || currentBranchId;

    if (!dish) return;
    if (!this.cloneForm.newCode.trim() || !this.cloneForm.newName.trim()) {
      this.showError('Código y nombre del plato clonado son obligatorios.');
      return;
    }

    this.isActionLoading.set(true);
    const req: CloneDishRequest = {
      targetBranchId,
      newCode: this.cloneForm.newCode.trim(),
      newName: this.cloneForm.newName.trim(),
      newSalePrice: this.cloneForm.newSalePrice !== null ? Number(this.cloneForm.newSalePrice) : null,
    };

    this.catalogService.cloneDish(dish.dishId, req).subscribe({
      next: (cloned) => {
        this.isActionLoading.set(false);
        this.closeCloneModal();
        const targetBranchName = this.branches().find((b) => b.id === targetBranchId)?.name;
        const suffix = targetBranchName && targetBranchId !== currentBranchId
          ? ` hacia "${targetBranchName}"`
          : '';
        this.showSuccess(`Plato "${cloned.name}" clonado${suffix} exitosamente.`);
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
    this.editingCategoryId.set(null);
    const maxOrder = this.categories().length > 0
      ? Math.max(...this.categories().map((c) => c.sortOrder || 0))
      : 0;
    this.newCategoryForm = {
      name: '',
      codePrefix: '',
      icon: '🍽️',
      sortOrder: maxOrder + 1,
    };
    this.isPrefixManuallyEdited.set(false);
    this.prefixConflictError.set(null);
    this.showCreateCategoryModal.set(true);
  }

  openEditCategoryModal(cat: CategoryDto): void {
    this.editingCategoryId.set(cat.id);
    this.newCategoryForm = {
      name: cat.name,
      codePrefix: cat.codePrefix || '',
      icon: cat.icon || '🍽️',
      sortOrder: cat.sortOrder || 1,
    };
    this.isPrefixManuallyEdited.set(true);
    this.prefixConflictError.set(null);
    this.showCreateCategoryModal.set(true);
  }

  closeCreateCategoryModal(): void {
    this.editingCategoryId.set(null);
    this.showCreateCategoryModal.set(false);
    this.prefixConflictError.set(null);
  }

  onCategoryNameChange(name: string): void {
    if (!this.isPrefixManuallyEdited()) {
      const cleaned = (name || '').replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
      const prefix = cleaned.length >= 3 ? cleaned.substring(0, 3) : (cleaned || '');
      this.newCategoryForm.codePrefix = prefix;
      if (prefix) {
        this.validateCategoryPrefix(prefix);
      } else {
        this.prefixConflictError.set(null);
      }
    }
  }

  onCategoryPrefixManualInput(): void {
    this.isPrefixManuallyEdited.set(true);
    if (this.newCategoryForm.codePrefix) {
      this.newCategoryForm.codePrefix = this.newCategoryForm.codePrefix.toUpperCase();
    }
    this.validateCategoryPrefix(this.newCategoryForm.codePrefix);
  }

  validateCategoryPrefix(prefix?: string): boolean {
    if (!prefix || !prefix.trim()) {
      this.prefixConflictError.set('El prefijo de código es obligatorio.');
      return false;
    }
    const trimmed = prefix.trim().toUpperCase();
    const editingId = this.editingCategoryId();
    const exists = this.categories().some(
      (c) => c.id !== editingId && (c.codePrefix || '').toUpperCase() === trimmed
    );
    if (exists) {
      this.prefixConflictError.set(`El prefijo "${trimmed}" ya está en uso por otra categoría.`);
      return false;
    }
    this.prefixConflictError.set(null);
    return true;
  }

  onEmojiSelected(emoji: string): void {
    this.newCategoryForm.icon = emoji;
  }

  saveCategory(): void {
    if (!this.newCategoryForm.name.trim()) {
      this.showError('El nombre de la categoría es obligatorio.');
      return;
    }

    if (!this.newCategoryForm.codePrefix?.trim()) {
      this.onCategoryNameChange(this.newCategoryForm.name);
    }

    if (!this.validateCategoryPrefix(this.newCategoryForm.codePrefix)) {
      this.showError(this.prefixConflictError() || 'El prefijo de código es obligatorio.');
      return;
    }

    this.isActionLoading.set(true);
    const editId = this.editingCategoryId();
    const request$ = editId
      ? this.catalogService.updateCategory(editId, this.newCategoryForm)
      : this.catalogService.createCategory(this.newCategoryForm);
    const verb = editId ? 'actualizada' : 'creada';

    request$.subscribe({
      next: (saved) => {
        this.isActionLoading.set(false);
        this.closeCreateCategoryModal();
        this.showSuccess(`Categoría "${saved.name}" ${verb} exitosamente.`);
        this.refreshCategories();
        if (editId) {
          this.refreshDishes();
        }
      },
      error: (err) => {
        this.isActionLoading.set(false);
        const detail = err?.error?.detail || (editId ? 'Error al actualizar la categoría.' : 'Error al crear la categoría.');
        this.showError(detail);
      },
    });
  }

  requestDeleteCategory(cat: CategoryDto): void {
    this.categoryPendingDelete.set(cat);
  }

  cancelDeleteCategory(): void {
    this.categoryPendingDelete.set(null);
  }

  confirmDeleteCategory(): void {
    const cat = this.categoryPendingDelete();
    if (!cat) return;

    this.isActionLoading.set(true);
    this.catalogService.deleteCategory(cat.id).subscribe({
      next: () => {
        this.isActionLoading.set(false);
        this.categoryPendingDelete.set(null);
        this.showSuccess(`Categoría "${cat.name}" eliminada exitosamente.`);
        this.refreshCategories();
      },
      error: (err) => {
        this.isActionLoading.set(false);
        this.categoryPendingDelete.set(null);
        const detail = err?.error?.detail || 'No se pudo eliminar la categoría.';
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
