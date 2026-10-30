import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PosComponent } from './pos.component';
import { PosService } from '../../core/services/pos.service';
import { AuthService } from '../../core/services/auth.service';
import { CatalogService } from '../../core/services/catalog.service';
import { BranchContextService } from '../../core/services/branch-context.service';
import { FinanceService } from '../../core/services/finance.service';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { CachedCategory, CachedProduct, CartItem, OrderDto, PromotionDto } from '../../core/models/pos.models';
import { ComboDto } from '../../core/models/catalog.models';
import { CashShiftDto } from '../../core/models/finance.models';

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
    activePromotions: ReturnType<typeof signal<PromotionDto[]>>;
    selectedPromotion: ReturnType<typeof signal<PromotionDto | null>>;
    effectivePromotion: ReturnType<typeof signal<PromotionDto | null>>;
    rawSubtotal: ReturnType<typeof signal<number>>;
    discountAmount: ReturnType<typeof signal<number>>;
    totalAmount: ReturnType<typeof signal<number>>;
    totalItemsCount: ReturnType<typeof signal<number>>;
    loadCatalog: ReturnType<typeof vi.fn>;
    loadActivePromotions: ReturnType<typeof vi.fn>;
    addToCart: ReturnType<typeof vi.fn>;
    addComboToCart: ReturnType<typeof vi.fn>;
    updateQuantity: ReturnType<typeof vi.fn>;
    removeFromCart: ReturnType<typeof vi.fn>;
    clearCart: ReturnType<typeof vi.fn>;
    checkout: ReturnType<typeof vi.fn>;
    syncPendingOrders: ReturnType<typeof vi.fn>;
  };
  let mockAuthService: {
    currentUser: ReturnType<typeof vi.fn>;
    hasPrivilege: ReturnType<typeof vi.fn>;
  };
  let mockCatalogService: {
    getCombos: ReturnType<typeof vi.fn>;
  };
  let mockBranchContextService: {
    activeBranchId: ReturnType<typeof signal<string | null>>;
  };
  let mockFinanceService: {
    currentShift: ReturnType<typeof signal<CashShiftDto | null>>;
    hasOpenShift: ReturnType<typeof signal<boolean>>;
    isLoading: ReturnType<typeof signal<boolean>>;
    errorMessage: ReturnType<typeof signal<string | null>>;
    loadCurrentShift: ReturnType<typeof vi.fn>;
    openShift: ReturnType<typeof vi.fn>;
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

  const sampleCombos: ComboDto[] = [
    {
      id: 'combo-1',
      categoryId: 'cat-1',
      code: 'PACK-01',
      name: 'Combo Duo Clásico',
      description: 'Hamburguesa y soda a elección',
      basePrice: 40.0,
      isActive: true,
      items: [
        {
          id: 'ci-1',
          name: 'Hamburguesa Doble',
          selectionMode: 'FIXED',
          isRequired: true,
          quantity: 1,
          sortOrder: 1,
          options: [{ id: 'opt-1', dishId: 'p-1', dishName: 'Hamburguesa Doble', priceAdjustment: 0, isDefault: true }],
        },
        {
          id: 'ci-2',
          name: 'Bebida a Elección',
          selectionMode: 'CHOICE',
          isRequired: true,
          quantity: 1,
          sortOrder: 2,
          options: [
            { id: 'opt-2', dishId: 'p-3', dishName: 'Gaseosa Cola', priceAdjustment: 0, isDefault: true },
            { id: 'opt-3', dishId: 'p-4', dishName: 'Jugo Natural', priceAdjustment: 2, isDefault: false },
          ],
        },
      ],
    },
  ];

  const sampleOpenShift: CashShiftDto = {
    id: 'shift-open-uuid-1',
    tenantId: 'tenant-1',
    branchId: 'branch-100',
    openedBy: 'usr-1',
    openedByName: 'Juan Cajero',
    openedAt: '2026-10-29T08:00:00Z',
    initialCash: 200,
    totalCashSales: 0,
    totalCardSales: 0,
    totalOtherSales: 0,
    totalCashExpenses: 0,
    status: 'OPEN',
  };

  beforeEach(async () => {
    mockPosService = {
      isOnline: signal(true),
      pendingCount: signal(0),
      cartItems: signal<CartItem[]>([]),
      isSyncing: signal(false),
      isLoading: signal(false),
      categories: signal<CachedCategory[]>(sampleCategories),
      products: signal<CachedProduct[]>(sampleProducts),
      activePromotions: signal<PromotionDto[]>([]),
      selectedPromotion: signal<PromotionDto | null>(null),
      effectivePromotion: signal<PromotionDto | null>(null),
      rawSubtotal: signal(0),
      discountAmount: signal(0),
      totalAmount: signal(0),
      totalItemsCount: signal(0),
      loadCatalog: vi.fn().mockResolvedValue({ categories: sampleCategories, products: sampleProducts }),
      loadActivePromotions: vi.fn().mockResolvedValue([]),
      addToCart: vi.fn(),
      addComboToCart: vi.fn(),
      updateQuantity: vi.fn(),
      removeFromCart: vi.fn(),
      clearCart: vi.fn(),
      checkout: vi.fn(),
      syncPendingOrders: vi.fn(),
    };

    mockFinanceService = {
      currentShift: signal<CashShiftDto | null>(sampleOpenShift),
      hasOpenShift: signal<boolean>(true),
      isLoading: signal(false),
      errorMessage: signal(null),
      loadCurrentShift: vi.fn().mockReturnValue(of(sampleOpenShift)),
      openShift: vi.fn().mockReturnValue(of(sampleOpenShift)),
    };

    mockAuthService = {
      currentUser: vi.fn().mockReturnValue({
        id: 'usr-1',
        username: 'cajero1',
        fullName: 'Juan Cajero',
        branchId: 'branch-100',
        role: 'CASHIER',
        permissions: ['finance:shifts'],
      }),
      hasPrivilege: vi.fn().mockReturnValue(true),
    };

    mockCatalogService = {
      getCombos: vi.fn().mockReturnValue(of(sampleCombos)),
    };

    mockBranchContextService = {
      activeBranchId: signal<string | null>('branch-100'),
    };

    await TestBed.configureTestingModule({
      imports: [PosComponent],
      providers: [
        { provide: PosService, useValue: mockPosService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: CatalogService, useValue: mockCatalogService },
        { provide: BranchContextService, useValue: mockBranchContextService },
        { provide: FinanceService, useValue: mockFinanceService },
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
    it('debe listar todas las categorías incluyendo la opción "Todas" y "Combos / Packs"', () => {
      const cats = component.categoriesWithCount();
      expect(cats.length).toBe(4);
      expect(cats[0].id).toBe('all');
      expect(cats[0].count).toBe(4);
      expect(cats[1].id).toBe('combos');
      expect(cats[1].count).toBe(1);
      expect(cats[2].id).toBe('cat-1');
      expect(cats[2].count).toBe(2);
      expect(cats[3].id).toBe('cat-2');
      expect(cats[3].count).toBe(1);
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

  describe('Gestión y Selección de Combos', () => {
    it('debe listar combos en filteredCombos cuando la categoría es "all" o "combos"', () => {
      component.selectCategory('all');
      expect(component.filteredCombos().length).toBe(1);
      expect(component.filteredCombos()[0].code).toBe('PACK-01');

      component.selectCategory('combos');
      expect(component.filteredCombos().length).toBe(1);
      expect(component.filteredProducts().length).toBe(0);

      component.selectCategory('cat-1');
      expect(component.filteredCombos().length).toBe(0);
    });

    it('debe abrir modal de personalización si el combo tiene opciones CHOICE', () => {
      const comboWithChoice = sampleCombos[0];
      component.onComboClicked(comboWithChoice);

      expect(component.showComboModal()).toBe(true);
      expect(component.selectedCombo()).toBe(comboWithChoice);
      expect(component.comboSelections()['ci-2']).toBe('p-3');
    });

    it('debe permitir cambiar opción en modal y confirmar agregado al carrito', () => {
      const comboWithChoice = sampleCombos[0];
      component.onComboClicked(comboWithChoice);

      component.setComboOption('ci-2', 'p-4');
      expect(component.comboSelections()['ci-2']).toBe('p-4');

      component.confirmComboSelection();

      expect(mockPosService.addComboToCart).toHaveBeenCalledWith(
        comboWithChoice,
        1,
        ['p-1', 'p-4']
      );
      expect(component.showComboModal()).toBe(false);
      expect(component.selectedCombo()).toBeNull();
    });

    it('debe agregar directamente al carrito si el combo solo tiene items fijos', () => {
      const fixedCombo: ComboDto = {
        id: 'combo-fixed',
        categoryId: 'cat-1',
        code: 'PACK-FIXED',
        name: 'Combo Fijo',
        basePrice: 25.0,
        isActive: true,
        items: [
          {
            id: 'ci-f',
            name: 'Papas Fritas',
            selectionMode: 'FIXED',
            isRequired: true,
            quantity: 1,
            sortOrder: 1,
            options: [{ id: 'opt-f', dishId: 'p-1', dishName: 'Papas', priceAdjustment: 0, isDefault: true }],
          },
        ],
      };

      component.onComboClicked(fixedCombo);
      expect(mockPosService.addComboToCart).toHaveBeenCalledWith(fixedCombo);
      expect(component.showComboModal()).toBe(false);
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

      expect(mockPosService.checkout).toHaveBeenCalledWith(
        'CASH',
        'Sin cebolla',
        'branch-100',
        'shift-open-uuid-1'
      );
      expect(component.showCheckoutModal()).toBe(false);
      expect(component.showTicketModal()).toBe(true);
      expect(component.lastOrder()).toEqual(mockResult);
    });
  });

  describe('Promociones Temporales y Happy Hour', () => {
    it('debe mostrar banner de promoción activa si existe una vigente', () => {
      mockPosService.effectivePromotion.set({
        id: 'promo-hh',
        name: 'Happy Hour 20%',
        discountType: 'PERCENTAGE',
        discountValue: 20,
        scope: 'GLOBAL',
        applicationMode: 'AUTOMATIC',
        isActive: true,
      });
      fixture.detectChanges();

      const banner = fixture.nativeElement.querySelector('[data-testid="active-promotion-banner"]');
      expect(banner).toBeTruthy();
      expect(banner.textContent).toContain('Happy Hour 20%');
      expect(banner.textContent).toContain('20% de descuento');
    });

    it('debe renderizar desglose de descuento en el carrito cuando discountAmount > 0', () => {
      mockPosService.effectivePromotion.set({
        id: 'promo-hh',
        name: 'Happy Hour 20%',
        discountType: 'PERCENTAGE',
        discountValue: 20,
        scope: 'GLOBAL',
        applicationMode: 'AUTOMATIC',
        isActive: true,
      });
      mockPosService.rawSubtotal.set(100.0);
      mockPosService.discountAmount.set(20.0);
      mockPosService.totalAmount.set(80.0);
      fixture.detectChanges();

      const discountRow = fixture.nativeElement.querySelector('[data-testid="cart-discount-row"]');
      expect(discountRow).toBeTruthy();
      expect(discountRow.textContent).toContain('-Bs. 20.00');
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

  describe('Grupos de Modificadores y Extras (TASK-210)', () => {
    const productWithModifiers: CachedProduct = {
      id: 'p-custom-burger',
      categoryId: 'cat-1',
      code: 'HAMB-EXTRA',
      name: 'Hamburguesa Personalizable',
      salePrice: 30.0,
      isAvailable: true,
      categoryName: 'Hamburguesas',
      modifierGroups: [
        {
          id: 'grp-1',
          name: 'Extras de Queso y Salsas',
          minSelectable: 0,
          maxSelectable: 2,
          isRequired: false,
          modifiers: [
            {
              id: 'mod-1',
              groupId: 'grp-1',
              name: 'Extra Cheddar',
              additionalPrice: 5.0,
              quantityConsumed: 0.05,
              sortOrder: 1,
            },
            {
              id: 'mod-2',
              groupId: 'grp-1',
              name: 'Extra Salsa BBQ',
              additionalPrice: 2.5,
              quantityConsumed: 0.03,
              sortOrder: 2,
            },
          ],
        },
      ],
    };

    it('debe abrir modal de modificadores al hacer clic en un plato con grupos de extras', () => {
      component.addToCart(productWithModifiers);
      expect(component.showModifierModal()).toBe(true);
      expect(component.selectedDishForModifiers()?.id).toBe('p-custom-burger');
      expect(mockPosService.addToCart).not.toHaveBeenCalled();
    });

    it('debe permitir seleccionar y deseleccionar modificadores calculando el recargo', () => {
      component.addToCart(productWithModifiers);
      const mod1 = productWithModifiers.modifierGroups![0].modifiers[0];
      const mod2 = productWithModifiers.modifierGroups![0].modifiers[1];

      component.toggleModifier(mod1);
      expect(component.isModifierSelected('mod-1')).toBe(true);
      expect(component.modifierExtraTotal()).toBe(5.0);

      component.toggleModifier(mod2);
      expect(component.isModifierSelected('mod-2')).toBe(true);
      expect(component.modifierExtraTotal()).toBe(7.5);

      // Deseleccionar mod1
      component.toggleModifier(mod1);
      expect(component.isModifierSelected('mod-1')).toBe(false);
      expect(component.modifierExtraTotal()).toBe(2.5);
    });

    it('debe agregar el plato con los modificadores seleccionados al confirmar y cerrar modal', () => {
      component.addToCart(productWithModifiers);
      const mod1 = productWithModifiers.modifierGroups![0].modifiers[0];
      component.toggleModifier(mod1);

      component.confirmModifierSelection();

      expect(mockPosService.addToCart).toHaveBeenCalledWith(
        productWithModifiers,
        1,
        undefined,
        [
          {
            modifierId: 'mod-1',
            modifierName: 'Extra Cheddar',
            additionalPrice: 5.0,
            ingredientId: undefined,
            quantityConsumed: 0.05,
          },
        ]
      );
      expect(component.showModifierModal()).toBe(false);
      expect(component.notification()?.type).toBe('success');
    });

    it('debe validar selección mínima si el grupo de modificadores es requerido', () => {
      const requiredProduct: CachedProduct = {
        ...productWithModifiers,
        id: 'p-req',
        modifierGroups: [
          {
            id: 'grp-req',
            name: 'Punto de Cocción',
            minSelectable: 1,
            maxSelectable: 1,
            isRequired: true,
            modifiers: [
              { id: 'm-term-medio', name: 'Término Medio', additionalPrice: 0, sortOrder: 1 },
              { id: 'm-bien-cocido', name: 'Bien Cocido', additionalPrice: 0, sortOrder: 2 },
            ],
          },
        ],
      };

      component.addToCart(requiredProduct);
      component.confirmModifierSelection();

      // Debe fallar por minSelectable = 1
      expect(mockPosService.addToCart).not.toHaveBeenCalled();
      expect(component.notification()?.type).toBe('error');
      expect(component.notification()?.message).toContain('Debes seleccionar al menos 1 opción');
    });
  });

  describe('Adaptabilidad Móvil y Modo Lista Compacta (TASK-317 / PB-028)', () => {
    it('debe alternar entre modo cuadrícula (grid) y modo lista compacta (list)', () => {
      expect(component.catalogViewMode()).toBe('grid');
      expect(fixture.nativeElement.querySelector('[data-testid="catalog-grid-view"]')).toBeTruthy();

      const listBtn = fixture.nativeElement.querySelector(
        '[data-testid="view-mode-list-btn"]'
      ) as HTMLButtonElement;
      listBtn.click();
      fixture.detectChanges();

      expect(component.catalogViewMode()).toBe('list');
      expect(fixture.nativeElement.querySelector('[data-testid="catalog-list-view"]')).toBeTruthy();

      const gridBtn = fixture.nativeElement.querySelector(
        '[data-testid="view-mode-grid-btn"]'
      ) as HTMLButtonElement;
      gridBtn.click();
      fixture.detectChanges();

      expect(component.catalogViewMode()).toBe('grid');
    });

    it('debe mostrar la barra flotante de comanda cuando hay ítems y abrir el BottomSheet móvil', () => {
      expect(fixture.nativeElement.querySelector('[data-testid="floating-cart-bar"]')).toBeFalsy();

      mockPosService.cartItems.set([
        {
          dishId: 'p-1',
          code: 'HAMB-01',
          name: 'Hamburguesa Doble',
          unitPrice: 35.0,
          quantity: 2,
          subtotal: 70.0,
        },
      ]);
      mockPosService.totalItemsCount.set(2);
      mockPosService.totalAmount.set(70.0);
      fixture.detectChanges();

      const floatingBar = fixture.nativeElement.querySelector('[data-testid="floating-cart-bar"]');
      expect(floatingBar).toBeTruthy();

      const openCartBtn = fixture.nativeElement.querySelector(
        '[data-testid="open-mobile-cart-btn"]'
      ) as HTMLButtonElement;
      openCartBtn.click();
      fixture.detectChanges();

      expect(component.isMobileCartOpen()).toBe(true);

      const sheetCheckoutBtn = fixture.nativeElement.querySelector(
        '[data-testid="mobile-sheet-checkout-btn"]'
      ) as HTMLButtonElement;
      sheetCheckoutBtn.click();
      fixture.detectChanges();

      expect(component.isMobileCartOpen()).toBe(false);
      expect(component.showCheckoutModal()).toBe(true);
    });
  });

  describe('Blindaje Reactivo de Turno de Caja (TASK-403 / PB-041 / ADR-0013)', () => {
    it('debe invocar loadCurrentShift al inicializar o al cambiar la sucursal activa', () => {
      expect(mockFinanceService.loadCurrentShift).toHaveBeenCalled();
    });

    it('si no hay turno abierto, debe renderizar el banner accesible de Caja Cerrada', () => {
      mockFinanceService.hasOpenShift.set(false);
      mockFinanceService.currentShift.set(null);
      fixture.detectChanges();

      const banner = fixture.nativeElement.querySelector('[data-testid="closed-shift-banner"]');
      expect(banner).toBeTruthy();
      expect(banner.textContent).toContain('Caja Cerrada — Debes abrir un turno para comenzar a vender');
    });

    it('si no hay turno abierto, debe deshabilitar botones de catálogo y checkout', () => {
      mockFinanceService.hasOpenShift.set(false);
      fixture.detectChanges();

      const addBtn = fixture.nativeElement.querySelector('[data-testid="add-btn-HAMB-01"]') as HTMLButtonElement;
      expect(addBtn.disabled).toBe(true);

      const addComboBtn = fixture.nativeElement.querySelector('[data-testid="add-combo-btn-PACK-01"]') as HTMLButtonElement;
      expect(addComboBtn.disabled).toBe(true);

      const checkoutBtn = fixture.nativeElement.querySelector('[data-testid="open-checkout-btn"]') as HTMLButtonElement;
      expect(checkoutBtn.disabled).toBe(true);
    });

    it('si no hay turno abierto, debe bloquear agregar platos o combos e informar con notificación', () => {
      mockFinanceService.hasOpenShift.set(false);
      fixture.detectChanges();

      component.addToCart(sampleProducts[0]);
      expect(mockPosService.addToCart).not.toHaveBeenCalled();
      expect(component.notification()?.type).toBe('warning');

      component.onComboClicked(sampleCombos[0]);
      expect(mockPosService.addComboToCart).not.toHaveBeenCalled();
      expect(component.notification()?.type).toBe('warning');
    });

    it('si no hay turno abierto, debe bloquear abrir checkout y confirmCheckout', async () => {
      mockFinanceService.hasOpenShift.set(false);
      fixture.detectChanges();

      component.openCheckoutModal();
      expect(component.showCheckoutModal()).toBe(false);

      await component.confirmCheckout();
      expect(mockPosService.checkout).not.toHaveBeenCalled();
      expect(component.notification()?.type).toBe('error');
    });

    it('debe permitir apertura rápida de turno mediante modal y emitir openShift', () => {
      mockFinanceService.hasOpenShift.set(false);
      fixture.detectChanges();

      const ctaBtn = fixture.nativeElement.querySelector('[data-testid="open-shift-cta-btn"]') as HTMLButtonElement;
      expect(ctaBtn).toBeTruthy();
      ctaBtn.click();
      fixture.detectChanges();

      expect(component.showOpenShiftModal()).toBe(true);
      expect(fixture.nativeElement.querySelector('[data-testid="open-shift-modal"]')).toBeTruthy();

      component.initialCashInput.set(300);
      component.confirmOpenShift();

      expect(mockFinanceService.openShift).toHaveBeenCalledWith({
        branchId: 'branch-100',
        initialCash: 300,
      });
      expect(component.showOpenShiftModal()).toBe(false);
      expect(component.notification()?.type).toBe('success');
    });

    it('debe mostrar mensaje de supervisor si el colaborador no posee permiso para abrir turno', () => {
      mockFinanceService.hasOpenShift.set(false);
      mockAuthService.hasPrivilege.mockReturnValue(false);
      mockAuthService.currentUser.mockReturnValue({
        id: 'usr-regular',
        username: 'mesero1',
        role: 'WAITER',
        permissions: [],
      });
      fixture.detectChanges();

      expect(component.canOpenShift()).toBe(false);
      const banner = fixture.nativeElement.querySelector('[data-testid="closed-shift-banner"]');
      expect(banner.textContent).toContain('Esperando apertura de turno por parte de un supervisor');
      expect(fixture.nativeElement.querySelector('[data-testid="open-shift-cta-btn"]')).toBeFalsy();
    });
  });

  describe('Ergonomía Móvil y Barra de Advertencia Offline (ADR-0013)', () => {
    it('no debe mostrar barra de advertencia en estado normal (en línea y 0 pendientes)', () => {
      mockPosService.isOnline.set(true);
      mockPosService.pendingCount.set(0);
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-testid="offline-warning-bar"]')).toBeFalsy();
    });

    it('debe desplegar barra ámbar de advertencia al estar sin conexión (!isOnline)', () => {
      mockPosService.isOnline.set(false);
      mockPosService.pendingCount.set(0);
      fixture.detectChanges();

      const warningBar = fixture.nativeElement.querySelector('[data-testid="offline-warning-bar"]');
      expect(warningBar).toBeTruthy();
      expect(warningBar.textContent).toContain('Modo Offline (Sin conexión) · 0 órdenes locales guardadas en terminal');
    });

    it('debe desplegar barra ámbar de advertencia si hay órdenes pendientes en terminal', () => {
      mockPosService.isOnline.set(true);
      mockPosService.pendingCount.set(2);
      fixture.detectChanges();

      const warningBar = fixture.nativeElement.querySelector('[data-testid="offline-warning-bar"]');
      expect(warningBar).toBeTruthy();
      expect(warningBar.textContent).toContain('Modo Offline (Sin conexión) · 2 órdenes locales guardadas en terminal');

      const syncBtn = fixture.nativeElement.querySelector('[data-testid="sync-pending-btn"]');
      expect(syncBtn).toBeTruthy();
    });
  });

  describe('Ticket Visual de Comanda con Identificación de Cajero (ADR-0013)', () => {
    it('debe mostrar el nombre del cajero en sesión activa en el ticket visual de venta', () => {
      component.lastOrder.set({
        id: 'order-ticket-1',
        branchId: 'branch-100',
        cashShiftId: 'shift-open-uuid-1',
        cashierId: 'usr-1',
        ticketNumber: 'TKT-9999',
        orderStatus: 'PAID',
        paymentMethod: 'CASH',
        totalAmount: 50.0,
        createdAt: '2026-10-29T10:00:00Z',
        items: [
          {
            dishId: 'p-1',
            dishName: 'Hamburguesa Doble',
            quantity: 1,
            unitPrice: 35.0,
            subtotal: 35.0,
          },
        ],
      });
      component.showTicketModal.set(true);
      fixture.detectChanges();

      const ticketModal = fixture.nativeElement.querySelector('[data-testid="ticket-modal"]');
      expect(ticketModal).toBeTruthy();

      const cashierElem = fixture.nativeElement.querySelector('[data-testid="ticket-cashier-name"]');
      expect(cashierElem).toBeTruthy();
      expect(cashierElem.textContent).toContain('Juan Cajero');
    });
  });
});
