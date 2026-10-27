import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
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
  readonly themeService = inject(ThemeService);
  readonly authService = inject(AuthService);
  readonly branchContextService = inject(BranchContextService);
  readonly title = 'Gestión Restaurante';

  readonly isBranchDropdownOpen = signal<boolean>(false);

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.branchContextService.loadBranches().subscribe();
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
  }
}
