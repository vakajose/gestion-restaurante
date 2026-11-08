export type UnitOfMeasure = 'KG' | 'GRAM' | 'LITER' | 'ML' | 'UNIT';

export interface CategoryDto {
  id: string;
  tenantId?: string;
  name: string;
  icon?: string | null;
  sortOrder: number;
  codePrefix?: string;
  createdAt?: string;
}

export interface CreateCategoryRequest {
  name: string;
  icon?: string | null;
  sortOrder?: number;
  codePrefix?: string;
}

export interface NextDishCodeResponse {
  nextCode: string;
  prefix: string;
  suggestedSequence: number;
}

export interface IngredientDto {
  id: string;
  tenantId?: string;
  name: string;
  unitOfMeasure: UnitOfMeasure | string;
  minStockAlert: number;
  createdAt?: string;
}

export interface CreateIngredientRequest {
  name: string;
  unitOfMeasure: UnitOfMeasure | string;
  minStockAlert: number;
}

export interface DishDto {
  id: string;
  tenantId?: string;
  branchId?: string | null;
  clonedFromId?: string | null;
  categoryId: string;
  categoryName?: string;
  code: string;
  name: string;
  description?: string | null;
  salePrice: number;
  isActive: boolean;
  createdAt?: string;
  version?: number;
  inventoryStrategy?: InventoryStrategy | string;
  allowNegativeStockOverride?: boolean | null;
  imageUrl?: string | null;
}

export interface CreateDishRequest {
  categoryId: string;
  branchId?: string | null;
  code: string;
  name: string;
  description?: string | null;
  salePrice: number;
  isActive?: boolean;
  recipeItems?: DishRecipeItemRequest[];
  inventoryStrategy?: InventoryStrategy | string;
  allowNegativeStockOverride?: boolean | null;
  imageUrl?: string | null;
}

export interface DishPriceDto {
  dishId: string;
  code: string;
  name: string;
  description?: string | null;
  categoryId: string;
  categoryName?: string;
  basePrice: number;
  effectivePrice: number;
  hasOverride: boolean;
  isAvailable: boolean;
  modifierGroups?: ModifierGroupDto[];
  inventoryStrategy?: InventoryStrategy | string;
  allowNegativeStockOverride?: boolean | null;
  imageUrl?: string | null;
}

export interface ModifierDto {
  id: string;
  groupId?: string;
  name: string;
  additionalPrice: number;
  ingredientId?: string | null;
  quantityConsumed?: number;
  sortOrder: number;
}

export interface ModifierGroupDto {
  id: string;
  name: string;
  minSelectable: number;
  maxSelectable: number;
  isRequired: boolean;
  modifiers: ModifierDto[];
}

export interface CreateModifierRequest {
  name: string;
  additionalPrice: number;
  ingredientId?: string | null;
  quantityConsumed?: number;
  sortOrder?: number;
}

export interface CreateModifierGroupRequest {
  name: string;
  minSelectable?: number;
  maxSelectable?: number;
  isRequired?: boolean;
  modifiers?: CreateModifierRequest[];
}

export interface RecipeIngredientDto {
  ingredientId: string;
  ingredientName: string;
  unitOfMeasure: UnitOfMeasure | string;
  quantity: number;
}

export interface DishRecipeDto {
  dishId: string;
  dishName: string;
  items: RecipeIngredientDto[];
}

export interface DishRecipeItemRequest {
  ingredientId: string;
  quantity: number;
}

export interface BranchOverrideRequest {
  branchId: string;
  isAvailable?: boolean;
  priceOverride?: number | null;
}

export interface CloneDishRequest {
  targetBranchId?: string | null;
  newCode: string;
  newName: string;
  newSalePrice?: number | null;
  customRecipe?: DishRecipeItemRequest[];
}

export interface ComboItemOptionDto {
  id?: string;
  dishId: string;
  dishName: string;
  priceAdjustment: number;
  isDefault: boolean;
}

export interface ComboItemDto {
  id?: string;
  name: string;
  selectionMode: 'FIXED' | 'CHOICE' | string;
  isRequired: boolean;
  defaultDishId?: string | null;
  defaultDishName?: string | null;
  quantity: number;
  sortOrder: number;
  options: ComboItemOptionDto[];
}

export interface ComboDto {
  id: string;
  tenantId?: string;
  branchId?: string | null;
  categoryId: string;
  code: string;
  name: string;
  description?: string | null;
  basePrice: number;
  isActive: boolean;
  items: ComboItemDto[];
}

export interface CreateComboItemOptionRequest {
  dishId: string;
  priceAdjustment?: number;
  isDefault?: boolean;
}

export interface CreateComboItemRequest {
  name: string;
  selectionMode?: 'FIXED' | 'CHOICE' | string;
  isRequired?: boolean;
  defaultDishId?: string | null;
  quantity: number;
  sortOrder?: number;
  options?: CreateComboItemOptionRequest[];
}

export interface CreateComboRequest {
  branchId?: string | null;
  categoryId: string;
  code: string;
  name: string;
  description?: string | null;
  basePrice: number;
  items?: CreateComboItemRequest[];
}

export type CourseType = 'ENTRADA' | 'SEGUNDO' | 'POSTRE' | 'BEBIDA' | 'GENERAL';
export type PriceMode = 'INCLUDED' | 'SURCHARGE' | 'DISCOUNT_FIXED';
export type InventoryStrategy = 'RECIPE_BOM_STRICT' | 'DISH_AS_INVENTORY_UNIT' | 'COMMERCIAL_NO_INVENTORY';

export interface DailyMenuItemDto {
  id: string;
  dishId: string;
  dishName: string;
  dishCode: string;
  courseType: CourseType;
  suggestedQuota: number;
  priceMode: PriceMode;
  surcharge?: number | null;
  sortOrder: number;
}

export interface DailyMenuDto {
  id: string;
  tenantId?: string;
  branchId?: string | null;
  name: string;
  description?: string | null;
  dayOfWeek?: string | null;
  basePrice?: number | null;
  isActive: boolean;
  isTemplate: boolean;
  items: DailyMenuItemDto[];
}

export interface CreateDailyMenuItemRequest {
  dishId: string;
  courseType: CourseType;
  suggestedQuota: number;
  priceMode?: PriceMode;
  surcharge?: number | null;
  sortOrder?: number;
}

export interface CreateDailyMenuRequest {
  branchId?: string | null;
  name: string;
  description?: string | null;
  dayOfWeek?: string | null;
  basePrice?: number | null;
  isTemplate?: boolean;
  items: CreateDailyMenuItemRequest[];
}

export interface ShiftMenuItemDto {
  id: string;
  dishId: string;
  dishName: string;
  dishCode: string;
  courseType: CourseType;
  initialQuota: number;
  currentQuota: number;
  soldQuantity: number;
  isSoldOut: boolean;
  priceMode: PriceMode;
  surcharge?: number | null;
  sortOrder: number;
  version: number;
}

export interface ShiftMenuDto {
  assignmentId: string;
  cashShiftId: string;
  menuTemplateId?: string | null;
  menuTitle: string;
  serviceDate: string;
  items: ShiftMenuItemDto[];
}

export interface AdjustQuotaRequest {
  quantityDelta: number;
  reason: string;
}

export interface InstantiateShiftMenuRequest {
  menuTemplateId: string;
  branchId?: string | null;
  customQuotas?: Record<string, number> | null;
}
