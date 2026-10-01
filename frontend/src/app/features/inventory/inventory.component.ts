import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
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

  readonly activeTab = signal<'stock' | 'kardex'>('stock');
  readonly searchQuery = signal<string>('');
  readonly showOnlyLowStock = signal<boolean>(false);
  readonly selectedIngredientFilter = signal<string>('');

  // Modal de Ajuste
  readonly showAdjustModal = signal<boolean>(false);
  readonly adjustingItem = signal<StockBalanceDto | null>(null);
  readonly newQuantity = signal<number>(0);
  readonly newUnitCost = signal<number>(0);
  readonly adjustReason = signal<string>('Conteo físico de auditoría');
  readonly isSubmitting = signal<boolean>(false);

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
    const user = this.authService.currentUser();
    const branchId = user?.branchId;
    this.inventoryService.loadStock(branchId).subscribe();
    this.inventoryService.loadKardex(branchId).subscribe();
  }

  setTab(tab: 'stock' | 'kardex'): void {
    this.activeTab.set(tab);
    this.inventoryService.clearAlerts();
  }

  openAdjustModal(item: StockBalanceDto): void {
    this.adjustingItem.set(item);
    this.newQuantity.set(item.currentQuantity);
    this.newUnitCost.set(item.averageUnitCost);
    this.adjustReason.set('Conteo físico de inventario');
    this.showAdjustModal.set(true);
  }

  closeAdjustModal(): void {
    this.showAdjustModal.set(false);
    this.adjustingItem.set(null);
  }

  confirmAdjustment(): void {
    const item = this.adjustingItem();
    if (!item) return;

    this.isSubmitting.set(true);
    const user = this.authService.currentUser();

    this.inventoryService
      .adjustStock(
        {
          ingredientId: item.ingredientId,
          newQuantity: this.newQuantity(),
          unitCost: this.newUnitCost(),
          reason: this.adjustReason(),
        },
        user?.branchId
      )
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.closeAdjustModal();
          // Recargar movimientos de kardex
          this.inventoryService.loadKardex(user?.branchId).subscribe();
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
        return 'Inventario Inicial';
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
