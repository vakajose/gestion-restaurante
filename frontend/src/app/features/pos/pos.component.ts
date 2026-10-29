import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PosService } from '../../core/services/pos.service';
import { AuthService } from '../../core/services/auth.service';
import { CatalogService } from '../../core/services/catalog.service';
import { BranchContextService } from '../../core/services/branch-context.service';
import { FinanceService } from '../../core/services/finance.service';
import { CachedProduct, PaymentMethod, OrderDto, SelectedModifierRequest } from '../../core/models/pos.models';
import { ComboDto, ModifierDto } from '../../core/models/catalog.models';
import { BottomSheetComponent } from '../../shared/components/bottom-sheet/bottom-sheet.component';

@Component({
  selector: 'app-pos',
  standalone: true,
  imports: [CommonModule, FormsModule, BottomSheetComponent],
  templateUrl: './pos.component.html',
})
export class PosComponent implements OnInit {
  readonly posService = inject(PosService);
  readonly authService = inject(AuthService);
  readonly catalogService = inject(CatalogService);
  readonly branchContextService = inject(BranchContextService);
  readonly financeService = inject(FinanceService);

  // Filtros y Búsqueda
  readonly searchQuery = signal<string>('');
  readonly selectedCategoryId = signal<string>('all');
  readonly combos = signal<ComboDto[]>([]);

  // Adaptabilidad Móvil y Modo de Vista (TASK-317 / PB-028)
  readonly catalogViewMode = signal<'grid' | 'list'>('grid');
  readonly isMobileCartOpen = signal<boolean>(false);

  // Estados de Modales
  readonly showCheckoutModal = signal<boolean>(false);
  readonly showTicketModal = signal<boolean>(false);
  readonly showComboModal = signal<boolean>(false);
  readonly selectedCombo = signal<ComboDto | null>(null);
  readonly comboSelections = signal<Record<string, string>>({});
  readonly showModifierModal = signal<boolean>(false);
  readonly selectedDishForModifiers = signal<CachedProduct | null>(null);
  readonly selectedModifiers = signal<SelectedModifierRequest[]>([]);

  // Formulario de Cobro
  readonly selectedPaymentMethod = signal<PaymentMethod>('CASH');
  readonly receivedAmount = signal<number>(0);
  readonly orderNotes = signal<string>('');
  readonly isSubmitting = signal<boolean>(false);

  // Control de Turno de Caja (TASK-403 / PB-041 / ADR-0013)
  readonly showOpenShiftModal = signal<boolean>(false);
  readonly initialCashInput = signal<number>(200);
  readonly isOpeningShift = signal<boolean>(false);

  // Permiso para abrir turno de caja (ADMIN, SUPERADMIN o con permisos explícitos)
  readonly canOpenShift = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return false;
    if (user.role === 'ADMIN_TENANT' || user.role === 'SUPERADMIN') return true;
    return this.authService.hasPrivilege(['finance:shifts', 'finance:shifts_supervisor']);
  });

  // Nombre del cajero en sesión activa para visualización en ticket
  readonly cashierName = computed(() => {
    const user = this.authService.currentUser();
    return user?.fullName || user?.username || 'Cajero';
  });

  // Último pedido cobrado para el ticket
  readonly lastOrder = signal<OrderDto | null>(null);

  // Notificaciones de feedback temporal
  readonly notification = signal<{ type: 'success' | 'error' | 'warning' | 'info'; message: string } | null>(null);

  // Productos filtrados reactivamente
  readonly filteredProducts = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const catId = this.selectedCategoryId();
    if (catId === 'combos') {
      return [];
    }
    const products = this.posService.products();

    return products.filter((prod) => {
      const matchesCategory = catId === 'all' || prod.categoryId === catId;
      const matchesQuery =
        !query ||
        prod.name.toLowerCase().includes(query) ||
        prod.code.toLowerCase().includes(query);
      return matchesCategory && matchesQuery;
    });
  });

  // Combos filtrados reactivamente
  readonly filteredCombos = computed(() => {
    const catId = this.selectedCategoryId();
    if (catId !== 'all' && catId !== 'combos') {
      return [];
    }
    const query = this.searchQuery().toLowerCase().trim();
    return this.combos().filter((c) =>
      !query || c.name.toLowerCase().includes(query) || c.code.toLowerCase().includes(query)
    );
  });

  // Lista de categorías con conteo de platos y combos
  readonly categoriesWithCount = computed(() => {
    const cats = this.posService.categories();
    const products = this.posService.products();
    const combosCount = this.combos().length;

    const allCount = products.length + combosCount;
    const list = [
      { id: 'all', name: 'Todas', icon: '🍽️', count: allCount },
      { id: 'combos', name: 'Combos / Packs', icon: '🎁', count: combosCount },
      ...cats.map((c) => ({
        id: c.id,
        name: c.name,
        icon: c.icon || '🍴',
        count: products.filter((p) => p.categoryId === c.id).length,
      })),
    ];
    return list;
  });

  // Cálculo de cambio / vuelto para pago en efectivo
  readonly changeAmount = computed(() => {
    const total = this.posService.totalAmount();
    const received = this.receivedAmount();
    return Number((received - total).toFixed(2));
  });

  // Validador de pago
  readonly isPaymentValid = computed(() => {
    const total = this.posService.totalAmount();
    if (total <= 0) return false;
    if (this.selectedPaymentMethod() === 'CASH') {
      return this.receivedAmount() >= total;
    }
    return true;
  });

  // Total de recargo por extras/modificadores seleccionados
  readonly modifierExtraTotal = computed(() => {
    return Number(
      this.selectedModifiers().reduce((acc, m) => acc + (m.additionalPrice || 0), 0).toFixed(2)
    );
  });

  constructor() {
    effect(() => {
      const branchId = this.branchContextService.activeBranchId() || this.authService.currentUser()?.branchId;
      if (branchId) {
        this.financeService.loadCurrentShift(branchId).subscribe();
      }
    });
  }

  ngOnInit(): void {
    const branchId = this.branchContextService.activeBranchId() || this.authService.currentUser()?.branchId;
    this.financeService.loadCurrentShift(branchId || undefined).subscribe();
    this.loadCatalog();
  }

  openOpenShiftModal(): void {
    if (!this.canOpenShift()) {
      this.showNotification('warning', 'No tienes permisos para abrir turnos de caja');
      return;
    }
    this.initialCashInput.set(200);
    this.showOpenShiftModal.set(true);
  }

  closeOpenShiftModal(): void {
    this.showOpenShiftModal.set(false);
  }

  confirmOpenShift(): void {
    const branchId = this.branchContextService.activeBranchId() || this.authService.currentUser()?.branchId;
    if (!branchId) {
      this.showNotification('error', 'No hay una sucursal activa seleccionada');
      return;
    }
    const amount = this.initialCashInput();
    if (amount < 0) {
      this.showNotification('error', 'El fondo inicial no puede ser negativo');
      return;
    }

    this.isOpeningShift.set(true);
    this.financeService.openShift({ branchId, initialCash: amount }).subscribe({
      next: (shift) => {
        this.isOpeningShift.set(false);
        this.showOpenShiftModal.set(false);
        this.showNotification('success', `Turno de caja abierto exitosamente (Turno #${shift.id.substring(0, 8)})`);
      },
      error: (err) => {
        this.isOpeningShift.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al abrir el turno de caja';
        this.showNotification('error', msg);
      },
    });
  }

  async loadCatalog(): Promise<void> {
    const branchId = this.branchContextService.activeBranchId() || this.authService.currentUser()?.branchId;
    try {
      await this.posService.loadCatalog(branchId || undefined);
    } catch {
      this.showNotification('warning', 'Catálogo cargado en modo sin conexión');
    }

    this.catalogService.getCombos(branchId).subscribe({
      next: (c) => this.combos.set(c || []),
      error: () => {
        this.showNotification('info', 'No se pudieron sincronizar combos remotos');
      },
    });

    this.posService.loadActivePromotions(branchId || undefined).catch(() => {
      // Ignorar si falla la carga offline de promociones
    });
  }

  onComboClicked(combo: ComboDto): void {
    if (!this.financeService.hasOpenShift()) {
      this.showNotification('warning', '🔴 Caja Cerrada — Debes abrir un turno para comenzar a vender');
      return;
    }

    const hasChoices = combo.items.some((i) => i.selectionMode === 'CHOICE' && i.options.length > 1);
    if (!hasChoices) {
      this.posService.addComboToCart(combo);
      this.showNotification('success', `Combo "${combo.name}" agregado`);
      return;
    }

    const initialSelections: Record<string, string> = {};
    for (const item of combo.items) {
      if (item.options.length > 0) {
        const defaultOpt = item.options.find((o) => o.isDefault) || item.options[0];
        initialSelections[item.id || item.name] = defaultOpt.dishId;
      }
    }
    this.comboSelections.set(initialSelections);
    this.selectedCombo.set(combo);
    this.showComboModal.set(true);
  }

  setComboOption(itemId: string, dishId: string): void {
    this.comboSelections.update((prev) => ({
      ...prev,
      [itemId]: dishId,
    }));
  }

  confirmComboSelection(): void {
    const combo = this.selectedCombo();
    if (!combo) return;

    const selections = this.comboSelections();
    const componentIds = Object.values(selections);
    this.posService.addComboToCart(combo, 1, componentIds);
    this.showComboModal.set(false);
    this.selectedCombo.set(null);
    this.showNotification('success', `Combo "${combo.name}" personalizado agregado`);
  }

  closeComboModal(): void {
    this.showComboModal.set(false);
    this.selectedCombo.set(null);
  }

  selectCategory(catId: string): void {
    this.selectedCategoryId.set(catId);
  }

  setCatalogViewMode(mode: 'grid' | 'list'): void {
    this.catalogViewMode.set(mode);
  }

  openMobileCart(): void {
    this.isMobileCartOpen.set(true);
  }

  closeMobileCart(): void {
    this.isMobileCartOpen.set(false);
  }

  openCheckout(): void {
    this.isMobileCartOpen.set(false);
    this.openCheckoutModal();
  }

  addToCart(prod: CachedProduct): void {
    if (!this.financeService.hasOpenShift()) {
      this.showNotification('warning', '🔴 Caja Cerrada — Debes abrir un turno para comenzar a vender');
      return;
    }
    if (!prod.isAvailable) {
      this.showNotification('warning', `El plato "${prod.name}" no está disponible`);
      return;
    }
    if (prod.modifierGroups && prod.modifierGroups.length > 0) {
      this.selectedDishForModifiers.set(prod);
      this.selectedModifiers.set([]);
      this.showModifierModal.set(true);
      return;
    }
    this.posService.addToCart(prod);
  }

  toggleModifier(mod: ModifierDto): void {
    this.selectedModifiers.update((current) => {
      const exists = current.some((m) => m.modifierId === mod.id);
      if (exists) {
        return current.filter((m) => m.modifierId !== mod.id);
      } else {
        return [
          ...current,
          {
            modifierId: mod.id,
            modifierName: mod.name,
            additionalPrice: mod.additionalPrice,
            ingredientId: mod.ingredientId,
            quantityConsumed: mod.quantityConsumed,
          },
        ];
      }
    });
  }

  isModifierSelected(modId: string): boolean {
    return this.selectedModifiers().some((m) => m.modifierId === modId);
  }

  confirmModifierSelection(): void {
    const dish = this.selectedDishForModifiers();
    if (!dish) return;

    if (dish.modifierGroups) {
      for (const group of dish.modifierGroups) {
        const countInGroup = this.selectedModifiers().filter((m) =>
          group.modifiers.some((gm) => gm.id === m.modifierId)
        ).length;
        if (group.isRequired && countInGroup < (group.minSelectable || 1)) {
          this.showNotification(
            'error',
            `Debes seleccionar al menos ${group.minSelectable || 1} opción(es) en "${group.name}"`
          );
          return;
        }
        if (group.maxSelectable && countInGroup > group.maxSelectable) {
          this.showNotification(
            'error',
            `Puedes seleccionar como máximo ${group.maxSelectable} opción(es) en "${group.name}"`
          );
          return;
        }
      }
    }

    this.posService.addToCart(dish, 1, undefined, this.selectedModifiers());
    this.closeModifierModal();
    this.showNotification('success', `"${dish.name}" con extras agregado al pedido`);
  }

  closeModifierModal(): void {
    this.showModifierModal.set(false);
    this.selectedDishForModifiers.set(null);
    this.selectedModifiers.set([]);
  }

  updateQuantity(dishId: string, delta: number): void {
    if (!this.financeService.hasOpenShift()) return;
    this.posService.updateQuantity(dishId, delta);
  }

  removeFromCart(dishId: string): void {
    if (!this.financeService.hasOpenShift()) return;
    this.posService.removeFromCart(dishId);
  }

  clearCart(): void {
    if (!this.financeService.hasOpenShift()) return;
    this.posService.clearCart();
  }

  openCheckoutModal(): void {
    if (!this.financeService.hasOpenShift()) {
      this.showNotification('warning', '🔴 Caja Cerrada — Debes abrir un turno para comenzar a vender');
      return;
    }
    if (this.posService.cartItems().length === 0) return;
    this.isMobileCartOpen.set(false);
    const total = this.posService.totalAmount();
    this.selectedPaymentMethod.set('CASH');
    this.receivedAmount.set(total);
    this.orderNotes.set('');
    this.showCheckoutModal.set(true);
  }

  closeCheckoutModal(): void {
    this.showCheckoutModal.set(false);
  }

  setExactAmount(): void {
    this.receivedAmount.set(this.posService.totalAmount());
  }

  addQuickCash(amount: number): void {
    const current = this.receivedAmount() || 0;
    this.receivedAmount.set(Number((current + amount).toFixed(2)));
  }

  async confirmCheckout(): Promise<void> {
    if (!this.financeService.hasOpenShift()) {
      this.showNotification('error', 'Caja cerrada: No se pueden registrar ventas sin un turno activo');
      return;
    }
    if (!this.isPaymentValid() || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    try {
      const branchId = this.branchContextService.activeBranchId() || this.authService.currentUser()?.branchId || undefined;
      const cashShiftId = this.financeService.currentShift()?.id || null;

      const order = await this.posService.checkout(
        this.selectedPaymentMethod(),
        this.orderNotes(),
        branchId,
        cashShiftId
      );

      this.lastOrder.set(order);
      this.showCheckoutModal.set(false);
      this.showTicketModal.set(true);

      if (order.ticketNumber.startsWith('LOCAL-')) {
        this.showNotification('warning', `Pedido guardado localmente (${order.ticketNumber}). Se sincronizará al volver la conexión.`);
      } else {
        this.showNotification('success', `Pedido ${order.ticketNumber} registrado y pagado con éxito.`);
      }
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : 'Error al procesar el cobro';
      this.showNotification('error', errorMsg);
    } finally {
      this.isSubmitting.set(false);
    }
  }

  closeTicketModal(): void {
    this.showTicketModal.set(false);
    this.lastOrder.set(null);
  }

  printTicket(): void {
    if (typeof window !== 'undefined') {
      window.print();
    }
  }

  async syncNow(): Promise<void> {
    if (this.posService.isSyncing() || !this.posService.isOnline()) {
      return;
    }

    try {
      const res = await this.posService.syncPendingOrders();
      if (res && res.totalProcessed > 0) {
        this.showNotification('success', `Se sincronizaron ${res.totalProcessed} órdenes pendientes con el servidor.`);
      } else {
        this.showNotification('info', 'No hay pedidos pendientes de sincronización.');
      }
    } catch {
      this.showNotification('error', 'Fallo al sincronizar con el servidor central.');
    }
  }

  private showNotification(type: 'success' | 'error' | 'warning' | 'info', message: string): void {
    this.notification.set({ type, message });
    setTimeout(() => {
      this.notification.set(null);
    }, 4500);
  }
}
