import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BottomSheetComponent } from './bottom-sheet.component';

describe('BottomSheetComponent (TASK-317 / PB-028)', () => {
  let component: BottomSheetComponent;
  let fixture: ComponentFixture<BottomSheetComponent>;

  beforeEach(async () => {
    document.body.style.overflow = '';

    await TestBed.configureTestingModule({
      imports: [BottomSheetComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BottomSheetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    document.body.style.overflow = '';
  });

  it('debe inicializarse cerrado por defecto y sin bloquear el scroll del body', () => {
    expect(component).toBeTruthy();
    expect(component.isOpen()).toBe(false);
    expect(document.body.style.overflow).toBe('');
    expect(fixture.nativeElement.querySelector('[data-testid="bottom-sheet-backdrop"]')).toBeFalsy();
  });

  it('debe mostrar el backdrop, bloquear el scroll del body y renderizar el drag handle al abrirse', () => {
    fixture.componentRef.setInput('title', 'Comanda Actual');
    component.isOpen.set(true);
    fixture.detectChanges();

    expect(document.body.style.overflow).toBe('hidden');
    expect(fixture.nativeElement.querySelector('[data-testid="bottom-sheet-backdrop"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="bottom-sheet-drag-handle"]')).toBeTruthy();

    const titleEl = fixture.nativeElement.querySelector('[data-testid="bottom-sheet-title"]');
    expect(titleEl.textContent.trim()).toBe('Comanda Actual');
  });

  it('debe cerrarse y restaurar el scroll al hacer clic en el backdrop o en el botón cerrar', () => {
    component.isOpen.set(true);
    fixture.detectChanges();
    expect(document.body.style.overflow).toBe('hidden');

    const backdrop = fixture.nativeElement.querySelector(
      '[data-testid="bottom-sheet-backdrop"]'
    ) as HTMLElement;
    backdrop.click();
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
    expect(document.body.style.overflow).toBe('');

    // Reabrir y probar botón cerrar
    component.isOpen.set(true);
    fixture.detectChanges();
    const closeBtn = fixture.nativeElement.querySelector(
      '[data-testid="bottom-sheet-close-btn"]'
    ) as HTMLButtonElement;
    closeBtn.click();
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
  });

  it('debe cerrarse al presionar la tecla Escape cuando dismissible es true', () => {
    component.isOpen.set(true);
    fixture.detectChanges();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
  });

  it('debe cerrarse al arrastrar hacia abajo superando el umbral táctil (swipe down)', () => {
    component.isOpen.set(true);
    fixture.detectChanges();

    component.onTouchStart({ touches: [{ clientY: 100 }] } as unknown as TouchEvent);
    component.onTouchMove({ touches: [{ clientY: 185 }] } as unknown as TouchEvent);
    expect(component.dragOffsetY()).toBe(85);

    component.onTouchEnd();
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
    expect(component.dragOffsetY()).toBe(0);
  });
});
