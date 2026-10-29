import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { OnboardingComponent } from './onboarding.component';
import { OnboardingService } from '../../core/services/onboarding.service';
import { AuthService } from '../../core/services/auth.service';
import { BranchContextService } from '../../core/services/branch-context.service';
import { OnboardingTourService } from '../../core/services/onboarding-tour.service';
import { ThemeService } from '../../core/services/theme.service';
import { OnboardingResultDto } from '../../core/models/company.models';

describe('OnboardingComponent', () => {
  let component: OnboardingComponent;
  let fixture: ComponentFixture<OnboardingComponent>;
  let onboardingServiceSpy: {
    checkSlugAvailability: ReturnType<typeof vi.fn>;
    checkAvailability: ReturnType<typeof vi.fn>;
    registerOnboarding: ReturnType<typeof vi.fn>;
  };
  let authServiceSpy: {
    establishSessionFromOnboarding: ReturnType<typeof vi.fn>;
  };
  let branchContextSpy: {
    loadBranches: ReturnType<typeof vi.fn>;
  };
  let tourService: OnboardingTourService;
  let router: Router;

  const mockOnboardingResult: OnboardingResultDto = {
    tenantId: 'tenant-new-1',
    branchId: 'branch-new-1',
    adminUserId: 'admin-new-1',
    slug: 'bella-napoli',
    accessToken: 'jwt-new-token',
    token: 'jwt-new-token',
    user: {
      id: 'admin-new-1',
      username: 'admin',
      email: 'carlos@napoli.com',
      role: 'ADMIN_TENANT',
      tenantId: 'tenant-new-1',
      branchId: 'branch-new-1',
    },
  };

  beforeEach(async () => {
    localStorage.clear();

    onboardingServiceSpy = {
      checkSlugAvailability: vi.fn().mockReturnValue(of({ available: true })),
      checkAvailability: vi.fn().mockReturnValue(of({ available: true })),
      registerOnboarding: vi.fn().mockReturnValue(of(mockOnboardingResult)),
    };

    authServiceSpy = {
      establishSessionFromOnboarding: vi.fn(),
    };

    branchContextSpy = {
      loadBranches: vi.fn().mockReturnValue(of([])),
    };

    await TestBed.configureTestingModule({
      imports: [OnboardingComponent],
      providers: [
        provideRouter([]),
        ThemeService,
        OnboardingTourService,
        { provide: OnboardingService, useValue: onboardingServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: BranchContextService, useValue: branchContextSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OnboardingComponent);
    component = fixture.componentInstance;
    tourService = TestBed.inject(OnboardingTourService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('debe inicializarse en el Paso 1 con valores predeterminados', () => {
    expect(component).toBeTruthy();
    expect(component.currentStep()).toBe(1);
    expect(component.step2Form.controls.name.value).toBe('Sucursal Principal');
    expect(component.step2Form.controls.timezone.value).toBe('America/La_Paz');
    expect(component.step2Form.controls.currency.value).toBe('BOB');
    expect(component.selectedTemplate()).toBe('FAST_FOOD');
    expect(component.includeDemoCatalog()).toBe(true);
  });

  it('debe inferir automáticamente el slug a partir de legalName sin requerir NIT', () => {
    component.step1Form.controls.legalName.setValue('Pizzería Bella Nápoli');
    expect(component.step1Form.controls.slug.value).toBe('pizzeria-bella-napoli');

    // Completar el resto del paso 1 dejando NIT vacío
    component.step1Form.controls.fullName.setValue('Carlos Mendoza');
    component.step1Form.controls.username.setValue('carlos');
    component.step1Form.controls.password.setValue('secret123');

    expect(component.step1Form.valid).toBe(true);
  });

  it('debe permitir aplicar en un clic el suggestedSlug cuando el slug está ocupado', () => {
    component.slugAvailable.set(false);
    component.suggestedSlug.set('bella-napoli-2');
    fixture.detectChanges();

    const applyBtn = fixture.nativeElement.querySelector(
      '[data-testid="apply-suggested-slug-btn"]'
    ) as HTMLButtonElement;
    expect(applyBtn).toBeTruthy();

    applyBtn.click();
    fixture.detectChanges();

    expect(component.step1Form.controls.slug.value).toBe('bella-napoli-2');
    expect(component.slugAvailable()).toBe(true);
    expect(component.suggestedSlug()).toBeNull();
  });

  it('debe avanzar del Paso 1 al Paso 2 y al Paso 3, seleccionar plantilla y confirmar el alta', () => {
    // Paso 1
    component.step1Form.setValue({
      legalName: 'Pizzería Bella Napoli',
      slug: 'bella-napoli',
      nitOrTaxId: '',
      fullName: 'Carlos Mendoza',
      username: 'carlos',
      email: 'carlos@napoli.com',
      password: 'password123',
    });
    component.slugAvailable.set(true);

    component.nextStep();
    expect(component.currentStep()).toBe(2);

    // Paso 2: probar preset de ciudad Santa Cruz
    component.selectCityPreset({ name: 'Santa Cruz', latitude: -17.7833, longitude: -63.1821 });
    expect(component.step2Form.controls.latitude.value).toBe(-17.7833);

    component.nextStep();
    expect(component.currentStep()).toBe(3);

    // Paso 3: elegir plantilla TRADITIONAL y abrir resumen
    component.selectTemplate('TRADITIONAL');
    expect(component.selectedTemplate()).toBe('TRADITIONAL');
    expect(component.includeDemoCatalog()).toBe(true);

    component.openSummaryModal();
    expect(component.isSummaryModalOpen()).toBe(true);

    // Confirmar y enviar
    const tourSpy = vi.spyOn(tourService, 'startTour');
    component.confirmAndSubmitOnboarding();

    expect(onboardingServiceSpy.registerOnboarding).toHaveBeenCalledWith({
      company: {
        legalName: 'Pizzería Bella Napoli',
        slug: 'bella-napoli',
        nitOrTaxId: null,
      },
      branch: {
        name: 'Sucursal Principal',
        address: null,
        phone: null,
        latitude: -17.7833,
        longitude: -63.1821,
        timezone: 'America/La_Paz',
        currency: 'BOB',
      },
      businessTemplate: 'TRADITIONAL',
      includeDemoCatalog: true,
      adminUser: {
        fullName: 'Carlos Mendoza',
        username: 'carlos',
        email: 'carlos@napoli.com',
        password: 'password123',
      },
    });

    expect(authServiceSpy.establishSessionFromOnboarding).toHaveBeenCalledWith(mockOnboardingResult);
    expect(branchContextSpy.loadBranches).toHaveBeenCalledWith('branch-new-1');
    expect(tourSpy).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/pos']);
  });

  it('debe desactivar includeDemoCatalog automáticamente si se elige la plantilla EMPTY', () => {
    component.selectTemplate('EMPTY');
    expect(component.selectedTemplate()).toBe('EMPTY');
    expect(component.includeDemoCatalog()).toBe(false);
  });

  it('debe mostrar alerta de error RFC 7807 si falla registerOnboarding', () => {
    const errorResponse = new HttpErrorResponse({
      status: 409,
      statusText: 'Conflict',
      error: {
        title: 'Slug Conflict',
        detail: 'El identificador bella-napoli ya se encuentra registrado.',
      },
    });
    onboardingServiceSpy.registerOnboarding.mockReturnValue(throwError(() => errorResponse));

    component.step1Form.setValue({
      legalName: 'Pizzería Bella Napoli',
      slug: 'bella-napoli',
      nitOrTaxId: '',
      fullName: 'Carlos Mendoza',
      username: 'carlos',
      email: '',
      password: 'password123',
    });

    component.confirmAndSubmitOnboarding();
    fixture.detectChanges();

    expect(component.isSubmitting()).toBe(false);
    expect(component.errorMessage()).toBe('El identificador bella-napoli ya se encuentra registrado.');
  });
});
