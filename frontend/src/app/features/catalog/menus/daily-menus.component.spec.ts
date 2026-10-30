import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DailyMenusComponent } from './daily-menus.component';
import { CatalogService } from '../../../core/services/catalog.service';
import { AuthService } from '../../../core/services/auth.service';
import { BranchContextService } from '../../../core/services/branch-context.service';
import { DailyMenuDto, DishPriceDto } from '../../../core/models/catalog.models';

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
      basePrice: 25.0,
      isActive: true,
      isTemplate: true,
      items: [
        {
          id: 'item-1',
          dishId: 'dish-1',
          dishName: 'Sopa de Verduras',
          dishCode: 'SOP-01',
          courseType: 'ENTRADA',
          suggestedQuota: 30,
          priceMode: 'INCLUDED',
          surcharge: 0,
          sortOrder: 1,
        },
        {
          id: 'item-2',
          dishId: 'dish-2',
          dishName: 'Milanesa con Papas',
          dishCode: 'MIL-01',
          courseType: 'SEGUNDO',
          suggestedQuota: 25,
          priceMode: 'INCLUDED',
          surcharge: 0,
          sortOrder: 2,
        },
      ],
    },
    {
      id: 'menu-2',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
      name: 'Menú Especial Fin de Semana',
      description: 'Platos premium',
      dayOfWeek: 'DOMINGO',
      basePrice: 35.0,
      isActive: true,
      isTemplate: true,
      items: [],
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

  let catalogServiceSpy: {
    getDailyMenus: ReturnType<typeof vi.fn>;
    getDishes: ReturnType<typeof vi.fn>;
    createDailyMenu: ReturnType<typeof vi.fn>;
  };

  let authServiceSpy: {
    currentUser: ReturnType<typeof vi.fn>;
    hasPermission: ReturnType<typeof vi.fn>;
  };

  let branchContextServiceSpy: {
    activeBranchId: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    catalogServiceSpy = {
      getDailyMenus: vi.fn().mockReturnValue(of(mockMenus)),
      getDishes: vi.fn().mockReturnValue(of(mockDishes)),
      createDailyMenu: vi.fn(),
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

    branchContextServiceSpy = {
      activeBranchId: vi.fn().mockReturnValue('branch-1'),
    };

    await TestBed.configureTestingModule({
      imports: [DailyMenusComponent],
      providers: [
        provideRouter([]),
        { provide: CatalogService, useValue: catalogServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: BranchContextService, useValue: branchContextServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DailyMenusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse e inicializar la lista de plantillas de menú', () => {
    expect(component).toBeTruthy();
    expect(component.menus().length).toBe(2);
    expect(catalogServiceSpy.getDailyMenus).toHaveBeenCalledWith('branch-1', false);
    expect(catalogServiceSpy.getDishes).toHaveBeenCalledWith('branch-1');
  });

  it('debe filtrar plantillas por término de búsqueda', () => {
    component.searchQuery.set('Ejecutivo');
    expect(component.filteredMenus().length).toBe(1);
    expect(component.filteredMenus()[0].name).toContain('Ejecutivo');

    component.searchQuery.set('Inexistente');
    expect(component.filteredMenus().length).toBe(0);
  });

  it('debe filtrar plantillas por día de la semana', () => {
    component.selectedDayFilter.set('LUNES');
    expect(component.filteredMenus().length).toBe(1);
    expect(component.filteredMenus()[0].dayOfWeek).toBe('LUNES');

    component.selectedDayFilter.set('DOMINGO');
    expect(component.filteredMenus().length).toBe(1);
    expect(component.filteredMenus()[0].dayOfWeek).toBe('DOMINGO');

    component.selectedDayFilter.set('MARTES');
    expect(component.filteredMenus().length).toBe(0);
  });

  it('debe expandir y colapsar el detalle de platos de un menú', () => {
    expect(component.expandedMenuId()).toBeNull();

    component.toggleExpand('menu-1');
    expect(component.expandedMenuId()).toBe('menu-1');

    component.toggleExpand('menu-1');
    expect(component.expandedMenuId()).toBeNull();
  });

  it('debe abrir modal y resetear campos del formulario', () => {
    component.openCreateModal();
    expect(component.showCreateModal()).toBe(true);
    expect(component.formName()).toBe('');
    expect(component.formBasePrice()).toBe(25);
    expect(component.formItems().length).toBe(0);
  });

  it('debe agregar y remover ítems al formulario de nueva plantilla', () => {
    component.openCreateModal();
    component.selectedDishId.set('dish-1');
    component.selectedCourseType.set('ENTRADA');
    component.selectedSuggestedQuota.set(30);

    component.addItemToForm();
    expect(component.formItems().length).toBe(1);
    expect(component.formItems()[0].dishId).toBe('dish-1');
    expect(component.formItems()[0].courseType).toBe('ENTRADA');
    expect(component.formItems()[0].suggestedQuota).toBe(30);

    // Intentar agregar el mismo plato debe generar error de validación
    component.addItemToForm();
    expect(component.formItems().length).toBe(1);
    expect(component.errorMessage()).toContain('ya ha sido agregado');

    // Remover ítem
    component.removeItemFromForm(0);
    expect(component.formItems().length).toBe(0);
  });

  it('debe validar y guardar exitosamente una nueva plantilla de menú', () => {
    component.openCreateModal();
    component.formName.set('Menú Miércoles');
    component.formDayOfWeek.set('MIERCOLES');
    component.formBasePrice.set(28.0);
    component.selectedDishId.set('dish-2');
    component.selectedCourseType.set('SEGUNDO');
    component.selectedSuggestedQuota.set(20);
    component.addItemToForm();

    const createdMock: DailyMenuDto = {
      id: 'menu-new',
      name: 'Menú Miércoles',
      dayOfWeek: 'MIERCOLES',
      basePrice: 28.0,
      isActive: true,
      isTemplate: true,
      items: [],
    };
    catalogServiceSpy.createDailyMenu.mockReturnValue(of(createdMock));

    component.saveMenu();

    expect(catalogServiceSpy.createDailyMenu).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Menú Miércoles',
        dayOfWeek: 'MIERCOLES',
        basePrice: 28.0,
        isTemplate: true,
        items: expect.arrayContaining([
          expect.objectContaining({
            dishId: 'dish-2',
            courseType: 'SEGUNDO',
            suggestedQuota: 20,
          }),
        ]),
      })
    );
    expect(component.showCreateModal()).toBe(false);
    expect(component.successMessage()).toContain('Menú Miércoles');
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
