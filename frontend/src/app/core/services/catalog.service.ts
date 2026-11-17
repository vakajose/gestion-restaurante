import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, catchError } from 'rxjs';
import {
  AdjustQuotaRequest,
  BranchOverrideRequest,
  BranchServiceSessionDto,
  CategoryDto,
  CloneDailyMenuRequest,
  CloneDishRequest,
  ComboDto,
  ConfigureServiceSessionRequest,
  CreateCategoryRequest,
  CreateComboRequest,
  CreateDailyMenuRequest,
  CreateDishRequest,
  CreateIngredientRequest,
  CreateModifierGroupRequest,
  DailyMenuDto,
  DishDto,
  DishPriceDto,
  DishRecipeDto,
  DishRecipeItemRequest,
  IngredientDto,
  InstantiateShiftMenuRequest,
  ModifierGroupDto,
  NextDishCodeResponse,
  ShiftMenuDto,
  ShiftMenuItemDto,
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
   * Actualiza una categoría existente
   */
  updateCategory(categoryId: string, req: CreateCategoryRequest): Observable<CategoryDto> {
    return this.http.put<CategoryDto>(`${this.baseUrl}/categories/${categoryId}`, req);
  }

  /**
   * Elimina una categoría sin platos asociados
   */
  deleteCategory(categoryId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/categories/${categoryId}`);
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
  getDishes(branchId?: string, includeInactive = false): Observable<DishPriceDto[]> {
    let params = new HttpParams();
    if (branchId) {
      params = params.set('branchId', branchId);
    }
    if (includeInactive) {
      params = params.set('includeInactive', 'true');
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
   * Obtiene el detalle de un plato maestro por ID
   */
  getDishById(dishId: string): Observable<DishDto> {
    return this.http.get<DishDto>(`${this.baseUrl}/dishes/${dishId}`);
  }

  /**
   * Actualiza un plato existente en el catálogo maestro
   */
  updateDish(dishId: string, req: CreateDishRequest): Observable<DishDto> {
    return this.http.put<DishDto>(`${this.baseUrl}/dishes/${dishId}`, req);
  }

  /**
   * Sugiere el siguiente código/SKU secuencial para una categoría dada
   */
  suggestNextDishCode(categoryId: string): Observable<NextDishCodeResponse> {
    const params = new HttpParams().set('categoryId', categoryId);
    return this.http.get<NextDishCodeResponse>(`${this.baseUrl}/dishes/next-code`, { params });
  }

  /**
   * Sube una imagen optimizada (Blob WebP) para un plato específico
   */
  uploadDishImage(dishId: string, imageBlob: Blob): Observable<DishDto> {
    const formData = new FormData();
    formData.append('file', imageBlob, `dish_${dishId}.webp`);
    return this.http.post<DishDto>(`${this.baseUrl}/dishes/${dishId}/image`, formData);
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

  /**
   * Obtiene los combos disponibles para la sucursal indicada
   */
  getCombos(branchId?: string | null): Observable<ComboDto[]> {
    let params = new HttpParams();
    if (branchId) {
      params = params.set('branchId', branchId);
    }
    return this.http.get<ComboDto[]>(`${this.baseUrl}/combos`, { params });
  }

  /**
   * Registra un nuevo combo gastronómico
   */
  createCombo(req: CreateComboRequest): Observable<ComboDto> {
    return this.http.post<ComboDto>(`${this.baseUrl}/combos`, req);
  }

  /**
   * Actualiza un combo existente
   */
  updateCombo(comboId: string, req: CreateComboRequest): Observable<ComboDto> {
    return this.http.put<ComboDto>(`${this.baseUrl}/combos/${comboId}`, req);
  }

  /**
   * Desactiva o elimina un combo
   */
  deleteCombo(comboId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/combos/${comboId}`);
  }

  /**
   * Obtiene todos los grupos de modificadores
   */
  getModifierGroups(): Observable<ModifierGroupDto[]> {
    return this.http.get<ModifierGroupDto[]>(`${this.baseUrl}/modifier-groups`);
  }

  /**
   * Crea un nuevo grupo de modificadores con sus opciones
   */
  createModifierGroup(req: CreateModifierGroupRequest): Observable<ModifierGroupDto> {
    return this.http.post<ModifierGroupDto>(`${this.baseUrl}/modifier-groups`, req);
  }

  /**
   * Actualiza un grupo de modificadores existente
   */
  updateModifierGroup(groupId: string, req: CreateModifierGroupRequest): Observable<ModifierGroupDto> {
    return this.http.put<ModifierGroupDto>(`${this.baseUrl}/modifier-groups/${groupId}`, req);
  }

  /**
   * Elimina un grupo de modificadores
   */
  deleteModifierGroup(groupId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/modifier-groups/${groupId}`);
  }

  /**
   * Obtiene los grupos de modificadores asignados a un plato
   */
  getModifierGroupsForDish(dishId: string): Observable<ModifierGroupDto[]> {
    return this.http.get<ModifierGroupDto[]>(`${this.baseUrl}/dishes/${dishId}/modifier-groups`);
  }

  /**
   * Asocia grupos de modificadores a un plato (reemplazando la lista)
   */
  assignModifierGroupsToDish(dishId: string, groupIds: string[]): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/dishes/${dishId}/modifier-groups`, groupIds);
  }

  /**
   * Obtiene las plantillas o menús del día registrados
   */
  getDailyMenus(branchId?: string, templateOnly?: boolean): Observable<DailyMenuDto[]> {
    let params = new HttpParams();
    if (branchId) {
      params = params.set('branchId', branchId);
    }
    if (templateOnly !== undefined) {
      params = params.set('templateOnly', templateOnly.toString());
    }
    return this.http.get<DailyMenuDto[]>(`${this.baseUrl}/menus`, { params });
  }

  /**
   * Obtiene una plantilla de menú por ID
   */
  getDailyMenuById(id: string): Observable<DailyMenuDto> {
    return this.http.get<DailyMenuDto>(`${this.baseUrl}/menus/${id}`);
  }

  /**
   * Crea una nueva plantilla de Menú del Día
   */
  createDailyMenu(req: CreateDailyMenuRequest): Observable<DailyMenuDto> {
    return this.http.post<DailyMenuDto>(`${this.baseUrl}/menus`, req);
  }

  /**
   * Actualiza una plantilla de Menú del Día
   */
  updateDailyMenu(id: string, req: CreateDailyMenuRequest): Observable<DailyMenuDto> {
    return this.http.put<DailyMenuDto>(`${this.baseUrl}/menus/${id}`, req);
  }

  /**
   * Desactiva o elimina una plantilla de Menú del Día
   */
  deleteDailyMenu(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/menus/${id}`);
  }

  /**
   * Clona una plantilla de Menú del Día
   */
  cloneDailyMenu(id: string, req?: CloneDailyMenuRequest): Observable<DailyMenuDto> {
    return this.http.post<DailyMenuDto>(`${this.baseUrl}/menus/${id}/clone`, req ?? {});
  }

  /**
   * Obtiene la jornada operativa activa o configurada para la sucursal
   */
  getCurrentServiceSession(branchId?: string | null): Observable<BranchServiceSessionDto | null> {
    let params = new HttpParams();
    if (branchId) {
      params = params.set('branchId', branchId);
    }
    return this.http.get<BranchServiceSessionDto>(`${this.baseUrl}/service-sessions/current`, { params }).pipe(
      catchError(() => of(null))
    );
  }

  /**
   * Configura o actualiza la jornada operativa de servicio (Carta Completa o Menú del Día)
   */
  configureServiceSession(req: ConfigureServiceSessionRequest): Observable<BranchServiceSessionDto> {
    return this.http.post<BranchServiceSessionDto>(`${this.baseUrl}/service-sessions/configure`, req);
  }

  /**
   * Cierra la jornada operativa de servicio de la sucursal
   */
  closeServiceSession(sessionId: string): Observable<BranchServiceSessionDto> {
    return this.http.post<BranchServiceSessionDto>(`${this.baseUrl}/service-sessions/${sessionId}/close`, {});
  }

  /**
   * Instancia una plantilla de menú para un turno de caja específico
   */
  instantiateShiftMenu(shiftId: string, req: InstantiateShiftMenuRequest): Observable<ShiftMenuDto> {
    return this.http.post<ShiftMenuDto>(`${this.baseUrl}/shifts/${shiftId}/menu/instantiate`, req);
  }

  /**
   * Obtiene el menú activo asignado a un turno de caja con sus cupos reactivos
   */
  getShiftMenu(shiftId: string): Observable<ShiftMenuDto | null> {
    return this.http.get<ShiftMenuDto>(`${this.baseUrl}/shifts/${shiftId}/menu`).pipe(
      catchError(() => of(null))
    );
  }

  /**
   * Ajusta manualmente el cupo disponible de un plato del turno (con auditoría)
   */
  adjustShiftQuota(shiftId: string, itemId: string, req: AdjustQuotaRequest): Observable<ShiftMenuItemDto> {
    return this.http.post<ShiftMenuItemDto>(`${this.baseUrl}/shifts/${shiftId}/menu-items/${itemId}/adjust-quota`, req);
  }
}
