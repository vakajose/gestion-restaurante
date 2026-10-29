import { Injectable, signal, inject, PLATFORM_ID, OnDestroy } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AuthService } from './auth.service';

export const DEFAULT_IDLE_TIMEOUT_MINUTES = 30;
export const WARNING_COUNTDOWN_SECONDS = 120;

@Injectable({
  providedIn: 'root',
})
export class IdleTimerService implements OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly isWarningModalOpen = signal<boolean>(false);
  readonly remainingSeconds = signal<number>(WARNING_COUNTDOWN_SECONDS);
  readonly timeoutMinutes = signal<number>(DEFAULT_IDLE_TIMEOUT_MINUTES);
  readonly isWatching = signal<boolean>(false);

  private warningTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private countdownIntervalId: ReturnType<typeof setInterval> | null = null;
  private lastActivityTimestamp = 0;

  private readonly activityEvents = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'];
  private readonly boundActivityHandler = (): void => this.onUserInteraction();

  /**
   * Configura el tiempo máximo de inactividad en minutos (mínimo 3 minutos).
   */
  setTimeoutMinutes(minutes: number): void {
    const safeMinutes = Math.max(3, minutes);
    this.timeoutMinutes.set(safeMinutes);
    if (this.isWatching()) {
      this.scheduleIdleWarning();
    }
  }

  /**
   * Inicia el monitoreo de eventos de actividad del usuario en el navegador.
   */
  startWatching(): void {
    if (!this.isBrowser()) {
      return;
    }
    this.stopWatching();
    this.isWatching.set(true);
    this.lastActivityTimestamp = Date.now();

    for (const evt of this.activityEvents) {
      window.addEventListener(evt, this.boundActivityHandler, { passive: true });
    }
    this.scheduleIdleWarning();
  }

  /**
   * Detiene todos los temporizadores y remueve los listeners de actividad.
   */
  stopWatching(): void {
    this.isWatching.set(false);
    this.isWarningModalOpen.set(false);
    this.clearTimers();

    if (this.isBrowser()) {
      for (const evt of this.activityEvents) {
        window.removeEventListener(evt, this.boundActivityHandler);
      }
    }
  }

  /**
   * Registra actividad explícita y reinicia el contador si el modal de advertencia no está abierto.
   */
  recordActivity(): void {
    if (this.isWarningModalOpen()) {
      return;
    }
    this.lastActivityTimestamp = Date.now();
    if (this.isWatching()) {
      this.scheduleIdleWarning();
    }
  }

  /**
   * Cierra el modal de advertencia preventiva y reinicia el ciclo de inactividad ("Continuar trabajando").
   */
  continueWorking(): void {
    this.isWarningModalOpen.set(false);
    this.remainingSeconds.set(WARNING_COUNTDOWN_SECONDS);
    this.clearTimers();
    this.lastActivityTimestamp = Date.now();
    if (this.isWatching()) {
      this.scheduleIdleWarning();
    }
  }

  /**
   * Abre inmediatamente el modal de advertencia e inicia la cuenta regresiva de 120 segundos.
   */
  triggerWarning(): void {
    this.clearTimers();
    this.remainingSeconds.set(WARNING_COUNTDOWN_SECONDS);
    this.isWarningModalOpen.set(true);

    this.countdownIntervalId = setInterval(() => {
      const nextVal = this.remainingSeconds() - 1;
      if (nextVal <= 0) {
        this.remainingSeconds.set(0);
        this.handleSessionExpiredByInactivity();
      } else {
        this.remainingSeconds.set(nextVal);
      }
    }, 1000);
  }

  /**
   * Ejecuta el cierre de sesión automático cuando el contador de inactividad llega a cero.
   */
  handleSessionExpiredByInactivity(): void {
    this.stopWatching();
    this.authService.logout(true);
  }

  ngOnDestroy(): void {
    this.stopWatching();
  }

  private onUserInteraction(): void {
    // Throttle ligero (1 segundo) para evitar reprogramar en cada pixel de mousemove
    const now = Date.now();
    if (now - this.lastActivityTimestamp < 1000) {
      return;
    }
    this.recordActivity();
  }

  private scheduleIdleWarning(): void {
    this.clearTimers();
    const totalSeconds = this.timeoutMinutes() * 60;
    const warningDelaySeconds = Math.max(5, totalSeconds - WARNING_COUNTDOWN_SECONDS);

    this.warningTimeoutId = setTimeout(() => {
      if (this.authService.isAuthenticated()) {
        this.triggerWarning();
      }
    }, warningDelaySeconds * 1000);
  }

  private clearTimers(): void {
    if (this.warningTimeoutId !== null) {
      clearTimeout(this.warningTimeoutId);
      this.warningTimeoutId = null;
    }
    if (this.countdownIntervalId !== null) {
      clearInterval(this.countdownIntervalId);
      this.countdownIntervalId = null;
    }
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId) && typeof window !== 'undefined';
  }
}
