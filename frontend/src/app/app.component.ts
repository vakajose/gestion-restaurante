import { Component, computed, inject, signal, OnInit, DestroyRef, HostListener } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { ThemeService } from './core/services/theme.service';
import { AuthService } from './core/services/auth.service';
import { BranchContextService } from './core/services/branch-context.service';
import { OnboardingTourService } from './core/services/onboarding-tour.service';
import { IdleTimerService } from './core/services/idle-timer.service';
import { ProductTourComponent } from './core/components/product-tour/product-tour.component';
import { InactivityWarningModalComponent } from './core/components/inactivity-modal/inactivity-modal.component';
import {
  BadgeComponent,
  IconComponent,
  ThemeCustomizerComponent,
  ToastPortalComponent,
} from './shared/components/ui';
import { BranchDto } from './core/models/company.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ProductTourComponent,
    InactivityWarningModalComponent,
    IconComponent,
    BadgeComponent,
    ToastPortalComponent,
    ThemeCustomizerComponent,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly themeService = inject(ThemeService);
  readonly authService = inject(AuthService);
  readonly branchContextService = inject(BranchContextService);
  readonly tourService = inject(OnboardingTourService);
  readonly idleTimerService = inject(IdleTimerService);
  readonly title = 'Gestión Restaurante';

  readonly isBranchDropdownOpen = signal<boolean>(false);
  readonly isSidebarOpen = signal<boolean>(false);

  /**
   * Indica si en pantallas Desktop (>= 1024px) el sidebar está en modo Mini-Rail (76px solo íconos).
   */
  readonly isMiniRailActive = computed<boolean>(
    () =>
      this.themeService.isDesktopSidebarCollapsed() &&
      this.themeService.sidebarCollapseMode() === 'mini-rail'
  );

  /**
   * Indica si en pantallas Desktop (>= 1024px) el sidebar está oculto por completo (0px).
   */
  readonly isFullHideActive = computed<boolean>(
    () =>
      this.themeService.isDesktopSidebarCollapsed() &&
      this.themeService.sidebarCollapseMode() === 'full-hide'
  );

  readonly sidebarClasses = computed<string>(() => {
    const base =
      'fixed inset-y-0 left-0 z-50 lg:static lg:z-auto w-[270px] flex flex-col flex-shrink-0 sidebar-surface border-r transition-all duration-300 ease-in-out lg:translate-x-0';
    const mobileState = this.isSidebarOpen() ? 'translate-x-0' : '-translate-x-full';
    const desktopState = this.isMiniRailActive()
      ? 'lg:w-[76px]'
      : this.isFullHideActive()
        ? 'lg:w-0 lg:border-r-0 lg:overflow-hidden'
        : 'lg:w-[270px]';
    return `${base} ${mobileState} ${desktopState}`;
  });

  constructor() {
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.closeSidebar();
      });
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.branchContextService.loadBranches().subscribe();
      this.idleTimerService.startWatching();
    }
  }

  toggleSidebar(): void {
    this.isSidebarOpen.update((open) => !open);
    this.themeService.toggleDesktopSidebar();
  }

  closeSidebar(): void {
    this.isSidebarOpen.set(false);
  }

  openSidebar(): void {
    this.isSidebarOpen.set(true);
  }

  @HostListener('window:keydown.escape')
  handleEscape(): void {
    if (this.isSidebarOpen()) {
      this.closeSidebar();
    }
    if (this.isBranchDropdownOpen()) {
      this.closeBranchDropdown();
    }
  }

  toggleBranchDropdown(): void {
    this.isBranchDropdownOpen.update((open) => !open);
  }

  closeBranchDropdown(): void {
    this.isBranchDropdownOpen.set(false);
  }

  selectBranch(branch: BranchDto): void {
    this.branchContextService.setActiveBranch(branch);
    this.isBranchDropdownOpen.set(false);
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  restartTour(): void {
    this.tourService.resetAndRestartTour();
    this.closeSidebar();
  }

  logout(): void {
    this.idleTimerService.stopWatching();
    this.branchContextService.clear();
    this.authService.logout();
    this.closeSidebar();
  }

  getUserInitials(fullName?: string | null, username?: string): string {
    if (fullName && fullName.trim().length > 0) {
      const parts = fullName.trim().split(/\s+/).filter(Boolean);
      if (parts.length >= 2) {
        return (parts[0][0] + parts[1][0]).toUpperCase();
      }
      return parts[0].slice(0, 2).toUpperCase();
    }
    if (username && username.trim().length > 0) {
      return username.trim().slice(0, 2).toUpperCase();
    }
    return 'U';
  }
}
