import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, signal } from '@angular/core';
import { ContextHelpComponent } from './context-help.component';
import { ParameterHelpDto } from '../../../../core/models/configuration.model';

@Component({
  standalone: true,
  imports: [ContextHelpComponent],
  template: `
    <app-context-help [help]="help()" [paramName]="paramName()"></app-context-help>
  `,
})
class TestHostComponent {
  readonly help = signal<ParameterHelpDto | null>({
    impact: 'Determina si el sistema permite cobrar ventas cuando no hay existencias.',
    example: 'Si vendes una hamburguesa y no hay pan, el modo estricto bloquea la comanda.',
  });
  readonly paramName = signal<string>('Modo de Deducción');
}

describe('ContextHelpComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let hostComponent: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente y mostrar el botón (?) cuando hay datos de ayuda', () => {
    const btn = fixture.nativeElement.querySelector('[data-testid="context-help-btn"]');
    expect(btn).toBeTruthy();
    expect(btn.textContent.trim()).toBe('?');
  });

  it('no debe renderizar nada si help es null', () => {
    hostComponent.help.set(null);
    fixture.detectChanges();

    const btn = fixture.nativeElement.querySelector('[data-testid="context-help-btn"]');
    expect(btn).toBeFalsy();
  });

  it('debe abrir el popover al hacer clic en el botón (?) y mostrar impacto y ejemplo', () => {
    const btn = fixture.nativeElement.querySelector('[data-testid="context-help-btn"]') as HTMLButtonElement;
    btn.click();
    fixture.detectChanges();

    const popover = fixture.nativeElement.querySelector('[data-testid="context-help-popover"]');
    expect(popover).toBeTruthy();

    const impact = fixture.nativeElement.querySelector('[data-testid="context-help-impact"]');
    expect(impact.textContent).toContain('Determina si el sistema permite cobrar ventas');

    const example = fixture.nativeElement.querySelector('[data-testid="context-help-example"]');
    expect(example.textContent).toContain('Si vendes una hamburguesa');
  });

  it('debe cerrar el popover al hacer clic en el botón de cerrar ✕', () => {
    const btn = fixture.nativeElement.querySelector('[data-testid="context-help-btn"]') as HTMLButtonElement;
    btn.click();
    fixture.detectChanges();

    const closeBtn = fixture.nativeElement.querySelector('[data-testid="context-help-close-btn"]') as HTMLButtonElement;
    closeBtn.click();
    fixture.detectChanges();

    const popover = fixture.nativeElement.querySelector('[data-testid="context-help-popover"]');
    expect(popover).toBeFalsy();
  });

  it('debe cerrar el popover al presionar la tecla Escape', () => {
    const btn = fixture.nativeElement.querySelector('[data-testid="context-help-btn"]') as HTMLButtonElement;
    btn.click();
    fixture.detectChanges();

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    const popover = fixture.nativeElement.querySelector('[data-testid="context-help-popover"]');
    expect(popover).toBeFalsy();
  });
});
