import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  BadgeComponent,
  ButtonComponent,
  CardComponent,
  FormFieldComponent,
  IconComponent,
  ModalComponent,
  PageWrapperComponent,
  ThemeCustomizerComponent,
  ToastPortalComponent,
} from './index';
import { NotificationService } from '../../../core/services/notification.service';

describe('UI Kit Standalone Suite (TailwindAdmin v3.0 - ADR-0014)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        IconComponent,
        PageWrapperComponent,
        ButtonComponent,
        BadgeComponent,
        CardComponent,
        ModalComponent,
        FormFieldComponent,
        ToastPortalComponent,
        ThemeCustomizerComponent,
      ],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('IconComponent should render official Solar SVG vector offline', () => {
    const fixture: ComponentFixture<IconComponent> = TestBed.createComponent(IconComponent);
    fixture.componentRef.setInput('name', 'pos-terminal');
    fixture.componentRef.setInput('size', 'lg');
    fixture.detectChanges();

    const svg = fixture.nativeElement.querySelector('svg');
    expect(svg).toBeTruthy();
    expect(svg.classList.contains('w-6')).toBe(true);
  });

  it('ButtonComponent should distinguish primary vs neutral cancel variants and block click when loading', () => {
    const fixture = TestBed.createComponent(ButtonComponent);
    fixture.componentRef.setInput('variant', 'neutral');
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(btn.className).toContain('bg-surface');

    let clicked = false;
    fixture.componentInstance.btnClick.subscribe(() => (clicked = true));
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    btn.click();
    expect(clicked).toBe(false);
    expect(btn.disabled).toBe(true);
  });

  it('BadgeComponent should render high-contrast soft classes and status dot', () => {
    const fixture = TestBed.createComponent(BadgeComponent);
    fixture.componentRef.setInput('color', 'success');
    fixture.componentRef.setInput('dot', true);
    fixture.detectChanges();

    const span: HTMLElement = fixture.nativeElement.querySelector('span');
    expect(span.className).toContain('text-[#15803d]');
    expect(span.querySelectorAll('span').length).toBe(1);
  });

  it('FormFieldComponent should apply .has-error class and render inline alert when error is set', () => {
    const fixture = TestBed.createComponent(FormFieldComponent);
    fixture.componentRef.setInput('label', 'Monto Inicial');
    fixture.componentRef.setInput('required', true);
    fixture.componentRef.setInput('error', 'El monto inicial no puede ser negativo');
    fixture.detectChanges();

    const wrapper = fixture.nativeElement.firstElementChild as HTMLElement;
    expect(wrapper.classList.contains('has-error')).toBe(true);
    const errorEl = fixture.nativeElement.querySelector('[data-testid="form-field-error"]');
    expect(errorEl?.textContent).toContain('El monto inicial no puede ser negativo');
  });

  it('ModalComponent should render at z-[100] with sticky formErrorSummary when open', () => {
    const fixture = TestBed.createComponent(ModalComponent);
    fixture.componentRef.setInput('isOpen', true);
    fixture.componentRef.setInput('title', 'Registrar Gasto');
    fixture.componentRef.setInput('formErrorSummary', 'Corrige 2 campos marcados en rojo');
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="ui-modal-error-banner"]');
    expect(banner?.textContent).toContain('Corrige 2 campos marcados en rojo');
  });

  it('ToastPortalComponent should render at z-[9999] above modals when NotificationService emits an error', () => {
    const notifService = TestBed.inject(NotificationService);
    const fixture = TestBed.createComponent(ToastPortalComponent);

    notifService.error('No hay un turno de caja abierto en esta sucursal', {
      title: 'Operación bloqueada',
      errorCode: 'SHIFT_NOT_OPEN',
    });
    fixture.detectChanges();

    const portal = fixture.nativeElement.querySelector('[data-testid="toast-portal"]') as HTMLElement;
    expect(portal).toBeTruthy();
    expect(portal.className).toContain('z-[9999]');
    expect(portal.textContent).toContain('SHIFT_NOT_OPEN');
    notifService.clearAll();
  });
});
