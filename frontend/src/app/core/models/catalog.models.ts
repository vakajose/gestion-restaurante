export type UnitOfMeasure = 'KG' | 'GRAM' | 'LITER' | 'ML' | 'UNIT';

export interface CategoryDto {
  id: string;
  tenantId?: string;
  name: string;
  icon?: string | null;
  sortOrder: number;
  createdAt?: string;
}

export interface CreateCategoryRequest {
  name: string;
  icon?: string | null;
  sortOrder?: number;
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
