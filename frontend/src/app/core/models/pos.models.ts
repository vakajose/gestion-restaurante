export type PaymentMethod = 'CASH' | 'CARD' | 'QR' | 'TRANSFER';

export type SyncStatus = 'PENDING' | 'SYNCED' | 'FAILED';

export interface CreateOrderItemRequest {
  dishId: string;
  quantity: number;
  unitPrice: number;
  notes?: string;
}

export interface CreateOrderRequest {
  clientTransactionId?: string;
  branchId: string;
  cashShiftId?: string | null;
  paymentMethod: PaymentMethod | string;
  notes?: string;
  items: CreateOrderItemRequest[];
}

export interface OrderItemDto {
  id?: string;
  dishId: string;
  dishName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  notes?: string;
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
