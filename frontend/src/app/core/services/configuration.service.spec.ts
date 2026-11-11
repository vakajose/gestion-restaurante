import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ConfigurationService } from './configuration.service';
import { SystemParameterDto, UpdateParameterRequest } from '../models/configuration.model';

describe('ConfigurationService', () => {
  let service: ConfigurationService;
  let httpTesting: HttpTestingController;

  const mockParameters: SystemParameterDto[] = [
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
      options: [
        { value: 'true', label: 'Activado' },
        { value: 'false', label: 'Desactivado' },
      ],
      help: {
        impact: 'Alterna la botonera visual con fotos.',
        example: 'Ideal para cajeros novatos.',
      },
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
      options: [
        { value: 'MANUAL_ISOLATED', label: 'Aislado / Manual', description: 'No descuenta insumos' },
        { value: 'AUTOMATIC_PERMISSIVE', label: 'Automático permisivo', description: 'Descuenta stock' },
        { value: 'AUTOMATIC_STRICT', label: 'Automático estricto', description: 'Bloquea comanda' },
      ],
      help: {
        impact: 'Determina si el sistema permite cobrar ventas cuando no hay existencias.',
        example: 'Si vendes una hamburguesa y no hay pan, el modo estricto bloquea la comanda.',
      },
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
      isOverridden: false,
      criticality: 'MEDIUM',
      requiresRestart: false,
      options: [],
      help: {
        impact: 'Tope máximo de dinero en efectivo de caja menor.',
        example: 'Comprar hielo por 45 BOB no exige PIN.',
      },
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ConfigurationService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(ConfigurationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe inicializarse con lista vacía y selectedScope en BRANCH', () => {
    expect(service.parameters()).toEqual([]);
    expect(service.selectedScope()).toBe('BRANCH');
    expect(service.selectedCategory()).toBe('ALL');
    expect(service.searchQuery()).toBe('');
  });

  it('debe cargar parámetros mediante GET /api/v1/parameters y almacenar ETag', () => {
    service.loadParameters('BRANCH', null, 'branch-1').subscribe((res) => {
      expect(res.length).toBe(3);
      expect(service.parameters().length).toBe(3);
    });

    const req = httpTesting.expectOne((r) => r.url === '/api/v1/parameters');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('scope')).toBe('BRANCH');
    expect(req.request.params.get('branchId')).toBe('branch-1');

    req.flush(mockParameters, {
      headers: { ETag: '"test-etag-123"' },
      status: 200,
      statusText: 'OK',
    });
  });

  it('debe reutilizar datos en caché cuando el backend responde HTTP 304 Not Modified', () => {
    // 1. Carga inicial con ETag
    service.loadParameters('BRANCH', 'POS').subscribe();
    const req1 = httpTesting.expectOne((r) => r.url === '/api/v1/parameters');
    req1.flush([mockParameters[0]], {
      headers: { ETag: '"etag-pos"' },
      status: 200,
      statusText: 'OK',
    });

    expect(service.parameters().length).toBe(1);

    // 2. Segunda llamada envía If-None-Match
    service.loadParameters('BRANCH', 'POS').subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].key).toBe('pos.display_dish_images');
    });

    const req2 = httpTesting.expectOne((r) => r.url === '/api/v1/parameters');
    expect(req2.request.headers.get('If-None-Match')).toBe('"etag-pos"');
    req2.flush(null, { status: 304, statusText: 'Not Modified' });
  });

  it('debe mutar parámetro atómicamente mediante PATCH /api/v1/parameters/{key} y actualizar signal', () => {
    service.parameters.set(mockParameters);

    const updateRequest: UpdateParameterRequest = {
      scope: 'BRANCH',
      branchId: 'branch-1',
      value: 'false',
    };

    const updatedDto: SystemParameterDto = {
      ...mockParameters[0],
      effectiveValue: 'false',
      isOverridden: true,
    };

    service.updateParameter('pos.display_dish_images', updateRequest).subscribe((res) => {
      expect(res.effectiveValue).toBe('false');
      expect(res.isOverridden).toBe(true);
    });

    const req = httpTesting.expectOne('/api/v1/parameters/pos.display_dish_images');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual(updateRequest);
    req.flush(updatedDto);

    // Verificar actualización reactiva del signal
    const updatedInSignal = service.parameters().find((p) => p.key === 'pos.display_dish_images');
    expect(updatedInSignal?.effectiveValue).toBe('false');
    expect(updatedInSignal?.isOverridden).toBe(true);
  });

  it('debe restablecer parámetro mediante DELETE /api/v1/parameters/{key}', () => {
    service.resetParameter('pos.display_dish_images', 'BRANCH', 'branch-1').subscribe();

    const req = httpTesting.expectOne((r) => r.url === '/api/v1/parameters/pos.display_dish_images');
    expect(req.request.method).toBe('DELETE');
    expect(req.request.params.get('scope')).toBe('BRANCH');
    expect(req.request.params.get('branchId')).toBe('branch-1');
    req.flush(null, { status: 204, statusText: 'No Content' });

    // Se dispara recarga subsecuente
    const reloadReq = httpTesting.expectOne((r) => r.url === '/api/v1/parameters');
    reloadReq.flush(mockParameters);
  });

  it('debe filtrar parámetros reactivamente mediante computed filteredParameters (búsqueda multicriterio)', () => {
    service.parameters.set(mockParameters);

    // Sin filtro: 3 parámetros
    expect(service.filteredParameters().length).toBe(3);

    // Filtro por categoría POS: 1 parámetro
    service.setSelectedCategory('POS');
    expect(service.filteredParameters().length).toBe(1);
    expect(service.filteredParameters()[0].key).toBe('pos.display_dish_images');

    // Volver a ALL y filtrar por búsqueda en clave/descripción
    service.setSelectedCategory('ALL');
    service.setSearchQuery('kardex');
    expect(service.filteredParameters().length).toBe(1);
    expect(service.filteredParameters()[0].key).toBe('inventory.deduction_mode');

    // Búsqueda por etiqueta de opción humanizada
    service.setSearchQuery('automático permisivo');
    expect(service.filteredParameters().length).toBe(1);
    expect(service.filteredParameters()[0].key).toBe('inventory.deduction_mode');

    // Búsqueda por caso de uso en help.example
    service.setSearchQuery('hamburguesa');
    expect(service.filteredParameters().length).toBe(1);
    expect(service.filteredParameters()[0].key).toBe('inventory.deduction_mode');

    // Búsqueda sin coincidencias
    service.setSearchQuery('inexistente_xyz');
    expect(service.filteredParameters().length).toBe(0);
  });
});
