export type ShiftStatus = 'OPEN' | 'CLOSED';
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
  openedAt: string;
  closedAt?: string;
  initialCash: number;
  totalCashSales: number;
  totalCardSales: number;
  totalOtherSales: number;
  totalCashExpenses: number;
  expectedCash?: number;
  actualCash?: number;
  difference?: number;
  status: ShiftStatus;
}

export interface OpenShiftRequest {
  branchId: string;
  initialCash: number;
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
