import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ParametersComponent } from './parameters.component';
import { ConfigurationService } from '../../../core/services/configuration.service';
import { BranchContextService } from '../../../core/services/branch-context.service';
import { AuthService } from '../../../core/services/auth.service';
import { SystemParameterDto } from '../../../core/models/configuration.model';

describe('ParametersComponent', () => {
  let component: ParametersComponent;
  let fixture: ComponentFixture<ParametersComponent>;

  const mockParams: SystemParameterDto[] = [
    {
      key: 'pos.display_dish_images',
      name: 'Mostrar Imágenes en Pantalla POS',
      description: 'Alterna entre botonera visual o compacta',
      category: 'POS',
      type: 'BOOLEAN',
      effectiveValue: 'true',
      defaultValue: 'true',
      scope: 'BRANCH',
      inheritedFrom: 'BRANCH',
      isOverridden: false,
      criticality: 'LOW',
      requiresRestart: false,
      options: ['true', 'false'],
    },
    {
      key: 'inventory.deduction_mode',
      name: 'Modo de Deducción de Inventario',
      description: 'Define la política de descuento en Kardex',
      category: 'INVENTORY',
      type: 'ENUM',
      effectiveValue: 'AUTOMATIC_PERMISSIVE',
      defaultValue: 'AUTOMATIC_PERMISSIVE',
      scope: 'TENANT',
      inheritedFrom: 'TENANT',
      isOverridden: false,
      criticality: 'HIGH',
      requiresRestart: false,
      options: ['MANUAL_ISOLATED', 'AUTOMATIC_PERMISSIVE', 'AUTOMATIC_STRICT'],
    },
    {
      key: 'finance.max_petty_cash_expense',
      name: 'Límite Máximo de Gasto Menor',
      description: 'Monto máximo sin PIN de supervisor',
      category: 'FINANCE',
      type: 'DOUBLE',
      effectiveValue: '100.00',
      defaultValue: '100.00',
      scope: 'BRANCH',
      inheritedFrom: 'BRANCH',
      isOverridden: true,
      criticality: 'MEDIUM',
      requiresRestart: false,
      options: [],
    },
  ];

  let configServiceMock: {
    parameters: ReturnType<typeof vi.fn>;
    isLoading: ReturnType<typeof vi.fn>;
    selectedScope: ReturnType<typeof vi.fn>;
    selectedCategory: ReturnType<typeof vi.fn>;
    searchQuery: ReturnType<typeof vi.fn>;
    filteredParameters: ReturnType<typeof vi.fn>;
    loadParameters: ReturnType<typeof vi.fn>;
    updateParameter: ReturnType<typeof vi.fn>;
    resetParameter: ReturnType<typeof vi.fn>;
    setSearchQuery: ReturnType<typeof vi.fn>;
    setSelectedCategory: ReturnType<typeof vi.fn>;
    setSelectedScope: ReturnType<typeof vi.fn>;
  };

  let branchContextMock: {
    activeBranchId: ReturnType<typeof vi.fn>;
  };

  let authServiceMock: {
    hasPermission: ReturnType<typeof vi.fn>;
    hasRole: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    configServiceMock = {
      parameters: vi.fn().mockReturnValue(mockParams),
      isLoading: vi.fn().mockReturnValue(false),
      selectedScope: vi.fn().mockReturnValue('BRANCH'),
      selectedCategory: vi.fn().mockReturnValue('ALL'),
      searchQuery: vi.fn().mockReturnValue(''),
      filteredParameters: vi.fn().mockReturnValue(mockParams),
      loadParameters: vi.fn().mockReturnValue(of(mockParams)),
      updateParameter: vi.fn().mockReturnValue(of(mockParams[0])),
      resetParameter: vi.fn().mockReturnValue(of(undefined)),
      setSearchQuery: vi.fn(),
      setSelectedCategory: vi.fn(),
      setSelectedScope: vi.fn(),
    };

    branchContextMock = {
      activeBranchId: vi.fn().mockReturnValue('branch-123'),
    };

    authServiceMock = {
      hasPermission: vi.fn().mockReturnValue(true),
      hasRole: vi.fn().mockReturnValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [ParametersComponent],
      providers: [
        provideRouter([]),
        { provide: ConfigurationService, useValue: configServiceMock },
        { provide: BranchContextService, useValue: branchContextMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ParametersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse y cargar parámetros en ngOnInit', () => {
    expect(component).toBeTruthy();
    expect(configServiceMock.loadParameters).toHaveBeenCalledWith('BRANCH', null, 'branch-123');
    expect(component.editValues()['pos.display_dish_images']).toBe('true');
  });

  it('debe conmutar el selector de ámbito (Scope Switcher)', () => {
    component.setScope('TENANT');
    expect(configServiceMock.setSelectedScope).toHaveBeenCalledWith('TENANT');
    expect(configServiceMock.loadParameters).toHaveBeenCalled();
  });

  it('debe filtrar por categoría funcional', () => {
    component.setCategory('POS');
    expect(configServiceMock.setSelectedCategory).toHaveBeenCalledWith('POS');
  });

  it('debe filtrar en tiempo real con debounce de 250ms', () => {
    vi.useFakeTimers();
    try {
      component.onSearchChange('kardex');
      vi.advanceTimersByTime(100);
      expect(configServiceMock.setSearchQuery).not.toHaveBeenCalled();

      vi.advanceTimersByTime(160);
      expect(configServiceMock.setSearchQuery).toHaveBeenCalledWith('kardex');
    } finally {
      vi.useRealTimers();
    }
  });

  it('debe actualizar parámetro booleano de baja criticidad directamente', () => {
    component.toggleBoolean(mockParams[0]);

    expect(configServiceMock.updateParameter).toHaveBeenCalledWith(
      'pos.display_dish_images',
      expect.objectContaining({
        value: 'false',
        scope: 'BRANCH',
        branchId: 'branch-123',
      })
    );
  });

  it('debe abrir modal de criticidad alta (HIGH) al modificar inventory.deduction_mode', () => {
    component.openCriticalModal(mockParams[1], 'AUTOMATIC_STRICT');

    expect(component.isCriticalModalOpen()).toBe(true);
    expect(component.criticalParam()).toEqual(mockParams[1]);
    expect(component.criticalNewValue()).toBe('AUTOMATIC_STRICT');
  });

  it('debe validar contraseña y justificación >= 10 caracteres antes de confirmar cambio crítico', () => {
    component.openCriticalModal(mockParams[1], 'AUTOMATIC_STRICT');

    // Sin contraseña
    component.passwordConfirmation.set('');
    component.auditReason.set('Justificación suficiente');
    component.confirmCriticalChange();
    expect(component.criticalError()).toContain('contraseña');
    expect(configServiceMock.updateParameter).not.toHaveBeenCalled();

    // Con justificación corta
    component.passwordConfirmation.set('admin123');
    component.auditReason.set('Corto');
    component.confirmCriticalChange();
    expect(component.criticalError()).toContain('10 caracteres');
    expect(configServiceMock.updateParameter).not.toHaveBeenCalled();

    // Con datos válidos
    component.passwordConfirmation.set('admin123');
    component.auditReason.set('Cierre mensual de inventario estricto');
    component.confirmCriticalChange();

    expect(configServiceMock.updateParameter).toHaveBeenCalledWith(
      'inventory.deduction_mode',
      expect.objectContaining({
        value: 'AUTOMATIC_STRICT',
        passwordConfirmation: 'admin123',
        auditReason: 'Cierre mensual de inventario estricto',
      })
    );
    expect(component.isCriticalModalOpen()).toBe(false);
  });

  it('debe restablecer parámetro al valor corporativo por defecto', () => {
    component.resetToDefault(mockParams[2]);

    expect(configServiceMock.resetParameter).toHaveBeenCalledWith(
      'finance.max_petty_cash_expense',
      'BRANCH',
      'branch-123'
    );
  });
});
