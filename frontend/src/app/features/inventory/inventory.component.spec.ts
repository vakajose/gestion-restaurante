import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, expect, it, beforeEach } from 'vitest';
import { of } from 'rxjs';
import { InventoryComponent } from './inventory.component';
import { InventoryService } from '../../core/services/inventory.service';
import { AuthService } from '../../core/services/auth.service';
import { StockBalanceDto, KardexMovementDto } from '../../core/models/inventory.models';

describe('InventoryComponent', () => {
  let component: InventoryComponent;
  let fixture: ComponentFixture<InventoryComponent>;
  let inventoryService: InventoryService;

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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InventoryComponent],
      providers: [
        InventoryService,
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    inventoryService = TestBed.inject(InventoryService);
    // Mock HTTP loaders
    inventoryService.loadStock = () => {
      inventoryService.stockList.set(mockStock);
      return of(mockStock);
    };
    inventoryService.loadKardex = () => {
      inventoryService.kardexMovements.set(mockKardex);
      return of(mockKardex);
    };

    fixture = TestBed.createComponent(InventoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse e inicializar datos de stock y kardex', () => {
    expect(component).toBeTruthy();
    expect(component.filteredStock().length).toBe(2);
    expect(component.filteredKardex().length).toBe(1);
  });

  it('debe filtrar insumos por término de búsqueda', () => {
    component.searchQuery.set('Carne');
    expect(component.filteredStock().length).toBe(1);
    expect(component.filteredStock()[0].ingredientName).toContain('Carne');
  });

  it('debe filtrar insumos mostrando solo los de stock bajo', () => {
    component.showOnlyLowStock.set(true);
    expect(component.filteredStock().length).toBe(1);
    expect(component.filteredStock()[0].ingredientName).toContain('Pan');
  });

  it('debe alternar de pestaña a Kardex y mostrar los movimientos', () => {
    component.setTab('kardex');
    expect(component.activeTab()).toBe('kardex');
    expect(component.filteredKardex()[0].movementType).toBe('SALE_OUT');
  });

  it('debe abrir y cerrar modal de ajuste de stock', () => {
    component.openAdjustModal(mockStock[0]);
    expect(component.showAdjustModal()).toBe(true);
    expect(component.adjustingItem()?.ingredientId).toBe('ing-1');

    component.closeAdjustModal();
    expect(component.showAdjustModal()).toBe(false);
  });
});
