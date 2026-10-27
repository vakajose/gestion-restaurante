export type PaymentMethod = 'CASH' | 'CARD' | 'QR' | 'TRANSFER';

export type SyncStatus = 'PENDING' | 'SYNCED' | 'FAILED';

export interface CreateOrderItemRequest {
  dishId: string;
  quantity: number;
  unitPrice: number;
  notes?: string;
  comboId?: string | null;
  componentDishIds?: string[];
  discountAmount?: number;
}

export interface CreateOrderRequest {
  clientTransactionId?: string;
  branchId: string;
  cashShiftId?: string | null;
  paymentMethod: PaymentMethod | string;
  notes?: string;
  items: CreateOrderItemRequest[];
  appliedPromotionId?: string | null;
}

export interface OrderItemDto {
  id?: string;
  dishId: string;
  dishName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  notes?: string;
  discountAmount?: number;
  comboId?: string | null;
}

export interface OrderDto {
  id: string;
  tenantId?: string;
  branchId: string;
  cashShiftId?: string | null;
  ticketNumber: string;
  orderStatus: string;
  paymentMethod: PaymentMethod | string;
  totalAmount: number;
  clientTransactionId?: string;
  notes?: string;
  createdAt: string;
  closedAt?: string;
  items: OrderItemDto[];
}

export interface SyncOrdersRequest {
  orders: CreateOrderRequest[];
}

export interface SyncOrdersResponse {
  syncedOrders: OrderDto[];
  totalReceived: number;
  totalProcessed: number;
}

export interface CartItem {
  dishId: string;
  code: string;
  name: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  notes?: string;
  comboId?: string | null;
  componentDishIds?: string[];
  isCombo?: boolean;
  discountAmount?: number;
}

export interface PendingOrderEntity {
  localId?: number;
  clientTransactionId: string;
  syncStatus: SyncStatus;
  createdAt: string;
  orderData: CreateOrderRequest;
  ticketNumber?: string;
  totalAmount?: number;
  errorMessage?: string;
}

export interface CachedCategory {
  id: string;
  name: string;
  icon?: string | null;
  sortOrder: number;
}

export interface CachedProduct {
  id: string;
  categoryId: string;
  code: string;
  name: string;
  description?: string | null;
  salePrice: number;
  isAvailable: boolean;
  categoryName?: string;
}

export interface ActiveShiftEntity {
  id: string;
  branchId: string;
  openedAt: string;
  status: 'OPEN' | 'CLOSED';
  openedBy?: string;
}

export interface PromotionDto {
  id: string;
  tenantId?: string;
  name: string;
  description?: string | null;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  scope: 'GLOBAL' | 'CATEGORY' | 'DISH';
  targetCategoryId?: string | null;
  targetDishId?: string | null;
  applicationMode: 'AUTOMATIC' | 'MANUAL';
  daysOfWeek?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  minOrderAmount?: number;
  isActive: boolean;
  branchIds?: string[];
}

export interface CreatePromotionRequest {
  name: string;
  description?: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  scope?: 'GLOBAL' | 'CATEGORY' | 'DISH';
  targetCategoryId?: string | null;
  targetDishId?: string | null;
  applicationMode?: 'AUTOMATIC' | 'MANUAL';
  daysOfWeek?: string;
  startTime?: string;
  endTime?: string;
  startDate?: string;
  endDate?: string;
  minOrderAmount?: number;
  branchIds?: string[];
}
