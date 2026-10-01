import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach, afterEach } from 'vitest';
import { InventoryService } from './inventory.service';
import { StockBalanceDto, KardexMovementDto } from '../models/inventory.models';

describe('InventoryService', () => {
  let service: InventoryService;
  let httpMock: HttpTestingController;

  const mockStock: StockBalanceDto[] = [
    {
      id: 'stock-1',
      ingredientId: 'ing-1',
      ingredientName: 'Carne de Res molida',
      unitOfMeasure: 'KG',
      currentQuantity: 10,
      averageUnitCost: 28,
      totalValue: 280,
      minStockAlert: 5,
      isLowStock: false,
      lastUpdated: '2026-10-01T00:00:00Z',
    },
    {
      id: 'stock-2',
      ingredientId: 'ing-2',
      ingredientName: 'Pan de Hamburguesa',
      unitOfMeasure: 'UNIT',
      currentQuantity: 2,
      averageUnitCost: 1.5,
      totalValue: 3,
      minStockAlert: 10,
      isLowStock: true,
      lastUpdated: '2026-10-01T00:00:00Z',
    },
  ];

  const mockKardex: KardexMovementDto[] = [
    {
      id: 'mov-1',
      ingredientId: 'ing-1',
      ingredientName: 'Carne de Res molida',
      unitOfMeasure: 'KG',
      movementType: 'SALE_OUT',
      quantity: 0.3,
      unitCost: 28,
      totalCost: 8.4,
      balanceQuantity: 9.7,
      referenceId: 'order-1',
      movementDate: '2026-10-01T01:00:00Z',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        InventoryService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(InventoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe crearse correctamente', () => {
    expect(service).toBeTruthy();
  });

  it('debe cargar existencias de stock y actualizar computed signals', () => {
    service.loadStock('branch-1').subscribe((items) => {
      expect(items.length).toBe(2);
      expect(service.totalIngredientsCount()).toBe(2);
      expect(service.lowStockCount()).toBe(1);
      expect(service.totalInventoryValue()).toBe(283);
    });

    const req = httpMock.expectOne('/api/v1/inventory/stock?branchId=branch-1');
    expect(req.request.method).toBe('GET');
    req.flush(mockStock);
  });

  it('debe cargar movimientos de kardex en su signal', () => {
    service.loadKardex('branch-1').subscribe((movements) => {
      expect(movements.length).toBe(1);
      expect(service.kardexMovements()[0].movementType).toBe('SALE_OUT');
    });

    const req = httpMock.expectOne('/api/v1/inventory/kardex?branchId=branch-1');
    expect(req.request.method).toBe('GET');
    req.flush(mockKardex);
  });

  it('debe enviar ajuste de inventario y actualizar el ítem en la lista', () => {
    service.stockList.set(mockStock);

    const updatedItem: StockBalanceDto = {
      ...mockStock[0],
      currentQuantity: 15,
      totalValue: 420,
    };

    service
      .adjustStock(
        {
          ingredientId: 'ing-1',
          newQuantity: 15,
          unitCost: 28,
          reason: 'Conteo físico',
        },
        'branch-1'
      )
      .subscribe((res) => {
        expect(res.currentQuantity).toBe(15);
        expect(service.stockList()[0].currentQuantity).toBe(15);
        expect(service.successMessage()).toContain('Carne de Res molida');
      });

    const req = httpMock.expectOne('/api/v1/inventory/stock/adjust?branchId=branch-1');
    expect(req.request.method).toBe('POST');
    req.flush(updatedItem);
  });
});
