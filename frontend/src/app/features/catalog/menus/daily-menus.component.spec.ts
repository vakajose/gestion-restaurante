import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DailyMenusComponent } from './daily-menus.component';
import { CatalogService } from '../../../core/services/catalog.service';
import { AuthService } from '../../../core/services/auth.service';
import { BranchContextService } from '../../../core/services/branch-context.service';
import { BranchServiceSessionDto, ComboDto, DailyMenuDto, DishPriceDto } from '../../../core/models/catalog.models';

describe('DailyMenusComponent', () => {
  let component: DailyMenusComponent;
  let fixture: ComponentFixture<DailyMenusComponent>;

  const mockMenus: DailyMenuDto[] = [
    {
      id: 'menu-1',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
      name: 'Menú Ejecutivo Lunes',
      description: 'Entrada, segundo y postre',
      dayOfWeek: 'LUNES',
      priceMode: 'FIXED_PACKAGE',
      packagePrice: 25.0,
      isActive: true,
      isTemplate: true,
      items: [
        {
          id: 'item-1',
          dishId: 'dish-1',
          dishName: 'Sopa de Verduras',
          dishCode: 'SOP-01',
          courseType: 'STARTER',
          suggestedQuota: 30,
          overridePrice: null,
          isMandatoryInPackage: false,
          sortOrder: 1,
          allowIndividualSale: true,
          isUnlimited: false,
        },
        {
          id: 'item-2',
          dishId: 'dish-2',
          dishName: 'Milanesa con Papas',
          dishCode: 'MIL-01',
          courseType: 'MAIN',
          suggestedQuota: 25,
          overridePrice: null,
          isMandatoryInPackage: false,
          sortOrder: 2,
          allowIndividualSale: true,
          isUnlimited: false,
        },
      ],
      comboIds: [],
    },
    {
      id: 'menu-2',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
      name: 'Menú Especial Fin de Semana',
      description: 'Platos premium',
      dayOfWeek: 'DOMINGO',
      priceMode: 'INDIVIDUAL_ITEMS',
      packagePrice: null,
      isActive: true,
      isTemplate: true,
      items: [],
      comboIds: [],
    },
  ];

  const mockDishes: DishPriceDto[] = [
    {
      dishId: 'dish-1',
      code: 'SOP-01',
      name: 'Sopa de Verduras',
      categoryId: 'cat-1',
      basePrice: 15.0,
      effectivePrice: 15.0,
      hasOverride: false,
      isAvailable: true,
    },
    {
      dishId: 'dish-2',
      code: 'MIL-01',
      name: 'Milanesa con Papas',
      categoryId: 'cat-1',
      basePrice: 25.0,
      effectivePrice: 25.0,
      hasOverride: false,
      isAvailable: true,
    },
  ];

  const mockCombos: ComboDto[] = [
    {
      id: 'combo-1',
      categoryId: 'cat-1',
      code: 'ALM-01',
      name: 'Almuerzo Completo',
      basePrice: 25,
      isActive: true,
      items: [],
    },
  ];

  let catalogServiceSpy: {
    getDailyMenus: ReturnType<typeof vi.fn>;
    getDishes: ReturnType<typeof vi.fn>;
    getCombos: ReturnType<typeof vi.fn>;
    getCurrentServiceSession: ReturnType<typeof vi.fn>;
    configureServiceSession: ReturnType<typeof vi.fn>;
    closeServiceSession: ReturnType<typeof vi.fn>;
    createDailyMenu: ReturnType<typeof vi.fn>;
    updateDailyMenu: ReturnType<typeof vi.fn>;
    deleteDailyMenu: ReturnType<typeof vi.fn>;
    cloneDailyMenu: ReturnType<typeof vi.fn>;
  };

  let authServiceSpy: {
    currentUser: ReturnType<typeof vi.fn>;
    hasPermission: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    catalogServiceSpy = {
      getDailyMenus: vi.fn().mockReturnValue(of(mockMenus)),
      getDishes: vi.fn().mockReturnValue(of(mockDishes)),
      getCombos: vi.fn().mockReturnValue(of(mockCombos)),
      getCurrentServiceSession: vi.fn().mockReturnValue(of(null)),
      configureServiceSession: vi.fn(),
      closeServiceSession: vi.fn(),
      createDailyMenu: vi.fn(),
      updateDailyMenu: vi.fn(),
      deleteDailyMenu: vi.fn(),
      cloneDailyMenu: vi.fn(),
    };

    authServiceSpy = {
      currentUser: vi.fn().mockReturnValue({
        id: 'user-1',
        username: 'admin',
        branchId: 'branch-1',
        role: 'ADMIN_TENANT',
      }),
      hasPermission: vi.fn().mockReturnValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [DailyMenusComponent],
      providers: [
        provideRouter([]),
        { provide: CatalogService, useValue: catalogServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        {
          provide: BranchContextService,
          useValue: {
            activeBranchId: vi.fn().mockReturnValue('branch-1'),
            branches: vi.fn().mockReturnValue([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DailyMenusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse e inicializar la lista de plantillas, combos y jornada actual', () => {
    expect(component).toBeTruthy();
    expect(component.menus().length).toBe(2);
    expect(catalogServiceSpy.getDailyMenus).toHaveBeenCalledWith('branch-1', false);
    expect(catalogServiceSpy.getDishes).toHaveBeenCalledWith('branch-1');
    expect(catalogServiceSpy.getCombos).toHaveBeenCalledWith('branch-1');
    expect(catalogServiceSpy.getCurrentServiceSession).toHaveBeenCalledWith('branch-1');
  });

  it('debe filtrar plantillas por término de búsqueda y día', () => {
    component.searchQuery.set('Ejecutivo');
    expect(component.filteredMenus().length).toBe(1);

    component.searchQuery.set('');
    component.selectedDayFilter.set('DOMINGO');
    expect(component.filteredMenus().length).toBe(1);
    expect(component.filteredMenus()[0].id).toBe('menu-2');
  });

  it('debe agregar ítems con stock ilimitado o venta suelta restringida y guardar plantilla', () => {
    component.openCreateModal();
    component.formName.set('Menú Miércoles');
    component.formDayOfWeek.set('MIERCOLES');
    component.formPriceMode.set('FIXED_PACKAGE');
    component.formPackagePrice.set(28.0);
    component.selectedDishId.set('dish-2');
    component.selectedCourseType.set('MAIN');
    component.selectedSuggestedQuota.set(20);
    component.selectedAllowIndividualSale.set(false);
    component.selectedIsUnlimited.set(true);
    component.addItemToForm();
    component.toggleComboSelection('combo-1');

    const createdMock: DailyMenuDto = {
      id: 'menu-new',
      name: 'Menú Miércoles',
      dayOfWeek: 'MIERCOLES',
      priceMode: 'FIXED_PACKAGE',
      packagePrice: 28.0,
      isActive: true,
      isTemplate: true,
      items: [],
      comboIds: ['combo-1'],
    };
    catalogServiceSpy.createDailyMenu.mockReturnValue(of(createdMock));

    component.saveMenu();

    expect(catalogServiceSpy.createDailyMenu).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Menú Miércoles',
        priceMode: 'FIXED_PACKAGE',
        packagePrice: 28.0,
        comboIds: ['combo-1'],
        items: expect.arrayContaining([
          expect.objectContaining({
            dishId: 'dish-2',
            courseType: 'MAIN',
            allowIndividualSale: false,
            isUnlimited: true,
          }),
        ]),
      })
    );
    expect(component.showCreateModal()).toBe(false);
  });

  it('debe editar, clonar y eliminar una plantilla de menú existente', () => {
    component.openEditModal(mockMenus[0]);
    expect(component.editingMenuId()).toBe('menu-1');
    component.formName.set('Menú Lunes Actualizado');
    catalogServiceSpy.updateDailyMenu.mockReturnValue(of({ ...mockMenus[0], name: 'Menú Lunes Actualizado' }));
    component.saveMenu();
    expect(catalogServiceSpy.updateDailyMenu).toHaveBeenCalledWith('menu-1', expect.objectContaining({ name: 'Menú Lunes Actualizado' }));

    component.openCloneModal(mockMenus[0]);
    catalogServiceSpy.cloneDailyMenu.mockReturnValue(of({ ...mockMenus[0], id: 'menu-clone', name: 'Menú Lunes (Copia)' }));
    component.confirmCloneMenu();
    expect(catalogServiceSpy.cloneDailyMenu).toHaveBeenCalled();

    component.requestDeleteMenu(mockMenus[1]);
    catalogServiceSpy.deleteDailyMenu.mockReturnValue(of(void 0));
    component.confirmDeleteMenu();
    expect(catalogServiceSpy.deleteDailyMenu).toHaveBeenCalledWith('menu-2');
  });

  it('debe configurar y cerrar la jornada operativa de sucursal', () => {
    const savedSession: BranchServiceSessionDto = {
      id: 'sess-1',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
      serviceDate: '2026-11-17',
      menuMode: 'DAILY_MENU',
      dailyMenuId: 'menu-1',
      dailyMenuName: 'Menú Ejecutivo Lunes',
      status: 'CONFIGURED',
      createdAt: '2026-11-17T10:00:00Z',
    };
    catalogServiceSpy.configureServiceSession.mockReturnValue(of(savedSession));

    component.openSessionModal();
    component.sessionMenuMode.set('DAILY_MENU');
    component.sessionDailyMenuId.set('menu-1');
    component.saveServiceSession();

    expect(catalogServiceSpy.configureServiceSession).toHaveBeenCalledWith(
      expect.objectContaining({
        branchId: 'branch-1',
        menuMode: 'DAILY_MENU',
        dailyMenuId: 'menu-1',
      })
    );
    expect(component.currentSession()?.id).toBe('sess-1');

    catalogServiceSpy.closeServiceSession.mockReturnValue(of({ ...savedSession, status: 'CLOSED' }));
    component.closeCurrentServiceSession();
    expect(catalogServiceSpy.closeServiceSession).toHaveBeenCalledWith('sess-1');
    expect(component.currentSession()).toBeNull();
  });

  it('debe manejar errores del servidor al crear plantilla', () => {
    component.openCreateModal();
    component.formName.set('Menú Falla');
    component.selectedDishId.set('dish-1');
    component.addItemToForm();

    catalogServiceSpy.createDailyMenu.mockReturnValue(
      throwError(() => ({ error: { detail: 'Error de prueba en base de datos' } }))
    );

    component.saveMenu();

    expect(component.isSaving()).toBe(false);
    expect(component.errorMessage()).toBe('Error de prueba en base de datos');
  });
});
