import { TestBed } from '@angular/core/testing';
import {
  IdleTimerService,
  WARNING_COUNTDOWN_SECONDS,
} from './idle-timer.service';
import { AuthService } from './auth.service';

describe('IdleTimerService', () => {
  let service: IdleTimerService;
  let authServiceSpy: {
    isAuthenticated: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    vi.useFakeTimers();

    authServiceSpy = {
      isAuthenticated: vi.fn().mockReturnValue(true),
      logout: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        IdleTimerService,
        { provide: AuthService, useValue: authServiceSpy },
      ],
    });

    service = TestBed.inject(IdleTimerService);
  });

  afterEach(() => {
    service.stopWatching();
    vi.useRealTimers();
  });

  it('debe inicializarse con el modal de advertencia cerrado y timeout de 30 minutos', () => {
    expect(service.isWarningModalOpen()).toBe(false);
    expect(service.timeoutMinutes()).toBe(30);
    expect(service.remainingSeconds()).toBe(WARNING_COUNTDOWN_SECONDS);
  });

  it('debe abrir el modal de advertencia al cumplirse el tiempo previo y descontar cada segundo', () => {
    service.setTimeoutMinutes(3); // 180s total -> advertencia a los 60s
    service.startWatching();

    vi.advanceTimersByTime(60 * 1000);
    expect(service.isWarningModalOpen()).toBe(true);
    expect(service.remainingSeconds()).toBe(120);

    vi.advanceTimersByTime(10 * 1000);
    expect(service.remainingSeconds()).toBe(110);
  });

  it('debe cerrar el modal y reiniciar la cuenta regresiva al llamar a continueWorking()', () => {
    service.triggerWarning();
    expect(service.isWarningModalOpen()).toBe(true);

    vi.advanceTimersByTime(15 * 1000);
    expect(service.remainingSeconds()).toBe(105);

    service.continueWorking();
    expect(service.isWarningModalOpen()).toBe(false);
    expect(service.remainingSeconds()).toBe(120);
  });

  it('debe cerrar la sesión automáticamente cuando la cuenta regresiva llega a 0 segundos', () => {
    service.triggerWarning();
    expect(service.isWarningModalOpen()).toBe(true);

    vi.advanceTimersByTime(120 * 1000);

    expect(service.isWarningModalOpen()).toBe(false);
    expect(authServiceSpy.logout).toHaveBeenCalledWith(true);
  });
});
