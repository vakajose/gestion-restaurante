import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';

export const ONBOARDING_TOUR_COMPLETED_KEY = 'onboarding_tour_completed';

export interface TourMilestone {
  index: number;
  stepNumber: number;
  title: string;
  subtitle: string;
  description: string;
  icon: string;
  route: string;
  actionLabel: string;
  highlightTip: string;
}

@Injectable({
  providedIn: 'root',
})
export class OnboardingTourService {
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  readonly milestones: TourMilestone[] = [
    {
      index: 0,
      stepNumber: 1,
      title: 'Hito 1: Apertura de Caja en POS',
      subtitle: 'Punto de Venta (/pos)',
      description:
        'Para comenzar tu jornada operativa, abre tu primer turno de caja ingresando el monto inicial en efectivo y selecciona uno de los platos disponibles en el terminal POS.',
      icon: '🔓',
      route: '/pos',
      actionLabel: 'Ir al Punto de Venta',
      highlightTip: 'Consejo: Toda venta queda asociada al turno de caja activo de tu sucursal.',
    },
    {
      index: 1,
      stepNumber: 2,
      title: 'Hito 2: Cobro de tu Primer Plato Demo',
      subtitle: 'Emisión de Comanda Rápida (/pos)',
      description:
        'Hemos precargado 3 platos de demostración listos para usar. Agrégalos al carrito, elige el método de pago (Efectivo, QR o Tarjeta) y registra tu primera venta en segundos.',
      icon: '💳',
      route: '/pos',
      actionLabel: 'Probar Cobro en POS',
      highlightTip: 'Consejo: El POS funciona incluso sin internet gracias a su motor Offline-First.',
    },
    {
      index: 2,
      stepNumber: 3,
      title: 'Hito 3: Validación Automática en Kardex',
      subtitle: 'Inventario e Insumos (/inventory)',
      description:
        'Cada vez que cobras un plato en el POS, el Kardex descuenta automáticamente las porciones exactas de cada ingrediente según su receta estándar en tiempo real.',
      icon: '📦',
      route: '/inventory',
      actionLabel: 'Ver Kardex e Inventario',
      highlightTip: 'Consejo: Revisa los movimientos de salida automática tras realizar tu venta de prueba.',
    },
    {
      index: 3,
      stepNumber: 4,
      title: 'Hito 4: Subida y Gestión de tu Catálogo',
      subtitle: 'Catálogo y Recetas (/catalog)',
      description:
        'Cuando estés listo para personalizar tu carta definitiva, administra tus categorías, platos y recetas o elimina los productos de prueba con un clic desde el Catálogo.',
      icon: '🍽️',
      route: '/catalog',
      actionLabel: 'Explorar Catálogo',
      highlightTip: 'Consejo: Puedes ajustar precios por sucursal y definir el costo de cada insumo.',
    },
  ];

  readonly isTourActive = signal<boolean>(false);
  readonly currentStepIndex = signal<number>(0);
  readonly isCompleted = signal<boolean>(false);

  readonly currentMilestone = computed<TourMilestone>(
    () => this.milestones[this.currentStepIndex()] ?? this.milestones[0]
  );

  readonly totalSteps = computed<number>(() => this.milestones.length);

  readonly progressPercentage = computed<number>(() =>
    Math.round(((this.currentStepIndex() + 1) / this.totalSteps()) * 100)
  );

  constructor() {
    this.restoreState();
  }

  /**
   * Inicia el tour guiado desde el primer hito (índice 0).
   */
  startTour(): void {
    this.currentStepIndex.set(0);
    this.isTourActive.set(true);
  }

  /**
   * Avanza al siguiente hito o completa el tour si está en el último paso.
   */
  nextStep(): void {
    const nextIdx = this.currentStepIndex() + 1;
    if (nextIdx < this.milestones.length) {
      this.currentStepIndex.set(nextIdx);
    } else {
      this.completeTour();
    }
  }

  /**
   * Retrocede al hito anterior si no está en el primero.
   */
  prevStep(): void {
    const prevIdx = this.currentStepIndex() - 1;
    if (prevIdx >= 0) {
      this.currentStepIndex.set(prevIdx);
    }
  }

  /**
   * Salta y cierra el tour marcándolo como completado para no volver a abrirse solo.
   */
  skipTour(): void {
    this.isTourActive.set(false);
    this.markCompleted(true);
  }

  /**
   * Finaliza satisfactoriamente el tour guiado y persiste su estado.
   */
  completeTour(): void {
    this.isTourActive.set(false);
    this.markCompleted(true);
  }

  /**
   * Reinicia el estado de completado y vuelve a lanzar el tour desde el paso 1.
   */
  resetAndRestartTour(): void {
    this.markCompleted(false);
    this.currentStepIndex.set(0);
    this.isTourActive.set(true);
  }

  /**
   * Navega a la ruta asociada al hito actual.
   */
  navigateToCurrentMilestoneRoute(): void {
    const milestone = this.currentMilestone();
    if (milestone?.route) {
      this.router.navigate([milestone.route]);
    }
  }

  private restoreState(): void {
    if (!this.isBrowser()) {
      return;
    }
    const saved = localStorage.getItem(ONBOARDING_TOUR_COMPLETED_KEY);
    this.isCompleted.set(saved === 'true');
  }

  private markCompleted(completed: boolean): void {
    this.isCompleted.set(completed);
    if (this.isBrowser()) {
      localStorage.setItem(ONBOARDING_TOUR_COMPLETED_KEY, completed ? 'true' : 'false');
    }
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId) && typeof window !== 'undefined';
  }
}
