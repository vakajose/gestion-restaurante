import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CompanyService } from '../../core/services/company.service';
import { TenantDto, BranchDto } from '../../core/models/company.models';
import { ThemeService } from '../../core/services/theme.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-company',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './company.component.html',
})
export class CompanyComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly companyService = inject(CompanyService);
  readonly themeService = inject(ThemeService);

  readonly activeTab = signal<'company' | 'branches'>('company');
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly message = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  readonly company = signal<TenantDto | null>(null);
  readonly branches = signal<BranchDto[]>([]);

  // Modal sucursal
  readonly isBranchModalOpen = signal<boolean>(false);
  readonly editingBranch = signal<BranchDto | null>(null);

  readonly companyForm = this.fb.group({
    legalName: ['', [Validators.required, Validators.maxLength(150)]],
    nitOrTaxId: ['', [Validators.required, Validators.maxLength(50)]],
    slug: ['', [Validators.required, Validators.maxLength(60)]],
  });

  readonly branchForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    address: [''],
    phone: ['', [Validators.maxLength(30)]],
    timezone: ['America/La_Paz', [Validators.required]],
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.companyService.getCompany().subscribe({
      next: (comp) => {
        this.company.set(comp);
        this.companyForm.patchValue({
          legalName: comp.legalName,
          nitOrTaxId: comp.nitOrTaxId,
          slug: comp.slug,
        });
        this.loadBranches();
      },
      error: () => {
        this.isLoading.set(false);
        this.message.set({ type: 'error', text: 'Error al cargar la información de la empresa.' });
      },
    });
  }

  loadBranches(): void {
    this.companyService.getBranches().subscribe({
      next: (list) => {
        this.branches.set(list);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.message.set({ type: 'error', text: 'Error al cargar las sucursales.' });
      },
    });
  }

  saveCompany(): void {
    if (this.companyForm.invalid || this.isSaving()) {
      this.companyForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.message.set(null);

    const val = this.companyForm.getRawValue();
    this.companyService
      .updateCompany({
        legalName: val.legalName ?? '',
        nitOrTaxId: val.nitOrTaxId ?? '',
        slug: val.slug ?? '',
      })
      .subscribe({
        next: (updated) => {
          this.company.set(updated);
          this.isSaving.set(false);
          this.message.set({ type: 'success', text: 'Datos de la empresa actualizados exitosamente.' });
        },
        error: (err) => {
          this.isSaving.set(false);
          const detail = err.error?.detail || err.error?.title || 'No se pudo actualizar la empresa.';
          this.message.set({ type: 'error', text: detail });
        },
      });
  }

  openCreateBranchModal(): void {
    this.editingBranch.set(null);
    this.branchForm.reset({
      name: '',
      address: '',
      phone: '',
      timezone: 'America/La_Paz',
    });
    this.isBranchModalOpen.set(true);
  }

  openEditBranchModal(branch: BranchDto): void {
    this.editingBranch.set(branch);
    this.branchForm.patchValue({
      name: branch.name,
      address: branch.address ?? '',
      phone: branch.phone ?? '',
      timezone: branch.timezone,
    });
    this.isBranchModalOpen.set(true);
  }

  closeBranchModal(): void {
    this.isBranchModalOpen.set(false);
    this.editingBranch.set(null);
  }

  saveBranch(): void {
    if (this.branchForm.invalid || this.isSaving()) {
      this.branchForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.message.set(null);

    const val = this.branchForm.getRawValue();
    const editing = this.editingBranch();

    if (editing) {
      this.companyService
        .updateBranch(editing.id, {
          name: val.name ?? '',
          address: val.address ?? '',
          phone: val.phone ?? '',
          timezone: val.timezone ?? 'America/La_Paz',
          isActive: editing.isActive,
        })
        .subscribe({
          next: () => {
            this.isSaving.set(false);
            this.closeBranchModal();
            this.message.set({ type: 'success', text: 'Sucursal actualizada exitosamente.' });
            this.loadBranches();
          },
          error: (err) => {
            this.isSaving.set(false);
            const detail = err.error?.detail || err.error?.title || 'Error al actualizar la sucursal.';
            this.message.set({ type: 'error', text: detail });
          },
        });
    } else {
      this.companyService
        .createBranch({
          name: val.name ?? '',
          address: val.address ?? '',
          phone: val.phone ?? '',
          timezone: val.timezone ?? 'America/La_Paz',
        })
        .subscribe({
          next: () => {
            this.isSaving.set(false);
            this.closeBranchModal();
            this.message.set({ type: 'success', text: 'Sucursal creada exitosamente.' });
            this.loadBranches();
          },
          error: (err) => {
            this.isSaving.set(false);
            const detail = err.error?.detail || err.error?.title || 'Error al crear la sucursal.';
            this.message.set({ type: 'error', text: detail });
          },
        });
    }
  }

  toggleBranchStatus(branch: BranchDto): void {
    const nextStatus = !branch.isActive;
    this.companyService.toggleBranchStatus(branch.id, nextStatus).subscribe({
      next: (updated) => {
        this.branches.update((list) =>
          list.map((b) => (b.id === updated.id ? updated : b))
        );
        this.message.set({
          type: 'success',
          text: `Sucursal "${updated.name}" ahora está ${updated.isActive ? 'Activa' : 'Inactiva'}.`,
        });
      },
      error: () => {
        this.message.set({ type: 'error', text: 'Error al cambiar estado de la sucursal.' });
      },
    });
  }

  clearMessage(): void {
    this.message.set(null);
  }
}
