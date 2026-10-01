export interface StockBalanceDto {
  id?: string;
  ingredientId: string;
  ingredientName: string;
  unitOfMeasure: string;
  currentQuantity: number;
  averageUnitCost: number;
  totalValue: number;
  minStockAlert: number;
  isLowStock: boolean;
  lastUpdated: string;
}

export interface KardexMovementDto {
  id: string;
  ingredientId: string;
  ingredientName: string;
  unitOfMeasure: string;
  movementType:
    | 'SALE_OUT'
    | 'PURCHASE_IN'
    | 'WASTE_ADJUSTMENT'
    | 'TRANSFER_IN'
    | 'TRANSFER_OUT'
    | 'INITIAL_INVENTORY'
    | 'PHYSICAL_COUNT_ADJUSTMENT';
  quantity: number;
  unitCost: number;
  totalCost: number;
  balanceQuantity: number;
  referenceId?: string;
  movementDate: string;
  createdBy?: string;
}

export interface StockAdjustmentRequest {
  ingredientId: string;
  newQuantity: number;
  unitCost?: number;
  reason?: string;
}

export interface InitialStockRequest {
  ingredientId: string;
  quantity: number;
  unitCost: number;
}
