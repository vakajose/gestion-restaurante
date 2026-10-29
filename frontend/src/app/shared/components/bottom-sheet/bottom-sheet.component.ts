import {
  Component,
  HostListener,
  OnDestroy,
  PLATFORM_ID,
  effect,
  inject,
  input,
  model,
  signal,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-bottom-sheet',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bottom-sheet.component.html',
})
export class BottomSheetComponent implements OnDestroy {
  private readonly platformId = inject(PLATFORM_ID);

  readonly isOpen = model<boolean>(false);
  readonly title = input<string>('');
  readonly maxHeight = input<string>('85vh');
  readonly dismissible = input<boolean>(true);

  readonly dragOffsetY = signal<number>(0);
  private touchStartY: number | null = null;
  private readonly swipeThresholdPx = 60;

  constructor() {
    effect(() => {
      const open = this.isOpen();
      if (this.isBrowser()) {
        document.body.style.overflow = open ? 'hidden' : '';
      }
      if (!open) {
        this.dragOffsetY.set(0);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.isBrowser()) {
      document.body.style.overflow = '';
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isOpen() && this.dismissible()) {
      this.close();
    }
  }

  onBackdropClick(): void {
    if (this.dismissible()) {
      this.close();
    }
  }

  close(): void {
    this.isOpen.set(false);
    this.dragOffsetY.set(0);
  }

  onTouchStart(event: TouchEvent): void {
    if (!this.dismissible() || event.touches.length === 0) {
      return;
    }
    this.touchStartY = event.touches[0].clientY;
  }

  onTouchMove(event: TouchEvent): void {
    if (!this.dismissible() || this.touchStartY === null || event.touches.length === 0) {
      return;
    }
    const deltaY = event.touches[0].clientY - this.touchStartY;
    if (deltaY > 0) {
      this.dragOffsetY.set(deltaY);
    }
  }

  onTouchEnd(): void {
    if (!this.dismissible() || this.touchStartY === null) {
      return;
    }
    const currentOffset = this.dragOffsetY();
    this.touchStartY = null;

    if (currentOffset >= this.swipeThresholdPx) {
      this.close();
    } else {
      this.dragOffsetY.set(0);
    }
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId) && typeof document !== 'undefined';
  }
}
