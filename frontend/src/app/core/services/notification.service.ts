import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastItem {
  id: string;
  type: ToastType;
  title: string;
  message: string;
  errorCode?: string;
  invalidParams?: Record<string, string>;
  durationMs: number;
  createdAt: number;
}

export interface ToastOptions {
  title?: string;
  errorCode?: string;
  invalidParams?: Record<string, string>;
  durationMs?: number;
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private readonly maxToasts = 5;
  private readonly timers = new Map<string, ReturnType<typeof setTimeout>>();

  readonly toasts = signal<ToastItem[]>([]);

  success(message: string, options?: ToastOptions): string {
    return this.show('success', message, {
      title: options?.title ?? 'Operación exitosa',
      durationMs: options?.durationMs ?? 4500,
      ...options,
    });
  }

  error(message: string, options?: ToastOptions): string {
    return this.show('error', message, {
      title: options?.title ?? 'No se pudo completar la acción',
      durationMs: options?.durationMs ?? 7500,
      ...options,
    });
  }

  warning(message: string, options?: ToastOptions): string {
    return this.show('warning', message, {
      title: options?.title ?? 'Atención requerida',
      durationMs: options?.durationMs ?? 6000,
      ...options,
    });
  }

  info(message: string, options?: ToastOptions): string {
    return this.show('info', message, {
      title: options?.title ?? 'Información',
      durationMs: options?.durationMs ?? 5000,
      ...options,
    });
  }

  show(type: ToastType, message: string, options?: ToastOptions): string {
    const cleanMsg = message?.trim() || 'Ocurrió un evento en el sistema.';
    const title = options?.title?.trim() || 'Notificación';
    const durationMs = options?.durationMs ?? (type === 'error' ? 7500 : 4500);

    // Deduplicación: si ya existe un toast idéntico activo en los últimos 1.5s, reiniciamos su timer
    const existing = this.toasts().find(
      (t) => t.type === type && t.message === cleanMsg && t.title === title
    );
    if (existing) {
      this.scheduleDismiss(existing.id, durationMs);
      return existing.id;
    }

    const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
    const item: ToastItem = {
      id,
      type,
      title,
      message: cleanMsg,
      errorCode: options?.errorCode,
      invalidParams: options?.invalidParams,
      durationMs,
      createdAt: Date.now(),
    };

    this.toasts.update((list) => [item, ...list].slice(0, this.maxToasts));

    if (durationMs > 0) {
      this.scheduleDismiss(id, durationMs);
    }

    return id;
  }

  dismiss(id: string): void {
    const timer = this.timers.get(id);
    if (timer) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  clearAll(): void {
    for (const timer of this.timers.values()) {
      clearTimeout(timer);
    }
    this.timers.clear();
    this.toasts.set([]);
  }

  private scheduleDismiss(id: string, durationMs: number): void {
    const prev = this.timers.get(id);
    if (prev) {
      clearTimeout(prev);
    }
    const timer = setTimeout(() => {
      this.timers.delete(id);
      this.toasts.update((list) => list.filter((t) => t.id !== id));
    }, durationMs);
    this.timers.set(id, timer);
  }
}
