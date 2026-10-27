import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Observable, map, of, catchError } from 'rxjs';
import { CompanyService } from './company.service';
import { AuthService } from './auth.service';
import { BranchDto } from '../models/company.models';

export const ACTIVE_BRANCH_ID_KEY = 'active_branch_id';

@Injectable({
  providedIn: 'root',
})
export class BranchContextService {
  private readonly companyService = inject(CompanyService);
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly branches = signal<BranchDto[]>([]);
  readonly activeBranch = signal<BranchDto | null>(null);
  readonly activeBranchId = computed(() => this.activeBranch()?.id ?? null);
  readonly isLoading = signal<boolean>(false);

  /**
   * Carga las sucursales autorizadas del usuario autenticado y resuelve la sucursal activa.
   */
  loadBranches(preferredBranchId?: string | null): Observable<BranchDto[]> {
    const user = this.authService.currentUser();
    if (!user) {
      this.clear();
      return of([]);
    }

    this.isLoading.set(true);
    return this.companyService.getBranches().pipe(
      map((allBranches) => {
        let authorized = allBranches;
        const isFullAdmin = user.role === 'ADMIN_TENANT' || user.role === 'SUPERADMIN';
        if (!isFullAdmin && user.authorizedBranchIds && user.authorizedBranchIds.length > 0) {
          authorized = allBranches.filter((b) => user.authorizedBranchIds!.includes(b.id));
        }

        this.branches.set(authorized);

        // Resolución de sucursal activa:
        // 1. preferredBranchId (si se proporciona)
        // 2. localStorage
        // 3. user.branchId asignado
        // 4. Primera sucursal autorizada de la lista
        const savedId =
          preferredBranchId ||
          (this.isBrowser() ? localStorage.getItem(ACTIVE_BRANCH_ID_KEY) : null);

        const resolved =
          (savedId ? authorized.find((b) => b.id === savedId) : null) ||
          (user.branchId ? authorized.find((b) => b.id === user.branchId) : null) ||
          authorized[0] ||
          null;

        this.setActiveBranch(resolved);
        this.isLoading.set(false);
        return authorized;
      }),
      catchError((err) => {
        this.isLoading.set(false);
        throw err;
      })
    );
  }

  setActiveBranch(branch: BranchDto | null): void {
    this.activeBranch.set(branch);
    if (this.isBrowser()) {
      if (branch) {
        localStorage.setItem(ACTIVE_BRANCH_ID_KEY, branch.id);
      } else {
        localStorage.removeItem(ACTIVE_BRANCH_ID_KEY);
      }
    }
  }

  clear(): void {
    this.branches.set([]);
    this.activeBranch.set(null);
    if (this.isBrowser()) {
      localStorage.removeItem(ACTIVE_BRANCH_ID_KEY);
    }
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId) && typeof window !== 'undefined';
  }
}
