import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { BranchContextService } from '../../core/services/branch-context.service';
import { InventoryService } from '../../core/services/inventory.service';
import { StockBalanceDto } from '../../core/models/inventory.models';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './inventory.component.html',
})
export class InventoryComponent implements OnInit {
  readonly inventoryService = inject(InventoryService);
  readonly authService = inject(AuthService);
  readonly branchContextService = inject(BranchContextService);

  readonly activeTab = signal<'stock' | 'kardex'>('stock');
  readonly searchQuery = signal<string>('');
  readonly showOnlyLowStock = signal<boolean>(false);
  readonly selectedIngredientFilter = signal<string>('');

  // Acordeón móvil para tarjetas de stock (TASK-317 / PB-028)
  readonly expandedIngredientId = signal<string | null>(null);

  // Modal de Ajuste
  readonly showAdjustModal = signal<boolean>(false);
  readonly adjustingItem = signal<StockBalanceDto | null>(null);
  readonly newQuantity = signal<number>(0);
  readonly newUnitCost = signal<number>(0);
  readonly adjustTotalCost = signal<number>(0);
  readonly adjustReason = signal<string>('Conteo físico de auditoría');
  readonly isSubmitting = signal<boolean>(false);

  // Modal de Stock Inicial
  readonly showInitialStockModal = signal<boolean>(false);
  readonly initialIngredientId = signal<string>('');
  readonly initialQuantity = signal<number>(10);
  readonly initialUnitCost = signal<number>(0);
  readonly initialTotalCost = signal<number>(0);

  readonly uninitializedStock = computed(() =>
    this.inventoryService.stockList().filter((s) => s.currentQuantity === 0 && s.averageUnitCost === 0)
  );

  readonly branchTimezone = computed<string>(
    () => this.branchContextService.activeBranch?.()?.timezone || 'America/La_Paz'
  );

  readonly filteredStock = computed(() => {
    let list = this.inventoryService.stockList();
    const query = this.searchQuery().trim().toLowerCase();
    const onlyLow = this.showOnlyLowStock();

    if (query) {
      list = list.filter((i) =>
        i.ingredientName.toLowerCase().includes(query)
      );
    }
    if (onlyLow) {
      list = list.filter((i) => i.isLowStock);
    }
    return list;
  });

  readonly filteredKardex = computed(() => {
    let list = this.inventoryService.kardexMovements();
    const query = this.searchQuery().trim().toLowerCase();
    const ingId = this.selectedIngredientFilter();

    if (ingId) {
      list = list.filter((m) => m.ingredientId === ingId);
    }
    if (query) {
      list = list.filter(
        (m) =>
          m.ingredientName.toLowerCase().includes(query) ||
          m.movementType.toLowerCase().includes(query)
      );
    }
    return list;
  });

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    const branchId =
      this.branchContextService.activeBranchId() ||
      this.authService.currentUser()?.branchId;
    this.inventoryService.loadStock(branchId).subscribe();
    this.inventoryService.loadKardex(branchId).subscribe();
  }

  setTab(tab: 'stock' | 'kardex'): void {
    this.activeTab.set(tab);
    this.inventoryService.clearAlerts();
  }

  toggleAccordion(ingredientId: string): void {
    this.expandedIngredientId.update((current) =>
      current === ingredientId ? null : ingredientId
    );
  }

  getStockBorderClass(item: StockBalanceDto): string {
    if (item.currentQuantity <= 0) {
      return 'border-l-error';
    }
    if (item.isLowStock) {
      return 'border-l-warning';
    }
    return 'border-l-success';
  }

  openInitialStockModalForIngredient(item: StockBalanceDto): void {
    this.openInitialStockModal(item);
  }

  openInitialStockModal(item?: StockBalanceDto): void {
    if (item) {
      this.initialIngredientId.set(item.ingredientId);
      const qty = item.currentQuantity > 0 ? item.currentQuantity : 10;
      const cost = item.averageUnitCost > 0 ? item.averageUnitCost : 0;
      this.initialQuantity.set(qty);
      this.initialUnitCost.set(cost);
      this.initialTotalCost.set(Number((qty * cost).toFixed(2)));
    } else {
      const candidates = this.uninitializedStock();
      const first = candidates[0] || this.filteredStock()[0];
      this.initialIngredientId.set(first ? first.ingredientId : '');
      this.initialQuantity.set(10);
      this.initialUnitCost.set(0);
      this.initialTotalCost.set(0);
    }
    this.showInitialStockModal.set(true);
  }

  onInitialTotalCostChange(totalCost: number): void {
    const cleanTotal = Math.max(0, Number(totalCost) || 0);
    this.initialTotalCost.set(cleanTotal);
    const qty = this.initialQuantity();
    if (qty > 0) {
      this.initialUnitCost.set(Number((cleanTotal / qty).toFixed(4)));
    }
  }

  onInitialQuantityOrUnitCostChange(): void {
    const qty = Math.max(0, Number(this.initialQuantity()) || 0);
    const cost = Math.max(0, Number(this.initialUnitCost()) || 0);
    this.initialTotalCost.set(Number((qty * cost).toFixed(2)));
  }

  closeInitialStockModal(): void {
    this.showInitialStockModal.set(false);
  }

  confirmInitialStock(): void {
    const ingId = this.initialIngredientId();
    if (!ingId) return;

    this.isSubmitting.set(true);
    const branchId =
      this.branchContextService.activeBranchId() ||
      this.authService.currentUser()?.branchId;

    this.inventoryService
      .registerInitialStock(
        {
          ingredientId: ingId,
          quantity: this.initialQuantity(),
          unitCost: this.initialUnitCost(),
        },
        branchId
      )
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeInitialStockModal();
          this.refresh();
        },
        error: () => {
          this.isSubmitting.set(false);
        },
      });
  }

  openAdjustModal(item: StockBalanceDto): void {
    this.adjustingItem.set(item);
    this.newQuantity.set(item.currentQuantity);
    this.newUnitCost.set(item.averageUnitCost);
    this.adjustTotalCost.set(Number((item.currentQuantity * item.averageUnitCost).toFixed(2)));
    this.adjustReason.set('Conteo físico de inventario');
    this.showAdjustModal.set(true);
  }

  onAdjustTotalCostChange(totalCost: number): void {
    const cleanTotal = Math.max(0, Number(totalCost) || 0);
    this.adjustTotalCost.set(cleanTotal);
    const qty = this.newQuantity();
    if (qty > 0) {
      this.newUnitCost.set(Number((cleanTotal / qty).toFixed(4)));
    }
  }

  onAdjustQuantityOrUnitCostChange(): void {
    const qty = Math.max(0, Number(this.newQuantity()) || 0);
    const cost = Math.max(0, Number(this.newUnitCost()) || 0);
    this.adjustTotalCost.set(Number((qty * cost).toFixed(2)));
  }

  closeAdjustModal(): void {
    this.showAdjustModal.set(false);
    this.adjustingItem.set(null);
  }

  confirmAdjustment(): void {
    const item = this.adjustingItem();
    if (!item) return;

    this.isSubmitting.set(true);
    const branchId =
      this.branchContextService.activeBranchId() ||
      this.authService.currentUser()?.branchId;

    this.inventoryService
      .adjustStock(
        {
          ingredientId: item.ingredientId,
          newQuantity: this.newQuantity(),
          unitCost: this.newUnitCost(),
          reason: this.adjustReason(),
        },
        branchId
      )
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeAdjustModal();
          this.refresh();
        },
        error: () => {
          this.isSubmitting.set(false);
        },
      });
  }

  getMovementBadgeClass(type: string): string {
    switch (type) {
      case 'SALE_OUT':
        return 'bg-lighterror text-error border-error/20';
      case 'INITIAL_INVENTORY':
      case 'INITIAL_STOCK':
      case 'PURCHASE_IN':
      case 'TRANSFER_IN':
        return 'bg-lightsuccess text-success border-success/20';
      case 'PHYSICAL_COUNT_ADJUSTMENT':
        return 'bg-lightprimary text-primary border-primary/20';
      case 'WASTE_ADJUSTMENT':
      case 'TRANSFER_OUT':
        return 'bg-lightwarning text-warning border-warning/20';
      default:
        return 'bg-lightgray text-bodytext border-border';
    }
  }

  getMovementLabel(type: string): string {
    switch (type) {
      case 'SALE_OUT':
        return 'Venta / Receta';
      case 'INITIAL_INVENTORY':
      case 'INITIAL_STOCK':
        return 'Stock Inicial / Apertura';
      case 'PURCHASE_IN':
        return 'Compra Ingreso';
      case 'PHYSICAL_COUNT_ADJUSTMENT':
        return 'Ajuste Físico';
      case 'WASTE_ADJUSTMENT':
        return 'Merma / Pérdida';
      case 'TRANSFER_IN':
        return 'Traspaso Entrada';
      case 'TRANSFER_OUT':
        return 'Traspaso Salida';
      default:
        return type;
    }
  }
}
