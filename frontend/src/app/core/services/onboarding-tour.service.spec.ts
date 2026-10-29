import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import {
  OnboardingTourService,
  ONBOARDING_TOUR_COMPLETED_KEY,
} from './onboarding-tour.service';

describe('OnboardingTourService', () => {
  let service: OnboardingTourService;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [OnboardingTourService, provideRouter([])],
    });

    service = TestBed.inject(OnboardingTourService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('debe inicializarse con 4 hitos interactivos e inactivo por defecto', () => {
    expect(service.totalSteps()).toBe(4);
    expect(service.isTourActive()).toBe(false);
    expect(service.currentStepIndex()).toBe(0);
    expect(service.isCompleted()).toBe(false);
    expect(service.currentMilestone().stepNumber).toBe(1);
  });

  it('debe iniciar el tour desde el paso 0 al llamar a startTour()', () => {
    service.startTour();
    expect(service.isTourActive()).toBe(true);
    expect(service.currentStepIndex()).toBe(0);
    expect(service.progressPercentage()).toBe(25);
  });

  it('debe avanzar por los 4 hitos con nextStep() y completar al finalizar el cuarto paso', () => {
    service.startTour();

    service.nextStep();
    expect(service.currentStepIndex()).toBe(1);
    expect(service.progressPercentage()).toBe(50);

    service.nextStep();
    expect(service.currentStepIndex()).toBe(2);
    expect(service.progressPercentage()).toBe(75);

    service.nextStep();
    expect(service.currentStepIndex()).toBe(3);
    expect(service.progressPercentage()).toBe(100);

    // Al avanzar desde el último paso se completa y cierra el tour
    service.nextStep();
    expect(service.isTourActive()).toBe(false);
    expect(service.isCompleted()).toBe(true);
    expect(localStorage.getItem(ONBOARDING_TOUR_COMPLETED_KEY)).toBe('true');
  });

  it('debe retroceder con prevStep() sin bajar de 0', () => {
    service.startTour();
    service.prevStep();
    expect(service.currentStepIndex()).toBe(0);

    service.nextStep();
    expect(service.currentStepIndex()).toBe(1);

    service.prevStep();
    expect(service.currentStepIndex()).toBe(0);
  });

  it('debe permitir saltar el tour con skipTour() y persistir onboarding_tour_completed = true', () => {
    service.startTour();
    service.skipTour();

    expect(service.isTourActive()).toBe(false);
    expect(service.isCompleted()).toBe(true);
    expect(localStorage.getItem(ONBOARDING_TOUR_COMPLETED_KEY)).toBe('true');
  });

  it('debe reiniciar el estado y volver a abrir el tour con resetAndRestartTour()', () => {
    service.completeTour();
    expect(service.isCompleted()).toBe(true);

    service.resetAndRestartTour();
    expect(service.isCompleted()).toBe(false);
    expect(service.isTourActive()).toBe(true);
    expect(service.currentStepIndex()).toBe(0);
    expect(localStorage.getItem(ONBOARDING_TOUR_COMPLETED_KEY)).toBe('false');
  });

  it('debe navegar a la ruta del hito actual con navigateToCurrentMilestoneRoute()', () => {
    service.startTour();
    service.navigateToCurrentMilestoneRoute();
    expect(router.navigate).toHaveBeenCalledWith(['/pos']);

    service.currentStepIndex.set(2);
    service.navigateToCurrentMilestoneRoute();
    expect(router.navigate).toHaveBeenCalledWith(['/inventory']);
  });
});
