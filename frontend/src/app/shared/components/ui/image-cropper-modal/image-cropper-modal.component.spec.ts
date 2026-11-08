import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach, vi } from 'vitest';
import { ImageCropperModalComponent } from './image-cropper-modal.component';

describe('ImageCropperModalComponent', () => {
  let component: ImageCropperModalComponent;
  let fixture: ComponentFixture<ImageCropperModalComponent>;

  const mockFile = new File(['fake-image-bytes'], 'dish.jpg', { type: 'image/jpeg' });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImageCropperModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ImageCropperModalComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('imageFile', mockFile);
    fixture.detectChanges();
  });

  it('debe crearse correctamente e inicializar el zoom en 1.0', () => {
    expect(component).toBeTruthy();
    expect(component.zoom()).toBe(1.0);
    expect(component.isDragging()).toBe(false);
  });

  it('debe aumentar y disminuir el zoom con zoomIn() y zoomOut() dentro de los límites', () => {
    component.zoomIn();
    expect(component.zoom()).toBe(1.2);

    component.zoomOut();
    expect(component.zoom()).toBe(1.0);

    // No debe reducirse por debajo de 1.0
    component.zoomOut();
    expect(component.zoom()).toBe(1.0);

    // Forzar zoom máximo
    component.setZoom(3.5);
    expect(component.zoom()).toBe(3.0);
  });

  it('debe manejar eventos de puntero (pointerdown, pointermove, pointerup) para el paneo', () => {
    // Simular que la imagen se cargó
    component.isImageLoaded.set(true);

    const downEvent = new PointerEvent('pointerdown', { clientX: 100, clientY: 100 });
    component.onPointerDown(downEvent);
    expect(component.isDragging()).toBe(true);

    const moveEvent = new PointerEvent('pointermove', { clientX: 150, clientY: 130 });
    component.onPointerMove(moveEvent);
    expect(component.offsetX()).toBe(50);
    expect(component.offsetY()).toBe(30);

    const upEvent = new PointerEvent('pointerup', { clientX: 150, clientY: 130 });
    component.onPointerUp(upEvent);
    expect(component.isDragging()).toBe(false);
  });

  it('debe restablecer el encuadre con resetCrop()', () => {
    component.setZoom(2.5);
    component.offsetX.set(80);
    component.offsetY.set(40);

    component.resetCrop();
    expect(component.zoom()).toBe(1.0);
  });

  it('debe emitir cancelled al pulsar el botón de cancelar', () => {
    const cancelSpy = vi.fn();
    component.cancelled.subscribe(cancelSpy);

    component.onCancel();
    expect(cancelSpy).toHaveBeenCalled();
  });

  it('debe procesar confirmCrop() y emitir un Blob WebP optimizado', () => {
    const croppedSpy = vi.fn();
    component.cropped.subscribe(croppedSpy);

    component.confirmCrop();
    expect(croppedSpy).toHaveBeenCalled();
    const emittedBlob = croppedSpy.mock.calls[0][0];
    expect(emittedBlob).toBeInstanceOf(Blob);
  });
});
