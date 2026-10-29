import { Component, inject, signal, computed, OnInit, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, filter, switchMap, catchError, of } from 'rxjs';
import { OnboardingService } from '../../core/services/onboarding.service';
import { AuthService } from '../../core/services/auth.service';
import { BranchContextService } from '../../core/services/branch-context.service';
import { OnboardingTourService } from '../../core/services/onboarding-tour.service';
import { ThemeService } from '../../core/services/theme.service';
import {
  BusinessTemplate,
  CheckAvailabilityResultDto,
  OnboardingRequest,
} from '../../core/models/company.models';
import { ProblemDetail } from '../../core/models/auth.models';

export interface BolivianCityPreset {
  name: string;
  latitude: number;
  longitude: number;
}

export interface BusinessTemplateOption {
  id: BusinessTemplate;
  title: string;
  subtitle: string;
  icon: string;
  sampleDishes: string[];
}

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './onboarding.component.html',
})
export class OnboardingComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly onboardingService = inject(OnboardingService);
  private readonly authService = inject(AuthService);
  private readonly branchContextService = inject(BranchContextService);
  private readonly tourService = inject(OnboardingTourService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly themeService = inject(ThemeService);

  readonly currentStep = signal<1 | 2 | 3>(1);
  readonly slugManuallyEdited = signal<boolean>(false);
  readonly isCheckingSlug = signal<boolean>(false);
  readonly slugAvailable = signal<boolean | null>(null);
  readonly suggestedSlug = signal<string | null>(null);
  readonly showSlugTooltip = signal<boolean>(false);
  readonly showPassword = signal<boolean>(false);

  readonly selectedTemplate = signal<BusinessTemplate>('FAST_FOOD');
  readonly includeDemoCatalog = signal<boolean>(true);
  readonly isSummaryModalOpen = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  readonly bolivianCities: BolivianCityPreset[] = [
    { name: 'La Paz', latitude: -16.5, longitude: -68.15 },
    { name: 'Santa Cruz', latitude: -17.7833, longitude: -63.1821 },
    { name: 'Cochabamba', latitude: -17.3895, longitude: -66.1568 },
    { name: 'El Alto', latitude: -16.5047, longitude: -68.1633 },
    { name: 'Tarija', latitude: -21.5355, longitude: -64.7296 },
    { name: 'Sucre', latitude: -19.0333, longitude: -65.2627 },
  ];

  readonly templateOptions: BusinessTemplateOption[] = [
    {
      id: 'FAST_FOOD',
      title: 'Comida Rápida / Pizzería',
      subtitle: 'Flujo ágil en mostrador, emisión rápida de comanda y control por porción.',
      icon: '🍕',
      sampleDishes: ['Pizza Margarita', 'Hamburguesa Clásica', 'Gaseosa 500ml'],
    },
    {
      id: 'TRADITIONAL',
      title: 'Restaurante a la Carta',
      subtitle: 'Atención tradicional en salón, platos fuertes y recetas detalladas.',
      icon: '🍽️',
      sampleDishes: ['Pique Macho', 'Silpancho Tradicional', 'Jarra de Limonada'],
    },
    {
      id: 'CAFE',
      title: 'Cafetería / Bar',
      subtitle: 'Cobro rápido en barra, bebidas calientes/frías, pastelería y snacks.',
      icon: '☕',
      sampleDishes: ['Cappuccino Italiano', 'Croissant de Jamón y Queso', 'Jugo Natural'],
    },
    {
      id: 'EMPTY',
      title: 'En Blanco / Personalizado',
      subtitle: 'Sin productos de ejemplo. Ideal si deseas subir tu propio menú desde cero.',
      icon: '⚙️',
      sampleDishes: [],
    },
  ];

  readonly step1Form = this.fb.group({
    legalName: ['', [Validators.required, Validators.minLength(2)]],
    slug: ['', [Validators.required, Validators.minLength(2), Validators.pattern(/^[a-z0-9-]+$/)]],
    nitOrTaxId: [''],
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    username: ['admin', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  readonly step2Form = this.fb.group({
    name: ['Sucursal Principal', [Validators.required, Validators.minLength(2)]],
    address: [''],
    phone: [''],
    latitude: [-16.5 as number | null],
    longitude: [-68.15 as number | null],
    timezone: ['America/La_Paz', [Validators.required]],
    currency: ['BOB', [Validators.required]],
  });

  readonly selectedTemplateInfo = computed<BusinessTemplateOption>(
    () =>
      this.templateOptions.find((t) => t.id === this.selectedTemplate()) ??
      this.templateOptions[0]
  );

  ngOnInit(): void {
    // Inferencia reactiva de slug a partir de legalName cuando no ha sido editado manualmente
    this.step1Form.controls.legalName.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((legalNameVal) => {
        if (!this.slugManuallyEdited()) {
          const generated = this.slugify(legalNameVal ?? '');
          this.step1Form.controls.slug.setValue(generated, { emitEvent: true });
        }
      });

    // Validación asíncrona con debounce de disponibilidad de slug
    this.step1Form.controls.slug.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        filter((val): val is string => !!val && val.trim().length >= 2),
        switchMap((slugVal) => {
          this.isCheckingSlug.set(true);
          return this.onboardingService.checkSlugAvailability(slugVal.trim()).pipe(
            catchError(() =>
              of<CheckAvailabilityResultDto>({
                available: true,
                suggestedSlug: null,
                suggestedAlternative: null,
              })
            )
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((result) => {
        this.isCheckingSlug.set(false);
        const isAvail = result.slugAvailable ?? result.available;
        this.slugAvailable.set(isAvail);
        this.suggestedSlug.set(
          !isAvail ? (result.suggestedSlug || result.suggestedAlternative || null) : null
        );
      });
  }

  /**
   * Transforma un nombre comercial en un identificador slug limpio (minúsculas, sin acentos, separado por guiones).
   */
  slugify(text: string): string {
    return text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9\s-]/g, '')
      .trim()
      .replace(/\s+/g, '-')
      .replace(/-+/g, '-');
  }

  onSlugInput(): void {
    this.slugManuallyEdited.set(true);
    const current = this.step1Form.controls.slug.value ?? '';
    const sanitized = this.slugify(current);
    if (sanitized !== current) {
      this.step1Form.controls.slug.setValue(sanitized);
    }
    this.triggerImmediateSlugCheck(sanitized);
  }

  triggerImmediateSlugCheck(slugVal: string): void {
    const clean = slugVal.trim();
    if (clean.length < 2) {
      this.slugAvailable.set(null);
      this.suggestedSlug.set(null);
      return;
    }
    this.isCheckingSlug.set(true);
    this.onboardingService
      .checkSlugAvailability(clean)
      .pipe(
        catchError(() =>
          of<CheckAvailabilityResultDto>({
            available: true,
            suggestedSlug: null,
            suggestedAlternative: null,
          })
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((res) => {
        this.isCheckingSlug.set(false);
        const isAvail = res.slugAvailable ?? res.available;
        this.slugAvailable.set(isAvail);
        this.suggestedSlug.set(
          !isAvail ? (res.suggestedSlug || res.suggestedAlternative || null) : null
        );
      });
  }

  applySuggestedSlug(): void {
    const suggested = this.suggestedSlug();
    if (suggested) {
      this.slugManuallyEdited.set(true);
      this.step1Form.controls.slug.setValue(suggested);
      this.slugAvailable.set(true);
      this.suggestedSlug.set(null);
    }
  }

  toggleSlugTooltip(): void {
    this.showSlugTooltip.update((v) => !v);
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((v) => !v);
  }

  selectCityPreset(city: BolivianCityPreset): void {
    this.step2Form.patchValue({
      latitude: city.latitude,
      longitude: city.longitude,
    });
  }

  selectTemplate(templateId: BusinessTemplate): void {
    this.selectedTemplate.set(templateId);
    if (templateId === 'EMPTY') {
      this.includeDemoCatalog.set(false);
    } else {
      this.includeDemoCatalog.set(true);
    }
  }

  toggleIncludeDemoCatalog(): void {
    if (this.selectedTemplate() === 'EMPTY') {
      return;
    }
    this.includeDemoCatalog.update((v) => !v);
  }

  goToStep(step: 1 | 2 | 3): void {
    if (step === 2 && this.currentStep() === 1) {
      if (this.step1Form.invalid || this.slugAvailable() === false) {
        this.step1Form.markAllAsTouched();
        return;
      }
    }
    if (step === 3 && this.currentStep() === 2) {
      if (this.step2Form.invalid) {
        this.step2Form.markAllAsTouched();
        return;
      }
    }
    this.errorMessage.set(null);
    this.currentStep.set(step);
  }

  nextStep(): void {
    const current = this.currentStep();
    if (current === 1) {
      this.goToStep(2);
    } else if (current === 2) {
      this.goToStep(3);
    }
  }

  prevStep(): void {
    const current = this.currentStep();
    if (current === 2) {
      this.currentStep.set(1);
    } else if (current === 3) {
      this.currentStep.set(2);
    }
  }

  openSummaryModal(): void {
    if (this.step1Form.invalid) {
      this.currentStep.set(1);
      this.step1Form.markAllAsTouched();
      return;
    }
    if (this.step2Form.invalid) {
      this.currentStep.set(2);
      this.step2Form.markAllAsTouched();
      return;
    }
    this.isSummaryModalOpen.set(true);
  }

  closeSummaryModal(): void {
    this.isSummaryModalOpen.set(false);
  }

  confirmAndSubmitOnboarding(): void {
    if (this.isSubmitting()) {
      return;
    }
    if (this.step1Form.invalid || this.step2Form.invalid) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const s1 = this.step1Form.getRawValue();
    const s2 = this.step2Form.getRawValue();

    const payload: OnboardingRequest = {
      company: {
        legalName: (s1.legalName ?? '').trim(),
        slug: (s1.slug ?? '').trim(),
        nitOrTaxId: s1.nitOrTaxId && s1.nitOrTaxId.trim().length > 0 ? s1.nitOrTaxId.trim() : null,
      },
      branch: {
        name: (s2.name ?? 'Sucursal Principal').trim(),
        address: s2.address && s2.address.trim().length > 0 ? s2.address.trim() : null,
        phone: s2.phone && s2.phone.trim().length > 0 ? s2.phone.trim() : null,
        latitude: s2.latitude ?? null,
        longitude: s2.longitude ?? null,
        timezone: s2.timezone || 'America/La_Paz',
        currency: s2.currency || 'BOB',
      },
      businessTemplate: this.selectedTemplate(),
      includeDemoCatalog: this.selectedTemplate() === 'EMPTY' ? false : this.includeDemoCatalog(),
      adminUser: {
        fullName: (s1.fullName ?? '').trim(),
        username: (s1.username ?? '').trim(),
        email: s1.email && s1.email.trim().length > 0 ? s1.email.trim() : null,
        password: s1.password ?? '',
      },
    };

    this.onboardingService.registerOnboarding(payload).subscribe({
      next: (result) => {
        this.authService.establishSessionFromOnboarding(result);
        this.branchContextService.loadBranches(result.branchId).subscribe({
          next: () => {
            this.isSubmitting.set(false);
            this.isSummaryModalOpen.set(false);
            this.tourService.startTour();
            this.router.navigate(['/pos']);
          },
          error: () => {
            this.isSubmitting.set(false);
            this.isSummaryModalOpen.set(false);
            this.tourService.startTour();
            this.router.navigate(['/pos']);
          },
        });
      },
      error: (err: unknown) => {
        this.isSubmitting.set(false);
        this.isSummaryModalOpen.set(false);
        if (err instanceof HttpErrorResponse) {
          if (err.error && typeof err.error === 'object') {
            const problem = err.error as ProblemDetail;
            this.errorMessage.set(
              problem.detail ||
                problem.title ||
                'No se pudo completar el registro de la empresa. Verifique los datos.'
            );
          } else {
            this.errorMessage.set('Error en el servidor al procesar el alta de la empresa.');
          }
        } else {
          this.errorMessage.set('Ocurrió un error inesperado al crear la empresa.');
        }
      },
    });
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  clearError(): void {
    this.errorMessage.set(null);
  }
}
