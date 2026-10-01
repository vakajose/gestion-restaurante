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
