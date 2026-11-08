import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthorizationQueueComponent } from './authorization-queue.component';
import { ShiftAuthorizationDto } from '../../../../core/models/finance.models';

describe('AuthorizationQueueComponent', () => {
  let component: AuthorizationQueueComponent;
  let fixture: ComponentFixture<AuthorizationQueueComponent>;

  const mockAuthorizations: ShiftAuthorizationDto[] = [
    {
      id: 'auth-1',
      tenantId: 't-1',
      branchId: 'b-1',
      requestType: 'ORDER_CANCELLATION',
      referenceId: 'ord-101',
      requestedBy: 'user-cajero',
      requestedByName: 'Juan Cajero',
      amount: 150.5,
      status: 'PENDING',
      justification: 'Cliente desistió por demora',
      createdAt: '2026-11-08T01:00:00Z',
    },
    {
      id: 'auth-2',
      tenantId: 't-1',
      branchId: 'b-1',
      requestType: 'EXPENSE',
      referenceId: 'exp-202',
      requestedBy: 'user-cajero',
      requestedByName: 'Juan Cajero',
      amount: 300.0,
      status: 'APPROVED',
      justification: 'Compra urgente de verduras',
      decidedBy: 'user-supervisor',
      decidedByName: 'Carlos Supervisor',
      decisionNotes: 'Aprobado vía consola',
      decisionChannel: 'REMOTE_SESSION',
      createdAt: '2026-11-08T00:30:00Z',
      decidedAt: '2026-11-08T00:35:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthorizationQueueComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AuthorizationQueueComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('authorizations', mockAuthorizations);
    fixture.componentRef.setInput('viewMode', 'SUPERVISOR');
    fixture.detectChanges();
  });

  it('debe renderizar el listado de autorizaciones correctamente', () => {
    expect(component).toBeTruthy();
    expect(component.filteredAuthorizations().length).toBe(2);

    const compiled = fixture.nativeElement as HTMLElement;
    const items = compiled.querySelectorAll('[data-testid="authorization-item"]');
    expect(items.length).toBe(2);
  });

  it('debe filtrar autorizaciones por tipo de solicitud', () => {
    component.filterType.set('ORDER_CANCELLATION');
    fixture.detectChanges();

    expect(component.filteredAuthorizations().length).toBe(1);
    expect(component.filteredAuthorizations()[0].requestType).toBe('ORDER_CANCELLATION');
  });

  it('debe filtrar autorizaciones por estado', () => {
    component.filterStatus.set('APPROVED');
    fixture.detectChanges();

    expect(component.filteredAuthorizations().length).toBe(1);
    expect(component.filteredAuthorizations()[0].status).toBe('APPROVED');
  });

  it('debe emitir approve con id y nota cuando el supervisor confirma aprobación', () => {
    let emitted: { id: string; notes?: string } | undefined;
    component.approve.subscribe((val) => (emitted = val));

    const compiled = fixture.nativeElement as HTMLElement;
    const approveBtn = compiled.querySelector('[data-testid="btn-approve-auth"]') as HTMLButtonElement;
    expect(approveBtn).toBeTruthy();
    approveBtn.click();

    expect(emitted).toBeDefined();
    expect(emitted?.id).toBe('auth-1');
  });

  it('debe solicitar motivo y emitir reject cuando el supervisor rechaza', () => {
    let emitted: { id: string; reason: string } | undefined;
    component.reject.subscribe((val) => (emitted = val));

    // Iniciar prompt de rechazo
    component.startRejectPrompt('auth-1');
    component.rejectReasonText = 'Monto excesivo sin justificar';
    component.confirmReject('auth-1');

    expect(emitted).toBeDefined();
    expect(emitted?.id).toBe('auth-1');
    expect(emitted?.reason).toBe('Monto excesivo sin justificar');
  });

  it('debe emitir requestPin en modo CASHIER cuando se solicita validar con PIN', () => {
    fixture.componentRef.setInput('viewMode', 'CASHIER');
    fixture.detectChanges();

    let emittedId: string | undefined;
    component.requestPin.subscribe((id) => (emittedId = id));

    const compiled = fixture.nativeElement as HTMLElement;
    const pinBtn = compiled.querySelector('[data-testid="btn-request-pin"]') as HTMLButtonElement;
    expect(pinBtn).toBeTruthy();
    pinBtn.click();

    expect(emittedId).toBe('auth-1');
  });

  it('debe mostrar estado vacío cuando no hay autorizaciones que coincidan', () => {
    component.filterStatus.set('REJECTED');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const emptyState = compiled.querySelector('[data-testid="empty-queue-state"]');
    expect(emptyState).toBeTruthy();
    expect(component.filteredAuthorizations().length).toBe(0);
  });
});
