import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmojiPickerComponent } from './emoji-picker.component';
import { By } from '@angular/platform-browser';

describe('EmojiPickerComponent', () => {
  let component: EmojiPickerComponent;
  let fixture: ComponentFixture<EmojiPickerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmojiPickerComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EmojiPickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse correctamente con emoji por defecto 🍽️', () => {
    expect(component).toBeTruthy();
    expect(component.selectedEmoji()).toBe('🍽️');
    expect(component.isOpen()).toBe(false);
  });

  it('debe abrir y cerrar el popover al pulsar el botón disparador', () => {
    const triggerBtn = fixture.debugElement.query(By.css('[data-testid="emoji-picker-trigger"]'));
    expect(triggerBtn).toBeTruthy();

    triggerBtn.nativeElement.click();
    fixture.detectChanges();

    expect(component.isOpen()).toBe(true);
    const popover = fixture.debugElement.query(By.css('[data-testid="emoji-picker-popover"]'));
    expect(popover).toBeTruthy();

    triggerBtn.nativeElement.click();
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
  });

  it('debe filtrar emojis por término de búsqueda', () => {
    component.open();
    fixture.detectChanges();

    component.searchQuery.set('burger');
    fixture.detectChanges();

    const filtered = component.filteredEmojis();
    expect(filtered.some((e) => e.char === '🍔')).toBe(true);
    expect(
      filtered.every(
        (e) =>
          e.name.toLowerCase().includes('burger') ||
          e.keywords.toLowerCase().includes('burger') ||
          e.char.includes('burger'),
      ),
    ).toBe(true);
  });

  it('debe filtrar emojis al seleccionar una categoría específica', () => {
    component.open();
    fixture.detectChanges();

    component.selectSection('drinks');
    fixture.detectChanges();

    const filtered = component.filteredEmojis();
    expect(filtered.some((e) => e.char === '☕')).toBe(true);
    expect(filtered.some((e) => e.char === '🍺')).toBe(true);
    expect(filtered.some((e) => e.char === '🍔')).toBe(false);
  });

  it('debe emitir emojiSelected y cerrar el popover al hacer clic en un emoji', () => {
    const emitSpy = vi.spyOn(component.emojiSelected, 'emit');

    component.open();
    fixture.detectChanges();

    const emojiBtns = fixture.debugElement.queryAll(By.css('.emoji-item-btn'));
    const emojiBtn = emojiBtns.find((b) => b.nativeElement.textContent?.trim() === '🍔');
    expect(emojiBtn).toBeTruthy();

    emojiBtn!.nativeElement.click();
    fixture.detectChanges();

    expect(emitSpy).toHaveBeenCalledWith('🍔');
    expect(component.isOpen()).toBe(false);
  });

  it('debe permitir ingresar un emoji personalizado y emitirlo', () => {
    const emitSpy = vi.spyOn(component.emojiSelected, 'emit');

    component.open();
    fixture.detectChanges();

    const customInput = fixture.debugElement.query(By.css('[data-testid="emoji-custom-input"]'));
    expect(customInput).toBeTruthy();

    customInput.nativeElement.value = '🍇';
    customInput.nativeElement.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(emitSpy).toHaveBeenCalledWith('🍇');
    expect(component.isOpen()).toBe(false);
  });

  it('debe cerrar el popover al hacer clic en el backdrop', () => {
    component.open();
    fixture.detectChanges();

    expect(component.isOpen()).toBe(true);

    const backdrop = fixture.debugElement.query(By.css('.fixed.inset-0'));
    expect(backdrop).toBeTruthy();

    backdrop.nativeElement.click();
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
  });
});
