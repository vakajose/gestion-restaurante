import { TestBed } from '@angular/core/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [NotificationService],
    });
    service = TestBed.inject(NotificationService);
  });

  afterEach(() => {
    service.clearAll();
  });

  it('should create and enqueue success, error, warning, and info toasts', () => {
    const id1 = service.success('Plato guardado correctamente');
    const id2 = service.error('Debe abrir un turno de caja', {
      title: 'Turno requerido',
      errorCode: 'SHIFT_NOT_OPEN',
    });

    expect(service.toasts().length).toBe(2);
    expect(service.toasts()[0].id).toBe(id2);
    expect(service.toasts()[0].type).toBe('error');
    expect(service.toasts()[0].errorCode).toBe('SHIFT_NOT_OPEN');
    expect(service.toasts()[1].id).toBe(id1);
    expect(service.toasts()[1].type).toBe('success');
  });

  it('should deduplicate identical active toasts', () => {
    const firstId = service.error('Error duplicado', { title: 'Conflicto' });
    const secondId = service.error('Error duplicado', { title: 'Conflicto' });

    expect(firstId).toBe(secondId);
    expect(service.toasts().length).toBe(1);
  });

  it('should dismiss a specific toast and clear all', () => {
    const id = service.warning('Stock bajo mínimo');
    expect(service.toasts().length).toBe(1);

    service.dismiss(id);
    expect(service.toasts().length).toBe(0);

    service.info('Mensaje 1');
    service.info('Mensaje 2');
    expect(service.toasts().length).toBe(2);

    service.clearAll();
    expect(service.toasts().length).toBe(0);
  });
});
