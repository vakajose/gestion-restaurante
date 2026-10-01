import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
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

@Injectable({
  providedIn: 'root',
})
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/catalog';

  /**
   * Obtiene la lista completa de categorías del menú
   */
  getCategories(): Observable<CategoryDto[]> {
    return this.http.get<CategoryDto[]>(`${this.baseUrl}/categories`);
  }

  /**
   * Crea una nueva categoría de platos
   */
  createCategory(req: CreateCategoryRequest): Observable<CategoryDto> {
    return this.http.post<CategoryDto>(`${this.baseUrl}/categories`, req);
  }

  /**
   * Obtiene el listado de ingredientes/insumos del maestro
   */
  getIngredients(): Observable<IngredientDto[]> {
    return this.http.get<IngredientDto[]>(`${this.baseUrl}/ingredients`);
  }

  /**
   * Registra un nuevo ingrediente con su unidad de medida y stock de alerta
   */
  createIngredient(req: CreateIngredientRequest): Observable<IngredientDto> {
    return this.http.post<IngredientDto>(`${this.baseUrl}/ingredients`, req);
  }

  /**
   * Obtiene los platos con sus precios base o efectivos para la sucursal indicada
   */
  getDishes(branchId?: string): Observable<DishPriceDto[]> {
    let params = new HttpParams();
    if (branchId) {
      params = params.set('branchId', branchId);
    }
    return this.http.get<DishPriceDto[]>(`${this.baseUrl}/dishes`, { params });
  }

  /**
   * Registra un nuevo plato en el catálogo maestro
   */
  createDish(req: CreateDishRequest): Observable<DishDto> {
    return this.http.post<DishDto>(`${this.baseUrl}/dishes`, req);
  }

  /**
   * Obtiene la receta técnica (BOM - Bill of Materials) de un plato
   */
  getDishRecipe(dishId: string): Observable<DishRecipeDto> {
    return this.http.get<DishRecipeDto>(`${this.baseUrl}/dishes/${dishId}/recipe`);
  }

  /**
   * Sobreescribe o actualiza la receta BOM completa de un plato
   */
  updateDishRecipe(dishId: string, items: DishRecipeItemRequest[]): Observable<DishRecipeDto> {
    return this.http.put<DishRecipeDto>(`${this.baseUrl}/dishes/${dishId}/recipe`, items);
  }

  /**
   * Configura o actualiza la sobreescritura de precio y disponibilidad por sucursal
   */
  setBranchOverride(dishId: string, req: BranchOverrideRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/dishes/${dishId}/branch-override`, req);
  }

  /**
   * Clona un plato para crear una versión diferenciada con receta propia y trazabilidad (cloned_from_id)
   */
  cloneDish(dishId: string, req: CloneDishRequest): Observable<DishDto> {
    return this.http.post<DishDto>(`${this.baseUrl}/dishes/${dishId}/clone`, req);
  }
}
