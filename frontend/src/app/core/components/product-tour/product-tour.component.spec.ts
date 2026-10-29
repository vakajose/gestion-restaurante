import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ProductTourComponent } from './product-tour.component';
import { OnboardingTourService } from '../../services/onboarding-tour.service';

describe('ProductTourComponent', () => {
  let component: ProductTourComponent;
  let fixture: ComponentFixture<ProductTourComponent>;
  let tourService: OnboardingTourService;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [ProductTourComponent],
      providers: [OnboardingTourService, provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductTourComponent);
    component = fixture.componentInstance;
    tourService = TestBed.inject(OnboardingTourService);
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('no debe mostrar el modal cuando el tour está inactivo', () => {
    expect(component).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="product-tour-modal"]')).toBeFalsy();
  });

  it('debe renderizar el modal con el Hito 1 cuando se activa el tour y navegar entre pasos', () => {
    tourService.startTour();
    fixture.detectChanges();

    const modal = fixture.nativeElement.querySelector('[data-testid="product-tour-modal"]');
    expect(modal).toBeTruthy();

    const title = fixture.nativeElement.querySelector('[data-testid="tour-milestone-title"]');
    expect(title.textContent).toContain('Hito 1: Apertura de Caja en POS');

    const nextBtn = fixture.nativeElement.querySelector('[data-testid="tour-next-btn"]') as HTMLButtonElement;
    nextBtn.click();
    fixture.detectChanges();

    expect(title.textContent).toContain('Hito 2: Cobro de tu Primer Plato Demo');
  });

  it('debe cerrar y saltar el tour al presionar la tecla Escape', () => {
    tourService.startTour();
    fixture.detectChanges();
    expect(tourService.isTourActive()).toBe(true);

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(tourService.isTourActive()).toBe(false);
    expect(tourService.isCompleted()).toBe(true);
  });
});
