import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { CatalogComponent } from './catalog.component';
import { CatalogService } from '../../core/services/catalog.service';
import { AuthService } from '../../core/services/auth.service';
import {
  CategoryDto,
  DishPriceDto,
  DishRecipeDto,
  IngredientDto,
  DishDto,
} from '../../core/models/catalog.models';
import { UserDto } from '../../core/models/auth.models';

describe('CatalogComponent', () => {
  let component: CatalogComponent;
  let fixture: ComponentFixture<CatalogComponent>;
  let catalogServiceSpy: {
    getCategories: ReturnType<typeof vi.fn>;
    getIngredients: ReturnType<typeof vi.fn>;
    getDishes: ReturnType<typeof vi.fn>;
    createDish: ReturnType<typeof vi.fn>;
    getDishRecipe: ReturnType<typeof vi.fn>;
    updateDishRecipe: ReturnType<typeof vi.fn>;
    setBranchOverride: ReturnType<typeof vi.fn>;
    cloneDish: ReturnType<typeof vi.fn>;
    createIngredient: ReturnType<typeof vi.fn>;
    createCategory: ReturnType<typeof vi.fn>;
  };
  let authServiceSpy: {
    currentUser: ReturnType<typeof vi.fn>;
  };

  const mockUser: UserDto = {
    id: 'user-1',
    username: 'gerente',
    email: 'gerente@restaurant.com',
    role: 'ADMIN_TENANT',
    tenantId: 'tenant-1',
    branchId: 'branch-1',
  };

  const mockCategories: CategoryDto[] = [
    { id: 'cat-1', name: 'Platos Fuertes', icon: '🍲', sortOrder: 1 },
    { id: 'cat-2', name: 'Bebidas', icon: '🥤', sortOrder: 2 },
  ];

  const mockIngredients: IngredientDto[] = [
    { id: 'ing-1', name: 'Carne Molida', unitOfMeasure: 'KG', minStockAlert: 5 },
    { id: 'ing-2', name: 'Pan Brioche', unitOfMeasure: 'UNIT', minStockAlert: 10 },
  ];

  const mockDishes: DishPriceDto[] = [
    {
      dishId: 'dish-1',
      code: 'PL01',
      name: 'Hamburguesa Doble',
      description: 'Doble carne y queso cheddar',
      categoryId: 'cat-1',
      categoryName: 'Platos Fuertes',
      basePrice: 40.0,
      effectivePrice: 45.0,
      hasOverride: true,
      isAvailable: true,
    },
    {
      dishId: 'dish-2',
      code: 'BEB01',
      name: 'Limonada Frozen',
      description: 'Limonada fresca con menta',
      categoryId: 'cat-2',
      categoryName: 'Bebidas',
      basePrice: 15.0,
      effectivePrice: 15.0,
      hasOverride: false,
      isAvailable: true,
    },
  ];

  const mockRecipe: DishRecipeDto = {
    dishId: 'dish-1',
    dishName: 'Hamburguesa Doble',
    items: [
      { ingredientId: 'ing-1', ingredientName: 'Carne Molida', unitOfMeasure: 'KG', quantity: 0.3 },
      { ingredientId: 'ing-2', ingredientName: 'Pan Brioche', unitOfMeasure: 'UNIT', quantity: 1 },
    ],
  };

  beforeEach(async () => {
    catalogServiceSpy = {
      getCategories: vi.fn().mockReturnValue(of(mockCategories)),
      getIngredients: vi.fn().mockReturnValue(of(mockIngredients)),
      getDishes: vi.fn().mockReturnValue(of(mockDishes)),
      createDish: vi.fn().mockReturnValue(of({ id: 'dish-3', name: 'Nuevo' } as DishDto)),
      getDishRecipe: vi.fn().mockReturnValue(of(mockRecipe)),
      updateDishRecipe: vi.fn().mockReturnValue(of(mockRecipe)),
      setBranchOverride: vi.fn().mockReturnValue(of(void 0)),
      cloneDish: vi.fn().mockReturnValue(of({ id: 'dish-clone', name: 'Clonado' } as DishDto)),
      createIngredient: vi.fn().mockReturnValue(of(mockIngredients[0])),
      createCategory: vi.fn().mockReturnValue(of(mockCategories[0])),
    };

    authServiceSpy = {
      currentUser: vi.fn().mockReturnValue(mockUser),
    };

    await TestBed.configureTestingModule({
      imports: [CatalogComponent],
      providers: [
        { provide: CatalogService, useValue: catalogServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente e inicializar datos de categorías, insumos y platos', () => {
    expect(component).toBeTruthy();
    expect(catalogServiceSpy.getCategories).toHaveBeenCalled();
    expect(catalogServiceSpy.getIngredients).toHaveBeenCalled();
    expect(catalogServiceSpy.getDishes).toHaveBeenCalledWith('branch-1');
    expect(component.dishes().length).toBe(2);
    expect(component.categories().length).toBe(2);
    expect(component.ingredients().length).toBe(2);
  });

  it('debe cambiar de pestañas correctamente', () => {
    expect(component.activeTab()).toBe('dishes');

    component.setTab('ingredients');
    fixture.detectChanges();
    expect(component.activeTab()).toBe('ingredients');

    component.setTab('categories');
    fixture.detectChanges();
    expect(component.activeTab()).toBe('categories');

    component.setTab('dishes');
    fixture.detectChanges();
    expect(component.activeTab()).toBe('dishes');
  });

  it('debe filtrar platos por texto de búsqueda en nombre o código', () => {
    component.searchDishQuery.set('PL01');
    fixture.detectChanges();
    expect(component.filteredDishes().length).toBe(1);
    expect(component.filteredDishes()[0].code).toBe('PL01');

    component.searchDishQuery.set('Limonada');
    fixture.detectChanges();
    expect(component.filteredDishes().length).toBe(1);
    expect(component.filteredDishes()[0].name).toBe('Limonada Frozen');

    component.searchDishQuery.set('Inexistente');
    fixture.detectChanges();
    expect(component.filteredDishes().length).toBe(0);
  });

  it('debe filtrar platos por categoría seleccionada', () => {
    component.selectedCategoryFilter.set('cat-2');
    fixture.detectChanges();
    expect(component.filteredDishes().length).toBe(1);
    expect(component.filteredDishes()[0].name).toBe('Limonada Frozen');
  });

  it('debe abrir y cargar el modal de receta BOM para un plato', () => {
    const dish = mockDishes[0];
    component.openRecipeModal(dish);
    fixture.detectChanges();

    expect(component.showRecipeModal()).toBe(true);
    expect(component.selectedDish()).toEqual(dish);
    expect(catalogServiceSpy.getDishRecipe).toHaveBeenCalledWith('dish-1');
    expect(component.currentRecipe()).toEqual(mockRecipe);
    expect(component.editingRecipeItems().length).toBe(2);
  });

  it('debe permitir agregar y quitar insumos en la receta y guardarla', () => {
    component.openRecipeModal(mockDishes[0]);
    component.newRecipeIngredientId = 'ing-2';
    component.newRecipeQuantity = 2;
    component.addRecipeItem();

    // Actualizó la cantidad del ingrediente existente ing-2
    expect(component.editingRecipeItems().find((i) => i.ingredientId === 'ing-2')?.quantity).toBe(2);

    component.saveRecipe();
    expect(catalogServiceSpy.updateDishRecipe).toHaveBeenCalledWith('dish-1', expect.any(Array));
    expect(component.showRecipeModal()).toBe(false);
    expect(component.successMessage()).toContain('Receta para');
  });

  it('debe abrir el modal de sobreescritura de sucursal y guardar los ajustes', () => {
    const dish = mockDishes[0];
    component.openOverrideModal(dish);
    expect(component.showOverrideModal()).toBe(true);
    expect(component.overrideForm.priceOverride).toBe(45);

    component.overrideForm.priceOverride = 48.0;
    component.overrideForm.isAvailable = false;
    component.saveBranchOverride();

    expect(catalogServiceSpy.setBranchOverride).toHaveBeenCalledWith('dish-1', {
      branchId: 'branch-1',
      isAvailable: false,
      priceOverride: 48.0,
    });
    expect(component.showOverrideModal()).toBe(false);
    expect(component.successMessage()).toContain('Sobreescritura de sucursal aplicada');
  });

  it('debe abrir el modal de clonación y clonar un plato', () => {
    const dish = mockDishes[0];
    component.openCloneModal(dish);
    expect(component.showCloneModal()).toBe(true);
    expect(component.cloneForm.newCode).toBe('PL01-SUC');

    component.cloneForm.newName = 'Hamburguesa Clon Especial';
    component.saveCloneDish();

    expect(catalogServiceSpy.cloneDish).toHaveBeenCalledWith('dish-1', {
      targetBranchId: 'branch-1',
      newCode: 'PL01-SUC',
      newName: 'Hamburguesa Clon Especial',
      newSalePrice: 45.0,
    });
    expect(component.showCloneModal()).toBe(false);
    expect(component.successMessage()).toContain('Plato clonado como');
  });

  it('debe registrar un nuevo plato desde el modal', () => {
    component.openCreateDishModal();
    expect(component.showCreateDishModal()).toBe(true);

    component.newDishForm = {
      code: 'PL03',
      name: 'Pique Macho',
      categoryId: 'cat-1',
      salePrice: 60.0,
      description: 'Tradicional cochabambino',
      isActive: true,
    };

    component.saveDish();

    expect(catalogServiceSpy.createDish).toHaveBeenCalledWith(component.newDishForm);
    expect(component.showCreateDishModal()).toBe(false);
    expect(component.successMessage()).toContain('registrado exitosamente');
  });

  it('debe registrar un nuevo insumo y actualizar la lista', () => {
    component.openCreateIngredientModal();
    expect(component.showCreateIngredientModal()).toBe(true);

    component.newIngredientForm = {
      name: 'Papas Fritas Congeladas',
      unitOfMeasure: 'KG',
      minStockAlert: 20,
    };

    component.saveIngredient();

    expect(catalogServiceSpy.createIngredient).toHaveBeenCalledWith(component.newIngredientForm);
    expect(component.showCreateIngredientModal()).toBe(false);
    expect(component.successMessage()).toContain('registrado correctamente');
  });

  it('debe registrar una nueva categoría', () => {
    component.openCreateCategoryModal();
    expect(component.showCreateCategoryModal()).toBe(true);

    component.newCategoryForm = {
      name: 'Postres y Cafés',
      icon: '🍰',
      sortOrder: 3,
    };

    component.saveCategory();

    expect(catalogServiceSpy.createCategory).toHaveBeenCalledWith(component.newCategoryForm);
    expect(component.showCreateCategoryModal()).toBe(false);
    expect(component.successMessage()).toContain('creada exitosamente');
  });
});
