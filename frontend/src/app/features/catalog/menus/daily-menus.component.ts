import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogService } from '../../../core/services/catalog.service';
import { AuthService } from '../../../core/services/auth.service';
import { BranchContextService } from '../../../core/services/branch-context.service';
import {
  CourseType,
  CreateDailyMenuItemRequest,
  CreateDailyMenuRequest,
  DailyMenuDto,
  DishPriceDto,
  PriceMode,
} from '../../../core/models/catalog.models';
import {
  BadgeComponent,
  ButtonComponent,
  CardComponent,
  ModalComponent,
  PageWrapperComponent,
  UiBadgeColor,
} from '../../../shared/components/ui';

@Component({
  selector: 'app-daily-menus',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PageWrapperComponent,
    CardComponent,
    ButtonComponent,
    BadgeComponent,
    ModalComponent,
  ],
  templateUrl: './daily-menus.component.html',
})
export class DailyMenusComponent implements OnInit {
  private readonly catalogService = inject(CatalogService);
  private readonly branchContextService = inject(BranchContextService);
  readonly authService = inject(AuthService);

  // Estados de Datos
  readonly menus = signal<DailyMenuDto[]>([]);
  readonly dishes = signal<DishPriceDto[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  // Filtros
  readonly searchQuery = signal<string>('');
  readonly selectedDayFilter = signal<string>('ALL');
  readonly expandedMenuId = signal<string | null>(null);

  // Modal de Creación
  readonly showCreateModal = signal<boolean>(false);
  readonly formName = signal<string>('');
  readonly formDescription = signal<string>('');
  readonly formDayOfWeek = signal<string>('ALL');
  readonly formDaysOfWeek = signal<string[]>(['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES']);
  readonly formBasePrice = signal<number | null>(25);
  readonly formItems = signal<CreateDailyMenuItemRequest[]>([]);

  readonly weekDays = [
    { value: 'LUNES', short: 'Lun' },
    { value: 'MARTES', short: 'Mar' },
    { value: 'MIERCOLES', short: 'Mié' },
    { value: 'JUEVES', short: 'Jue' },
    { value: 'VIERNES', short: 'Vie' },
    { value: 'SABADO', short: 'Sáb' },
    { value: 'DOMINGO', short: 'Dom' },
  ];

  // Campos para agregar nuevo ítem al formulario
  readonly selectedDishId = signal<string>('');
  readonly selectedCourseType = signal<CourseType>('SEGUNDO');
  readonly selectedSuggestedQuota = signal<number>(20);
  readonly selectedPriceMode = signal<PriceMode>('INCLUDED');
  readonly selectedSurcharge = signal<number>(0);

  readonly daysOfWeekList = [
    { value: 'ALL', label: 'Todos los Días' },
    { value: 'LUNES', label: 'Lunes' },
    { value: 'MARTES', label: 'Martes' },
    { value: 'MIERCOLES', label: 'Miércoles' },
    { value: 'JUEVES', label: 'Jueves' },
    { value: 'VIERNES', label: 'Viernes' },
    { value: 'SABADO', label: 'Sábado' },
    { value: 'DOMINGO', label: 'Domingo' },
  ];

  readonly courseTypesList: CourseType[] = ['ENTRADA', 'SEGUNDO', 'POSTRE', 'BEBIDA', 'GENERAL'];

  readonly filteredMenus = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const day = this.selectedDayFilter();
    return this.menus().filter((m) => {
      const matchesDay = day === 'ALL' || m.dayOfWeek === day || m.dayOfWeek === 'ALL' || !m.dayOfWeek;
      const matchesQuery =
        !query ||
        m.name.toLowerCase().includes(query) ||
        (m.description && m.description.toLowerCase().includes(query));
      return matchesDay && matchesQuery;
    });
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    const branchId = this.branchContextService.activeBranchId() || this.authService.currentUser()?.branchId || undefined;
    this.isLoading.set(true);

    this.catalogService.getDailyMenus(branchId, false).subscribe({
      next: (data) => {
        this.menus.set(data || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Error al cargar las plantillas de menú del día.');
        this.isLoading.set(false);
      },
    });

    this.catalogService.getDishes(branchId).subscribe({
      next: (dishList) => {
        this.dishes.set(dishList.filter((d) => d.isAvailable) || []);
        if (dishList.length > 0 && !this.selectedDishId()) {
          this.selectedDishId.set(dishList[0].dishId);
        }
      },
    });
  }

  toggleExpand(menuId: string): void {
    this.expandedMenuId.update((curr) => (curr === menuId ? null : menuId));
  }

  openCreateModal(): void {
    this.formName.set('');
    this.formDescription.set('');
    this.formDaysOfWeek.set(['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES']);
    this.formDayOfWeek.set('LUNES,MARTES,MIERCOLES,JUEVES,VIERNES');
    this.formBasePrice.set(25);
    this.formItems.set([]);
    if (this.dishes().length > 0) {
      this.selectedDishId.set(this.dishes()[0].dishId);
    }
    this.selectedCourseType.set('SEGUNDO');
    this.selectedSuggestedQuota.set(20);
    this.selectedPriceMode.set('INCLUDED');
    this.selectedSurcharge.set(0);
    this.errorMessage.set(null);
    this.showCreateModal.set(true);
  }

  toggleDayOfWeek(dayValue: string): void {
    this.formDaysOfWeek.update((current) => {
      if (current.includes(dayValue)) {
        return current.filter((d) => d !== dayValue);
      } else {
        return [...current, dayValue];
      }
    });
    const days = this.formDaysOfWeek();
    this.formDayOfWeek.set(days.length === 0 ? 'ALL' : days.join(','));
  }

  isDaySelected(dayValue: string): boolean {
    return this.formDaysOfWeek().includes(dayValue);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  addItemToForm(): void {
    const dishId = this.selectedDishId();
    if (!dishId) {
      this.errorMessage.set('Debes seleccionar un plato para agregar al menú.');
      return;
    }

    const current = this.formItems();
    if (current.some((i) => i.dishId === dishId)) {
      this.errorMessage.set('Este plato ya ha sido agregado a la plantilla.');
      return;
    }

    const newItem: CreateDailyMenuItemRequest = {
      dishId,
      courseType: this.selectedCourseType(),
      suggestedQuota: Math.max(1, this.selectedSuggestedQuota() || 1),
      priceMode: this.selectedPriceMode(),
      surcharge: this.selectedPriceMode() === 'SURCHARGE' ? (this.selectedSurcharge() || 0) : 0,
      sortOrder: current.length + 1,
    };

    this.formItems.update((list) => [...list, newItem]);
    this.errorMessage.set(null);
  }

  removeItemFromForm(index: number): void {
    this.formItems.update((list) => list.filter((_, i) => i !== index));
  }

  getDishName(dishId: string): string {
    const d = this.dishes().find((dish) => dish.dishId === dishId);
    return d ? `${d.code} - ${d.name}` : dishId;
  }

  saveMenu(): void {
    const name = this.formName().trim();
    if (!name) {
      this.errorMessage.set('El nombre de la plantilla de menú es obligatorio.');
      return;
    }

    const items = this.formItems();
    if (items.length === 0) {
      this.errorMessage.set('Debes incluir al menos un plato en la plantilla del menú.');
      return;
    }

    const branchId = this.branchContextService.activeBranchId() || this.authService.currentUser()?.branchId || null;

    const request: CreateDailyMenuRequest = {
      branchId,
      name,
      description: this.formDescription().trim() || null,
      dayOfWeek: this.formDayOfWeek() || 'ALL',
      basePrice: this.formBasePrice() != null ? Number(this.formBasePrice()) : null,
      isTemplate: true,
      items,
    };

    this.isSaving.set(true);
    this.errorMessage.set(null);

    this.catalogService.createDailyMenu(request).subscribe({
      next: (created) => {
        this.isSaving.set(false);
        this.showCreateModal.set(false);
        this.successMessage.set(`Plantilla de menú "${created.name}" creada exitosamente.`);
        this.loadData();
        setTimeout(() => this.successMessage.set(null), 5000);
      },
      error: (err) => {
        this.isSaving.set(false);
        const msg = err.error?.detail || err.error?.message || 'Error al guardar la plantilla de menú.';
        this.errorMessage.set(msg);
      },
    });
  }

  getCourseBadgeColor(course: CourseType | string): UiBadgeColor {
    switch (course) {
      case 'ENTRADA':
        return 'primary';
      case 'SEGUNDO':
        return 'warning';
      case 'POSTRE':
        return 'secondary';
      case 'BEBIDA':
        return 'success';
      default:
        return 'neutral';
    }
  }
}
