import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PosService } from '../../core/services/pos.service';
import { AuthService } from '../../core/services/auth.service';
import { CachedProduct, PaymentMethod, OrderDto } from '../../core/models/pos.models';

@Component({
  selector: 'app-pos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pos.component.html',
})
export class PosComponent implements OnInit {
  readonly posService = inject(PosService);
  readonly authService = inject(AuthService);

  // Filtros y Búsqueda
  readonly searchQuery = signal<string>('');
  readonly selectedCategoryId = signal<string>('all');

  // Estados de Modales
  readonly showCheckoutModal = signal<boolean>(false);
  readonly showTicketModal = signal<boolean>(false);

  // Formulario de Cobro
  readonly selectedPaymentMethod = signal<PaymentMethod>('CASH');
  readonly receivedAmount = signal<number>(0);
  readonly orderNotes = signal<string>('');
  readonly isSubmitting = signal<boolean>(false);

  // Último pedido cobrado para el ticket
  readonly lastOrder = signal<OrderDto | null>(null);

  // Notificaciones de feedback temporal
  readonly notification = signal<{ type: 'success' | 'error' | 'warning' | 'info'; message: string } | null>(null);

  // Productos filtrados reactivamente
  readonly filteredProducts = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const catId = this.selectedCategoryId();
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

  // Lista de categorías con conteo de platos
  readonly categoriesWithCount = computed(() => {
    const cats = this.posService.categories();
    const products = this.posService.products();

    const allCount = products.length;
    const list = [
      { id: 'all', name: 'Todas', icon: '🍽️', count: allCount },
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

  ngOnInit(): void {
    this.loadCatalog();
  }

  async loadCatalog(): Promise<void> {
    try {
      await this.posService.loadCatalog();
    } catch {
      this.showNotification('warning', 'Catálogo cargado en modo sin conexión');
    }
  }

  selectCategory(catId: string): void {
    this.selectedCategoryId.set(catId);
  }

  addToCart(prod: CachedProduct): void {
    if (!prod.isAvailable) {
      this.showNotification('warning', `El plato "${prod.name}" no está disponible`);
      return;
    }
    this.posService.addToCart(prod);
  }

  updateQuantity(dishId: string, delta: number): void {
    this.posService.updateQuantity(dishId, delta);
  }

  removeFromCart(dishId: string): void {
    this.posService.removeFromCart(dishId);
  }

  clearCart(): void {
    this.posService.clearCart();
  }

  openCheckoutModal(): void {
    if (this.posService.cartItems().length === 0) return;
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
    if (!this.isPaymentValid() || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    try {
      const order = await this.posService.checkout(
        this.selectedPaymentMethod(),
        this.orderNotes()
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
