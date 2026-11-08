export type ShiftStatus = 'OPEN' | 'CLOSED' | 'CONSOLIDATED';
export type ExpenseType = 'DAILY_OPERATIONAL' | 'MONTHLY_FIXED_PRORATED';
export type ApprovalStatus = 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';

export interface CashShiftDto {
  id: string;
  tenantId: string;
  branchId: string;
  openedBy: string;
  openedByName?: string;
  closedBy?: string;
  closedByName?: string;
  consolidatedBy?: string;
  consolidatedByName?: string;
  openedAt: string;
  closedAt?: string;
  consolidatedAt?: string;
  consolidationNotes?: string;
  initialCash: number;
  totalCashSales: number;
  totalCardSales: number;
  totalOtherSales: number;
  totalCashExpenses: number;
  totalCashRefunds?: number;
  expectedCash?: number;
  actualCash?: number;
  difference?: number;
  status: ShiftStatus;
}

export interface OpenShiftRequest {
  branchId: string;
  initialCash: number;
  menuTemplateId?: string | null;
  customQuotas?: Record<string, number> | null;
}

export interface CloseShiftRequest {
  actualCash: number;
  notes?: string;
}

export interface ExpenseDto {
  id: string;
  tenantId: string;
  branchId: string;
  cashShiftId?: string;
  expenseType: ExpenseType;
  description: string;
  amount: number;
  paidFromCashDrawer: boolean;
  approvalStatus: ApprovalStatus;
  approvedBy?: string;
  approvedByName?: string;
  evidenceUrl?: string;
  approvalNotes?: string;
  expenseDate: string; // ISO date 'YYYY-MM-DD' per ADR-0006
  createdBy: string;
  createdByName?: string;
  createdAt: string;
}

export interface CreateExpenseRequest {
  branchId: string;
  cashShiftId?: string;
  expenseType: ExpenseType;
  description: string;
  amount: number;
  paidFromCashDrawer: boolean;
  evidenceUrl?: string;
  expenseDate?: string;
}

export interface ExpenseApprovalRequest {
  status: 'APPROVED' | 'REJECTED';
  approvalNotes?: string;
}

export interface CashShiftSummaryDto {
  shift: CashShiftDto;
  totalOrders: number;
  approvedExpensesCount: number;
  pendingExpensesCount: number;
}

export type AuthorizationRequestType = 'EXPENSE' | 'ORDER_CANCELLATION';
export type AuthorizationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type AuthorizationChannel = 'IN_SITU_PIN' | 'REMOTE_SESSION';

export interface ShiftAuthorizationDto {
  id: string;
  tenantId: string;
  branchId: string;
  cashShiftId?: string;
  requestType: AuthorizationRequestType;
  referenceId?: string;
  requestedBy: string;
  requestedByName?: string;
  amount?: number;
  status: AuthorizationStatus;
  justification?: string;
  decidedBy?: string;
  decidedByName?: string;
  decisionNotes?: string;
  decisionChannel?: AuthorizationChannel;
  createdAt: string;
  decidedAt?: string;
}

export interface CreateShiftAuthorizationRequest {
  branchId: string;
  cashShiftId?: string;
  requestType: AuthorizationRequestType;
  referenceId?: string;
  amount?: number;
  justification?: string;
}

export interface ValidatePinRequest {
  branchId?: string;
  pin: string;
}

export interface ValidatePinResponse {
  valid: boolean;
  supervisorId?: string;
  supervisorName?: string;
}

export interface DecideAuthorizationRequest {
  notes?: string;
}

export interface OrderAuditSummaryDto {
  id: string;
  ticketNumber: string;
  createdAt: string;
  customerName?: string;
  paymentMethod: string;
  totalAmount: number;
  orderStatus: string;
  orderType: string;
  cancellationReason?: string;
}

export interface ShiftAuditDetailDto {
  shift: CashShiftDto;
  cashSales: number;
  cardSales: number;
  qrSales: number;
  transferSales: number;
  orders: OrderAuditSummaryDto[];
  expenses: ExpenseDto[];
  refunds: OrderAuditSummaryDto[];
}
