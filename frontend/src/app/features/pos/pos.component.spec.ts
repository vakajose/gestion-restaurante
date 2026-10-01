import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PosComponent } from './pos.component';
import { PosService } from '../../core/services/pos.service';
import { AuthService } from '../../core/services/auth.service';
import { signal } from '@angular/core';
import { CachedCategory, CachedProduct, CartItem, OrderDto } from '../../core/models/pos.models';

describe('PosComponent', () => {
  let component: PosComponent;
  let fixture: ComponentFixture<PosComponent>;
  let mockPosService: {
    isOnline: ReturnType<typeof signal<boolean>>;
    pendingCount: ReturnType<typeof signal<number>>;
    cartItems: ReturnType<typeof signal<CartItem[]>>;
    isSyncing: ReturnType<typeof signal<boolean>>;
    isLoading: ReturnType<typeof signal<boolean>>;
    categories: ReturnType<typeof signal<CachedCategory[]>>;
    products: ReturnType<typeof signal<CachedProduct[]>>;
    totalAmount: ReturnType<typeof signal<number>>;
    totalItemsCount: ReturnType<typeof signal<number>>;
    loadCatalog: ReturnType<typeof vi.fn>;
    addToCart: ReturnType<typeof vi.fn>;
    updateQuantity: ReturnType<typeof vi.fn>;
    removeFromCart: ReturnType<typeof vi.fn>;
    clearCart: ReturnType<typeof vi.fn>;
    checkout: ReturnType<typeof vi.fn>;
    syncPendingOrders: ReturnType<typeof vi.fn>;
  };
  let mockAuthService: {
    currentUser: ReturnType<typeof vi.fn>;
  };

  const sampleCategories: CachedCategory[] = [
    { id: 'cat-1', name: 'Hamburguesas', icon: '🍔', sortOrder: 1 },
    { id: 'cat-2', name: 'Bebidas', icon: '🥤', sortOrder: 2 },
  ];

  const sampleProducts: CachedProduct[] = [
    {
      id: 'p-1',
      categoryId: 'cat-1',
      code: 'HAMB-01',
      name: 'Hamburguesa Doble',
      salePrice: 35.0,
      isAvailable: true,
      categoryName: 'Hamburguesas',
    },
    {
      id: 'p-2',
      categoryId: 'cat-1',
      code: 'HAMB-02',
      name: 'Hamburguesa Triple',
      salePrice: 45.0,
      isAvailable: false,
      categoryName: 'Hamburguesas',
    },
    {
      id: 'p-3',
      categoryId: 'cat-2',
      code: 'BEB-01',
      name: 'Gaseosa Cola',
      salePrice: 10.0,
      isAvailable: true,
      categoryName: 'Bebidas',
    },
  ];

  beforeEach(async () => {
    mockPosService = {
      isOnline: signal(true),
      pendingCount: signal(0),
      cartItems: signal<CartItem[]>([]),
      isSyncing: signal(false),
      isLoading: signal(false),
      categories: signal<CachedCategory[]>(sampleCategories),
      products: signal<CachedProduct[]>(sampleProducts),
      totalAmount: signal(0),
      totalItemsCount: signal(0),
      loadCatalog: vi.fn().mockResolvedValue({ categories: sampleCategories, products: sampleProducts }),
      addToCart: vi.fn(),
      updateQuantity: vi.fn(),
      removeFromCart: vi.fn(),
      clearCart: vi.fn(),
      checkout: vi.fn(),
      syncPendingOrders: vi.fn(),
    };

    mockAuthService = {
      currentUser: vi.fn().mockReturnValue({
        id: 'usr-1',
        username: 'cajero1',
        branchId: 'branch-100',
        role: 'CASHIER',
      }),
    };

    await TestBed.configureTestingModule({
      imports: [PosComponent],
      providers: [
        { provide: PosService, useValue: mockPosService },
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente e invocar loadCatalog en ngOnInit', () => {
    expect(component).toBeTruthy();
    expect(mockPosService.loadCatalog).toHaveBeenCalled();
  });

  describe('Filtros y Búsqueda de Productos', () => {
    it('debe listar todas las categorías incluyendo la opción "Todas"', () => {
      const cats = component.categoriesWithCount();
      expect(cats.length).toBe(3);
      expect(cats[0].id).toBe('all');
      expect(cats[0].count).toBe(3);
      expect(cats[1].id).toBe('cat-1');
      expect(cats[1].count).toBe(2);
      expect(cats[2].id).toBe('cat-2');
      expect(cats[2].count).toBe(1);
    });

    it('debe filtrar productos por categoría seleccionada', () => {
      component.selectCategory('cat-2');
      const filtered = component.filteredProducts();
      expect(filtered.length).toBe(1);
      expect(filtered[0].code).toBe('BEB-01');
    });

    it('debe filtrar productos por texto de búsqueda (nombre o código)', () => {
      component.searchQuery.set('triple');
      let filtered = component.filteredProducts();
      expect(filtered.length).toBe(1);
      expect(filtered[0].code).toBe('HAMB-02');

      component.searchQuery.set('BEB');
      filtered = component.filteredProducts();
      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('Gaseosa Cola');
    });
  });

  describe('Interacciones con el Carrito', () => {
    it('debe agregar producto disponible al carrito', () => {
      component.addToCart(sampleProducts[0]);
      expect(mockPosService.addToCart).toHaveBeenCalledWith(sampleProducts[0]);
    });

    it('no debe agregar producto no disponible y mostrar advertencia', () => {
      component.addToCart(sampleProducts[1]);
      expect(mockPosService.addToCart).not.toHaveBeenCalled();
      expect(component.notification()?.type).toBe('warning');
    });

    it('debe delegar updateQuantity, removeFromCart y clearCart al servicio', () => {
      component.updateQuantity('p-1', 1);
      expect(mockPosService.updateQuantity).toHaveBeenCalledWith('p-1', 1);

      component.removeFromCart('p-1');
      expect(mockPosService.removeFromCart).toHaveBeenCalledWith('p-1');

      component.clearCart();
      expect(mockPosService.clearCart).toHaveBeenCalled();
    });
  });

  describe('Modal de Cobro (Checkout)', () => {
    it('no debe abrir el modal de cobro si el carrito está vacío', () => {
      mockPosService.cartItems.set([]);
      component.openCheckoutModal();
      expect(component.showCheckoutModal()).toBe(false);
    });

    it('debe abrir el modal de cobro con montos preestablecidos cuando hay ítems', () => {
      mockPosService.cartItems.set([
        { dishId: 'p-1', code: 'HAMB-01', name: 'Hamburguesa Doble', unitPrice: 35.0, quantity: 2, subtotal: 70.0 },
      ]);
      mockPosService.totalAmount.set(70.0);

      component.openCheckoutModal();
      expect(component.showCheckoutModal()).toBe(true);
      expect(component.selectedPaymentMethod()).toBe('CASH');
      expect(component.receivedAmount()).toBe(70.0);
    });

    it('debe calcular el cambio correctamente', () => {
      mockPosService.totalAmount.set(70.0);
      component.receivedAmount.set(100.0);
      expect(component.changeAmount()).toBe(30.0);
    });

    it('debe validar si el pago es suficiente en efectivo', () => {
      mockPosService.totalAmount.set(70.0);
      component.selectedPaymentMethod.set('CASH');

      component.receivedAmount.set(50.0);
      expect(component.isPaymentValid()).toBe(false);

      component.receivedAmount.set(70.0);
      expect(component.isPaymentValid()).toBe(true);

      component.selectedPaymentMethod.set('CARD');
      expect(component.isPaymentValid()).toBe(true);
    });

    it('debe sumar dinero rápido con addQuickCash y setExactAmount', () => {
      mockPosService.totalAmount.set(50.0);
      component.setExactAmount();
      expect(component.receivedAmount()).toBe(50.0);

      component.addQuickCash(20.0);
      expect(component.receivedAmount()).toBe(70.0);
    });

    it('debe procesar confirmCheckout y mostrar ticket modal', async () => {
      mockPosService.totalAmount.set(35.0);
      component.receivedAmount.set(50.0);
      component.selectedPaymentMethod.set('CASH');
      component.orderNotes.set('Sin cebolla');

      const mockResult: OrderDto = {
        id: 'ord-123',
        branchId: 'branch-100',
        ticketNumber: 'TKT-001',
        orderStatus: 'PAID',
        paymentMethod: 'CASH',
        totalAmount: 35.0,
        createdAt: '2026-10-01T00:00:00Z',
        items: [],
      };
      mockPosService.checkout.mockResolvedValue(mockResult);

      await component.confirmCheckout();

      expect(mockPosService.checkout).toHaveBeenCalledWith('CASH', 'Sin cebolla');
      expect(component.showCheckoutModal()).toBe(false);
      expect(component.showTicketModal()).toBe(true);
      expect(component.lastOrder()).toEqual(mockResult);
    });
  });

  describe('Sincronización Manual', () => {
    it('debe invocar syncPendingOrders y mostrar notificación de éxito', async () => {
      mockPosService.isOnline.set(true);
      mockPosService.syncPendingOrders.mockResolvedValue({ totalProcessed: 3, totalReceived: 3, syncedOrders: [] });

      await component.syncNow();

      expect(mockPosService.syncPendingOrders).toHaveBeenCalled();
      expect(component.notification()?.type).toBe('success');
      expect(component.notification()?.message).toContain('3 órdenes');
    });
  });
});
