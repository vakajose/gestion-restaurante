import { Component, inject, signal, OnInit, DestroyRef, HostListener } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { ThemeService } from './core/services/theme.service';
import { AuthService } from './core/services/auth.service';
import { BranchContextService } from './core/services/branch-context.service';
import { BranchDto } from './core/models/company.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly themeService = inject(ThemeService);
  readonly authService = inject(AuthService);
  readonly branchContextService = inject(BranchContextService);
  readonly title = 'Gestión Restaurante';

  readonly isBranchDropdownOpen = signal<boolean>(false);
  readonly isSidebarOpen = signal<boolean>(false);

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
    }
  }

  toggleSidebar(): void {
    this.isSidebarOpen.update((open) => !open);
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

  logout(): void {
    this.branchContextService.clear();
    this.authService.logout();
    this.closeSidebar();
  }
}
