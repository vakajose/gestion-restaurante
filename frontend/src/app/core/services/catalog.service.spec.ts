import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CatalogService } from './catalog.service';
import {
  BranchOverrideRequest,
  CategoryDto,
  CloneDishRequest,
  CreateCategoryRequest,
  CreateDishRequest,
  CreateIngredientRequest,
  DishDto,
  DishPriceDto,
  DishRecipeDto,
  DishRecipeItemRequest,
  IngredientDto,
} from '../models/catalog.models';

describe('CatalogService', () => {
  let service: CatalogService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CatalogService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(CatalogService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
  });

  it('debe obtener las categorías con getCategories()', () => {
    const mockCategories: CategoryDto[] = [
      { id: 'cat-1', name: 'Platos Fuertes', icon: '🍲', sortOrder: 1 },
      { id: 'cat-2', name: 'Bebidas', icon: '🥤', sortOrder: 2 },
    ];

    let result: CategoryDto[] | undefined;
    service.getCategories().subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/v1/catalog/categories');
    expect(req.request.method).toBe('GET');
    req.flush(mockCategories);

    expect(result).toEqual(mockCategories);
  });

  it('debe registrar una categoría con createCategory()', () => {
    const createReq: CreateCategoryRequest = {
      name: 'Postres',
      icon: '🍰',
      sortOrder: 3,
    };
    const mockCategory: CategoryDto = {
      id: 'cat-3',
      name: 'Postres',
      icon: '🍰',
      sortOrder: 3,
    };

    let result: CategoryDto | undefined;
    service.createCategory(createReq).subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/v1/catalog/categories');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(createReq);
    req.flush(mockCategory);

    expect(result).toEqual(mockCategory);
  });

  it('debe obtener los ingredientes con getIngredients()', () => {
    const mockIngredients: IngredientDto[] = [
      { id: 'ing-1', name: 'Carne de Res', unitOfMeasure: 'KG', minStockAlert: 5.0 },
      { id: 'ing-2', name: 'Aceite de Oliva', unitOfMeasure: 'LITER', minStockAlert: 2.0 },
    ];

    let result: IngredientDto[] | undefined;
    service.getIngredients().subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/v1/catalog/ingredients');
    expect(req.request.method).toBe('GET');
    req.flush(mockIngredients);

    expect(result).toEqual(mockIngredients);
  });

  it('debe registrar un ingrediente con createIngredient()', () => {
    const createReq: CreateIngredientRequest = {
      name: 'Papas Criollas',
      unitOfMeasure: 'KG',
      minStockAlert: 10,
    };
    const mockCreated: IngredientDto = {
      id: 'ing-3',
      name: 'Papas Criollas',
      unitOfMeasure: 'KG',
      minStockAlert: 10,
    };

    let result: IngredientDto | undefined;
    service.createIngredient(createReq).subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/v1/catalog/ingredients');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(createReq);
    req.flush(mockCreated);

    expect(result).toEqual(mockCreated);
  });

  it('debe obtener los platos sin branchId', () => {
    const mockDishes: DishPriceDto[] = [
      {
        dishId: 'dish-1',
        code: 'PL01',
        name: 'Hamburguesa Clásica',
        description: 'Carne y queso',
        categoryId: 'cat-1',
        categoryName: 'Platos Fuertes',
        basePrice: 35.0,
        effectivePrice: 35.0,
        hasOverride: false,
        isAvailable: true,
      },
    ];

    let result: DishPriceDto[] | undefined;
    service.getDishes().subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/v1/catalog/dishes');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('branchId')).toBe(false);
    req.flush(mockDishes);

    expect(result).toEqual(mockDishes);
  });

  it('debe obtener los platos con filtro por sucursal branchId', () => {
    const mockDishes: DishPriceDto[] = [
      {
        dishId: 'dish-1',
        code: 'PL01',
        name: 'Hamburguesa Clásica',
        description: 'Carne y queso',
        categoryId: 'cat-1',
        categoryName: 'Platos Fuertes',
        basePrice: 35.0,
        effectivePrice: 40.0,
        hasOverride: true,
        isAvailable: true,
      },
    ];

    let result: DishPriceDto[] | undefined;
    service.getDishes('branch-uuid-123').subscribe((res) => (result = res));

    const req = httpTesting.expectOne((r) => r.url === '/api/v1/catalog/dishes' && r.params.get('branchId') === 'branch-uuid-123');
    expect(req.request.method).toBe('GET');
    req.flush(mockDishes);

    expect(result).toEqual(mockDishes);
  });

  it('debe crear un nuevo plato con createDish()', () => {
    const createReq: CreateDishRequest = {
      categoryId: 'cat-1',
      code: 'PL02',
      name: 'Lomo Saltado',
      salePrice: 45.0,
      isActive: true,
    };
    const mockDish: DishDto = {
      id: 'dish-2',
      categoryId: 'cat-1',
      code: 'PL02',
      name: 'Lomo Saltado',
      salePrice: 45.0,
      isActive: true,
    };

    let result: DishDto | undefined;
    service.createDish(createReq).subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/v1/catalog/dishes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(createReq);
    req.flush(mockDish);

    expect(result).toEqual(mockDish);
  });

  it('debe obtener la receta BOM de un plato con getDishRecipe()', () => {
    const mockRecipe: DishRecipeDto = {
      dishId: 'dish-1',
      dishName: 'Hamburguesa Clásica',
      items: [
        { ingredientId: 'ing-1', ingredientName: 'Carne Molida', unitOfMeasure: 'KG', quantity: 0.2 },
        { ingredientId: 'ing-2', ingredientName: 'Pan de Hamburguesa', unitOfMeasure: 'UNIT', quantity: 1.0 },
      ],
    };

    let result: DishRecipeDto | undefined;
    service.getDishRecipe('dish-1').subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/v1/catalog/dishes/dish-1/recipe');
    expect(req.request.method).toBe('GET');
    req.flush(mockRecipe);

    expect(result).toEqual(mockRecipe);
  });

  it('debe actualizar la receta con updateDishRecipe()', () => {
    const items: DishRecipeItemRequest[] = [
      { ingredientId: 'ing-1', quantity: 0.25 },
      { ingredientId: 'ing-2', quantity: 1.0 },
    ];
    const mockRecipe: DishRecipeDto = {
      dishId: 'dish-1',
      dishName: 'Hamburguesa Clásica',
      items: [
        { ingredientId: 'ing-1', ingredientName: 'Carne Molida', unitOfMeasure: 'KG', quantity: 0.25 },
        { ingredientId: 'ing-2', ingredientName: 'Pan de Hamburguesa', unitOfMeasure: 'UNIT', quantity: 1.0 },
      ],
    };

    let result: DishRecipeDto | undefined;
    service.updateDishRecipe('dish-1', items).subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/v1/catalog/dishes/dish-1/recipe');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(items);
    req.flush(mockRecipe);

    expect(result).toEqual(mockRecipe);
  });

  it('debe guardar la sobreescritura de sucursal con setBranchOverride()', () => {
    const overrideReq: BranchOverrideRequest = {
      branchId: 'branch-1',
      isAvailable: true,
      priceOverride: 38.5,
    };

    let completed = false;
    service.setBranchOverride('dish-1', overrideReq).subscribe(() => (completed = true));

    const req = httpTesting.expectOne('/api/v1/catalog/dishes/dish-1/branch-override');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(overrideReq);
    req.flush(null);

    expect(completed).toBe(true);
  });

  it('debe clonar un plato con cloneDish()', () => {
    const cloneReq: CloneDishRequest = {
      targetBranchId: 'branch-2',
      newCode: 'PL01-CBBA',
      newName: 'Hamburguesa Cochabambina',
      newSalePrice: 38.0,
      customRecipe: [{ ingredientId: 'ing-1', quantity: 0.3 }],
    };
    const mockCloned: DishDto = {
      id: 'dish-cloned-1',
      branchId: 'branch-2',
      clonedFromId: 'dish-1',
      categoryId: 'cat-1',
      code: 'PL01-CBBA',
      name: 'Hamburguesa Cochabambina',
      salePrice: 38.0,
      isActive: true,
    };

    let result: DishDto | undefined;
    service.cloneDish('dish-1', cloneReq).subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/v1/catalog/dishes/dish-1/clone');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cloneReq);
    req.flush(mockCloned);

    expect(result).toEqual(mockCloned);
  });
});
