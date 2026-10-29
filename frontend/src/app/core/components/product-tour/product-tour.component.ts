import { Component, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OnboardingTourService } from '../../services/onboarding-tour.service';

@Component({
  selector: 'app-product-tour',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (tourService.isTourActive()) {
      <!-- Backdrop semitransparente accesible -->
      <div
        data-testid="product-tour-backdrop"
        class="fixed inset-0 z-50 flex items-center justify-center bg-dark/60 backdrop-blur-xs p-4 transition-opacity duration-200"
        role="dialog"
        aria-modal="true"
        aria-labelledby="product-tour-title"
      >
        <!-- Tarjeta del Tour Guiado TailwindAdmin v2.0 -->
        <div
          data-testid="product-tour-modal"
          class="card max-w-lg w-full bg-white dark:bg-darkcard border border-border dark:border-darkborder rounded-md shadow-xl p-6 sm:p-8 relative animate-in fade-in zoom-in-95 duration-150"
        >
          <!-- Encabezado con Indicador de Paso y Botón Saltar -->
          <div class="flex items-center justify-between mb-4">
            <span
              data-testid="tour-step-indicator"
              class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-lightprimary text-primary"
            >
              🎓 Paso {{ tourService.currentStepIndex() + 1 }} de {{ tourService.totalSteps() }}
            </span>

            <button
              type="button"
              data-testid="tour-skip-btn"
              (click)="tourService.skipTour()"
              class="text-xs font-medium text-bodytext dark:text-darklink hover:text-error transition-colors px-2 py-1 rounded"
            >
              Saltar tour ✕
            </button>
          </div>

          <!-- Barra de Progreso -->
          <div class="w-full h-1.5 bg-lightgray dark:bg-dark rounded-full overflow-hidden mb-6">
            <div
              data-testid="tour-progress-bar"
              class="h-full bg-primary transition-all duration-300"
              [style.width.%]="tourService.progressPercentage()"
            ></div>
          </div>

          <!-- Contenido del Hito Actual -->
          @if (tourService.currentMilestone(); as milestone) {
            <div class="space-y-4">
              <div class="flex items-start gap-4">
                <div
                  class="w-12 h-12 rounded-md bg-lightprimary dark:bg-primary/20 flex items-center justify-center text-2xl shrink-0 shadow-xs"
                  aria-hidden="true"
                >
                  {{ milestone.icon }}
                </div>
                <div>
                  <span class="text-xs font-semibold uppercase tracking-wider text-secondary">
                    {{ milestone.subtitle }}
                  </span>
                  <h2
                    id="product-tour-title"
                    data-testid="tour-milestone-title"
                    class="text-lg sm:text-xl font-bold text-link dark:text-white mt-0.5"
                  >
                    {{ milestone.title }}
                  </h2>
                </div>
              </div>

              <p
                data-testid="tour-milestone-description"
                class="text-sm text-bodytext dark:text-darklink leading-relaxed"
              >
                {{ milestone.description }}
              </p>

              <!-- Tip destacado -->
              <div class="p-3 rounded-md bg-lightsecondary/40 dark:bg-dark border border-secondary/30 text-xs text-link dark:text-darklink flex items-center justify-between gap-3">
                <span>💡 {{ milestone.highlightTip }}</span>
                <button
                  type="button"
                  data-testid="tour-go-route-btn"
                  (click)="tourService.navigateToCurrentMilestoneRoute()"
                  class="shrink-0 text-primary hover:underline font-semibold"
                >
                  {{ milestone.actionLabel }} →
                </button>
              </div>
            </div>
          }

          <!-- Indicadores de puntos (Dots) -->
          <div class="flex items-center justify-center gap-2 my-6">
            @for (m of tourService.milestones; track m.index) {
              <button
                type="button"
                [attr.data-testid]="'tour-dot-' + m.index"
                (click)="goToStep(m.index)"
                class="h-2 rounded-full transition-all"
                [class.w-6]="tourService.currentStepIndex() === m.index"
                [class.bg-primary]="tourService.currentStepIndex() === m.index"
                [class.w-2]="tourService.currentStepIndex() !== m.index"
                [class.bg-border]="tourService.currentStepIndex() !== m.index"
                [attr.aria-label]="'Ir al paso ' + m.stepNumber"
              ></button>
            }
          </div>

          <!-- Botones de Control: Atrás / Siguiente / Finalizar -->
          <div class="flex items-center justify-between pt-4 border-t border-border dark:border-darkborder">
            <button
              type="button"
              data-testid="tour-prev-btn"
              (click)="tourService.prevStep()"
              [disabled]="tourService.currentStepIndex() === 0"
              class="px-4 py-2 rounded-md border border-border dark:border-darkborder text-xs sm:text-sm font-semibold text-link dark:text-white hover:bg-lightgray dark:hover:bg-dark disabled:opacity-40 disabled:pointer-events-none transition-colors"
            >
              ← Atrás
            </button>

            <div class="flex items-center gap-2">
              @if (tourService.currentStepIndex() < tourService.totalSteps() - 1) {
                <button
                  type="button"
                  data-testid="tour-next-btn"
                  (click)="tourService.nextStep()"
                  class="btn px-5 py-2 text-xs sm:text-sm font-semibold shadow-xs"
                >
                  Siguiente →
                </button>
              } @else {
                <button
                  type="button"
                  data-testid="tour-finish-btn"
                  (click)="tourService.completeTour()"
                  class="btn bg-success hover:bg-success/90 text-white px-5 py-2 text-xs sm:text-sm font-semibold shadow-xs"
                >
                  ¡Entendido, Empezar! ✓
                </button>
              }
            </div>
          </div>
        </div>
      </div>
    }
  `,
})
export class ProductTourComponent {
  readonly tourService = inject(OnboardingTourService);

  @HostListener('window:keydown.escape')
  onEscapeKey(): void {
    if (this.tourService.isTourActive()) {
      this.tourService.skipTour();
    }
  }

  goToStep(index: number): void {
    if (index >= 0 && index < this.tourService.totalSteps()) {
      this.tourService.currentStepIndex.set(index);
    }
  }
}
