import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogService } from '../../../core/services/catalog.service';
import { AuthService } from '../../../core/services/auth.service';
import { BranchContextService } from '../../../core/services/branch-context.service';
import {
  BranchServiceSessionDto,
  ComboDto,
  ConfigureServiceSessionRequest,
  CourseType,
  CreateDailyMenuItemRequest,
  CreateDailyMenuRequest,
  DailyMenuDto,
  DishPriceDto,
  PriceMode,
  ServiceSessionMenuMode,
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
  readonly branchContextService = inject(BranchContextService);
  readonly authService = inject(AuthService);

  readonly activeBranchId = computed(
    () => this.branchContextService.activeBranchId() || this.authService.currentUser()?.branchId || null
  );
  readonly branches = this.branchContextService.branches;
  private lastLoadedBranchId: string | null | undefined = undefined;

  // Estados de Datos
  readonly menus = signal<DailyMenuDto[]>([]);
  readonly dishes = signal<DishPriceDto[]>([]);
  readonly combos = signal<ComboDto[]>([]);
  readonly currentSession = signal<BranchServiceSessionDto | null>(null);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  // Filtros
  readonly searchQuery = signal<string>('');
  readonly selectedDayFilter = signal<string>('ALL');
  readonly expandedMenuId = signal<string | null>(null);

  // Modal Crear / Editar Plantilla de Menú
  readonly showCreateModal = signal<boolean>(false);
  readonly editingMenuId = signal<string | null>(null);
  readonly formName = signal<string>('');
  readonly formDescription = signal<string>('');
  readonly formDayOfWeek = signal<string>('ALL');
  readonly formDaysOfWeek = signal<string[]>(['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES']);
  readonly formPriceMode = signal<PriceMode>('INDIVIDUAL_ITEMS');
  readonly formPackagePrice = signal<number | null>(25);
  readonly formItems = signal<CreateDailyMenuItemRequest[]>([]);
  readonly formComboIds = signal<string[]>([]);

  // Campos para agregar nuevo ítem al formulario
  readonly selectedDishId = signal<string>('');
  readonly selectedCourseType = signal<CourseType>('MAIN');
  readonly selectedSuggestedQuota = signal<number>(20);
  readonly selectedOverridePrice = signal<number | null>(null);
  readonly selectedAllowIndividualSale = signal<boolean>(true);
  readonly selectedIsUnlimited = signal<boolean>(false);

  // Modal Clonar / Eliminar Plantilla
  readonly menuPendingClone = signal<DailyMenuDto | null>(null);
  readonly cloneNewName = signal<string>('');
  readonly cloneTargetBranchId = signal<string>('');
  readonly menuPendingDelete = signal<DailyMenuDto | null>(null);

  // Modal Configurar Jornada de Sucursal
  readonly showSessionModal = signal<boolean>(false);
  readonly sessionMenuMode = signal<ServiceSessionMenuMode>('FULL_CATALOG');
  readonly sessionDailyMenuId = signal<string>('');
  readonly sessionNotes = signal<string>('');
  readonly sessionCustomQuotas = signal<Record<string, number>>({});
  readonly sessionUnlimitedDishIds = signal<string[]>([]);

  readonly weekDays = [
    { value: 'LUNES', short: 'Lun' },
    { value: 'MARTES', short: 'Mar' },
    { value: 'MIERCOLES', short: 'Mié' },
    { value: 'JUEVES', short: 'Jue' },
    { value: 'VIERNES', short: 'Vie' },
    { value: 'SABADO', short: 'Sáb' },
    { value: 'DOMINGO', short: 'Dom' },
  ];

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

  readonly courseTypesList: { value: CourseType; label: string }[] = [
    { value: 'STARTER', label: 'Entrada / Sopa' },
    { value: 'MAIN', label: 'Segundo / Plato Fuerte' },
    { value: 'DESSERT', label: 'Postre' },
    { value: 'BEVERAGE', label: 'Bebida' },
    { value: 'GENERAL', label: 'General' },
  ];

  readonly filteredMenus = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const day = this.selectedDayFilter();
    return this.menus().filter((m) => {
      const matchesDay = day === 'ALL' || (m.dayOfWeek && m.dayOfWeek.includes(day)) || m.dayOfWeek === 'ALL' || !m.dayOfWeek;
      const matchesQuery =
        !query ||
        m.name.toLowerCase().includes(query) ||
        (m.description && m.description.toLowerCase().includes(query));
      return matchesDay && matchesQuery;
    });
  });

  readonly selectedSessionMenuTemplate = computed(() => {
    const id = this.sessionDailyMenuId();
    return this.menus().find((m) => m.id === id) || null;
  });

  constructor() {
    effect(() => {
      const branchId = this.activeBranchId();
      if (this.lastLoadedBranchId !== undefined && branchId !== this.lastLoadedBranchId) {
        this.lastLoadedBranchId = branchId;
        this.loadData();
      }
    });
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    const branchId = this.activeBranchId() || undefined;
    this.lastLoadedBranchId = branchId ?? null;
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
        const available = (dishList || []).filter((d) => d.isAvailable);
        this.dishes.set(available);
        if (available.length > 0 && !this.selectedDishId()) {
          this.selectedDishId.set(available[0].dishId);
        }
      },
    });

    this.catalogService.getCombos(branchId).subscribe({
      next: (comboList) => this.combos.set(comboList || []),
      error: () => this.combos.set([]),
    });

    this.catalogService.getCurrentServiceSession(branchId).subscribe({
      next: (session) => this.currentSession.set(session),
      error: () => this.currentSession.set(null),
    });
  }

  toggleExpand(menuId: string): void {
    this.expandedMenuId.update((curr) => (curr === menuId ? null : menuId));
  }

  // ---------------------------------------------------------------------------
  // Crear / Editar Plantilla de Menú
  // ---------------------------------------------------------------------------
  openCreateModal(): void {
    this.editingMenuId.set(null);
    this.formName.set('');
    this.formDescription.set('');
    this.formDaysOfWeek.set(['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES']);
    this.formDayOfWeek.set('LUNES,MARTES,MIERCOLES,JUEVES,VIERNES');
    this.formPriceMode.set('INDIVIDUAL_ITEMS');
    this.formPackagePrice.set(25);
    this.formItems.set([]);
    this.formComboIds.set([]);
    if (this.dishes().length > 0) {
      this.selectedDishId.set(this.dishes()[0].dishId);
    }
    this.selectedCourseType.set('MAIN');
    this.selectedSuggestedQuota.set(20);
    this.selectedOverridePrice.set(null);
    this.selectedAllowIndividualSale.set(true);
    this.selectedIsUnlimited.set(false);
    this.errorMessage.set(null);
    this.showCreateModal.set(true);
  }

  openEditModal(menu: DailyMenuDto): void {
    this.editingMenuId.set(menu.id);
    this.formName.set(menu.name);
    this.formDescription.set(menu.description || '');
    const days =
      menu.dayOfWeek && menu.dayOfWeek !== 'ALL'
        ? menu.dayOfWeek.split(',').map((d) => d.trim()).filter(Boolean)
        : [];
    this.formDaysOfWeek.set(days);
    this.formDayOfWeek.set(menu.dayOfWeek || 'ALL');
    this.formPriceMode.set(menu.priceMode || 'INDIVIDUAL_ITEMS');
    this.formPackagePrice.set(menu.packagePrice ?? 25);
    this.formItems.set(
      (menu.items || []).map((it, idx) => ({
        dishId: it.dishId,
        courseType: it.courseType,
        suggestedQuota: it.suggestedQuota,
        overridePrice: it.overridePrice ?? null,
        isMandatoryInPackage: it.isMandatoryInPackage ?? false,
        sortOrder: it.sortOrder ?? idx + 1,
        allowIndividualSale: it.allowIndividualSale ?? true,
        isUnlimited: it.isUnlimited ?? false,
      }))
    );
    this.formComboIds.set([...(menu.comboIds || [])]);
    if (this.dishes().length > 0) {
      this.selectedDishId.set(this.dishes()[0].dishId);
    }
    this.selectedCourseType.set('MAIN');
    this.selectedSuggestedQuota.set(20);
    this.selectedOverridePrice.set(null);
    this.selectedAllowIndividualSale.set(true);
    this.selectedIsUnlimited.set(false);
    this.errorMessage.set(null);
    this.showCreateModal.set(true);
  }

  toggleDayOfWeek(dayValue: string): void {
    this.formDaysOfWeek.update((current) =>
      current.includes(dayValue) ? current.filter((d) => d !== dayValue) : [...current, dayValue]
    );
    const days = this.formDaysOfWeek();
    this.formDayOfWeek.set(days.length === 0 ? 'ALL' : days.join(','));
  }

  isDaySelected(dayValue: string): boolean {
    return this.formDaysOfWeek().includes(dayValue);
  }

  toggleComboSelection(comboId: string): void {
    this.formComboIds.update((current) =>
      current.includes(comboId) ? current.filter((id) => id !== comboId) : [...current, comboId]
    );
  }

  isComboSelected(comboId: string): boolean {
    return this.formComboIds().includes(comboId);
  }

  closeCreateModal(): void {
    this.editingMenuId.set(null);
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

    const unlimited = this.selectedIsUnlimited();
    const override = this.selectedOverridePrice();
    const newItem: CreateDailyMenuItemRequest = {
      dishId,
      courseType: this.selectedCourseType(),
      suggestedQuota: unlimited ? 0 : Math.max(1, Number(this.selectedSuggestedQuota()) || 1),
      overridePrice: override != null && Number(override) > 0 ? Number(override) : null,
      isMandatoryInPackage: false,
      sortOrder: current.length + 1,
      allowIndividualSale: this.selectedAllowIndividualSale(),
      isUnlimited: unlimited,
    };

    this.formItems.update((list) => [...list, newItem]);
    this.errorMessage.set(null);
  }

  toggleFormItemIndividualSale(index: number): void {
    this.formItems.update((list) =>
      list.map((it, i) => (i === index ? { ...it, allowIndividualSale: !(it.allowIndividualSale ?? true) } : it))
    );
  }

  toggleFormItemUnlimited(index: number): void {
    this.formItems.update((list) =>
      list.map((it, i) =>
        i === index ? { ...it, isUnlimited: !it.isUnlimited, suggestedQuota: !it.isUnlimited ? 0 : Math.max(1, it.suggestedQuota || 20) } : it
      )
    );
  }

  removeItemFromForm(index: number): void {
    this.formItems.update((list) => list.filter((_, i) => i !== index));
  }

  getDishName(dishId: string): string {
    const d = this.dishes().find((dish) => dish.dishId === dishId);
    return d ? `${d.code} - ${d.name}` : dishId;
  }

  getComboName(comboId: string): string {
    return this.combos().find((c) => c.id === comboId)?.name || comboId;
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

    const priceMode = this.formPriceMode();
    if (priceMode === 'FIXED_PACKAGE' && (this.formPackagePrice() == null || Number(this.formPackagePrice()) <= 0)) {
      this.errorMessage.set('El precio paquete debe ser mayor a cero para la modalidad Paquete Fijo.');
      return;
    }

    const branchId = this.activeBranchId();

    const request: CreateDailyMenuRequest = {
      branchId,
      name,
      description: this.formDescription().trim() || null,
      dayOfWeek: this.formDayOfWeek() || 'ALL',
      priceMode,
      packagePrice: priceMode === 'FIXED_PACKAGE' ? Number(this.formPackagePrice()) : null,
      isTemplate: true,
      items,
      comboIds: this.formComboIds(),
    };

    this.isSaving.set(true);
    this.errorMessage.set(null);

    const editId = this.editingMenuId();
    const obs$ = editId ? this.catalogService.updateDailyMenu(editId, request) : this.catalogService.createDailyMenu(request);
    const verb = editId ? 'actualizada' : 'creada';

    obs$.subscribe({
      next: (saved) => {
        this.isSaving.set(false);
        this.closeCreateModal();
        this.successMessage.set(`Plantilla de menú "${saved.name}" ${verb} exitosamente.`);
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

  // ---------------------------------------------------------------------------
  // Clonar / Eliminar Plantilla
  // ---------------------------------------------------------------------------
  openCloneModal(menu: DailyMenuDto): void {
    this.menuPendingClone.set(menu);
    this.cloneNewName.set(`${menu.name} (Copia)`);
    this.cloneTargetBranchId.set(this.activeBranchId() || '');
  }

  closeCloneModal(): void {
    this.menuPendingClone.set(null);
  }

  confirmCloneMenu(): void {
    const menu = this.menuPendingClone();
    if (!menu) return;
    this.isSaving.set(true);
    this.catalogService
      .cloneDailyMenu(menu.id, {
        targetBranchId: this.cloneTargetBranchId() || this.activeBranchId() || null,
        newName: this.cloneNewName().trim() || `${menu.name} (Copia)`,
      })
      .subscribe({
        next: (cloned) => {
          this.isSaving.set(false);
          this.menuPendingClone.set(null);
          this.successMessage.set(`Plantilla "${cloned.name}" clonada exitosamente.`);
          this.loadData();
        },
        error: (err) => {
          this.isSaving.set(false);
          this.errorMessage.set(err.error?.detail || 'No se pudo clonar la plantilla.');
        },
      });
  }

  requestDeleteMenu(menu: DailyMenuDto): void {
    this.menuPendingDelete.set(menu);
  }

  cancelDeleteMenu(): void {
    this.menuPendingDelete.set(null);
  }

  confirmDeleteMenu(): void {
    const menu = this.menuPendingDelete();
    if (!menu) return;
    this.isSaving.set(true);
    this.catalogService.deleteDailyMenu(menu.id).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.menuPendingDelete.set(null);
        this.successMessage.set(`Plantilla "${menu.name}" eliminada exitosamente.`);
        this.loadData();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.menuPendingDelete.set(null);
        this.errorMessage.set(err.error?.detail || 'No se pudo eliminar la plantilla.');
      },
    });
  }

  // ---------------------------------------------------------------------------
  // Configurar Jornada Operativa de Sucursal (BranchServiceSession)
  // ---------------------------------------------------------------------------
  openSessionModal(): void {
    const curr = this.currentSession();
    const firstMenuId = this.menus()[0]?.id || '';
    if (curr && curr.status !== 'CLOSED') {
      const mode: ServiceSessionMenuMode = curr.menuMode === 'DAILY_MENU' ? 'DAILY_MENU' : 'FULL_CATALOG';
      this.sessionMenuMode.set(mode);
      this.sessionDailyMenuId.set(curr.dailyMenuId || firstMenuId);
      this.sessionNotes.set(curr.notes || '');
      const quotas: Record<string, number> = {};
      const unlimited: string[] = [];
      (curr.shiftMenu?.items || []).forEach((it) => {
        quotas[it.dishId] = it.initialQuota;
        if (it.isUnlimited) unlimited.push(it.dishId);
      });
      this.sessionCustomQuotas.set(quotas);
      this.sessionUnlimitedDishIds.set(unlimited);
    } else {
      this.sessionMenuMode.set(firstMenuId ? 'DAILY_MENU' : 'FULL_CATALOG');
      this.sessionDailyMenuId.set(firstMenuId);
      this.sessionNotes.set('');
      this.syncSessionDefaultsFromTemplate(firstMenuId);
    }
    this.showSessionModal.set(true);
  }

  closeSessionModal(): void {
    this.showSessionModal.set(false);
  }

  onSessionDailyMenuChange(menuId: string): void {
    this.sessionDailyMenuId.set(menuId);
    this.syncSessionDefaultsFromTemplate(menuId);
  }

  private syncSessionDefaultsFromTemplate(menuId: string): void {
    const tpl = this.menus().find((m) => m.id === menuId);
    const quotas: Record<string, number> = {};
    const unlimited: string[] = [];
    (tpl?.items || []).forEach((it) => {
      quotas[it.dishId] = it.suggestedQuota;
      if (it.isUnlimited) unlimited.push(it.dishId);
    });
    this.sessionCustomQuotas.set(quotas);
    this.sessionUnlimitedDishIds.set(unlimited);
  }

  updateSessionDishQuota(dishId: string, quota: number): void {
    this.sessionCustomQuotas.update((map) => ({ ...map, [dishId]: Math.max(0, Number(quota) || 0) }));
  }

  isSessionDishUnlimited(dishId: string): boolean {
    return this.sessionUnlimitedDishIds().includes(dishId);
  }

  toggleSessionDishUnlimited(dishId: string): void {
    this.sessionUnlimitedDishIds.update((list) =>
      list.includes(dishId) ? list.filter((id) => id !== dishId) : [...list, dishId]
    );
  }

  saveServiceSession(): void {
    const branchId = this.activeBranchId();
    if (!branchId) {
      this.errorMessage.set('Debe haber una sucursal activa para configurar la jornada.');
      return;
    }
    if (this.sessionMenuMode() === 'DAILY_MENU' && !this.sessionDailyMenuId()) {
      this.errorMessage.set('Seleccione una plantilla de Menú del Día para la jornada.');
      return;
    }

    const req: ConfigureServiceSessionRequest = {
      branchId,
      menuMode: this.sessionMenuMode(),
      dailyMenuId: this.sessionMenuMode() === 'DAILY_MENU' ? this.sessionDailyMenuId() : null,
      customQuotas: this.sessionMenuMode() === 'DAILY_MENU' ? this.sessionCustomQuotas() : {},
      unlimitedDishIds: this.sessionMenuMode() === 'DAILY_MENU' ? this.sessionUnlimitedDishIds() : [],
      notes: this.sessionNotes().trim() || null,
    };

    this.isSaving.set(true);
    this.catalogService.configureServiceSession(req).subscribe({
      next: (saved) => {
        this.isSaving.set(false);
        this.currentSession.set(saved);
        this.showSessionModal.set(false);
        this.successMessage.set(
          saved.menuMode === 'FULL_CATALOG'
            ? 'Jornada de hoy configurada con Carta Completa.'
            : `Jornada de hoy configurada con el menú "${saved.dailyMenuName}".`
        );
      },
      error: (err) => {
        this.isSaving.set(false);
        this.errorMessage.set(err.error?.detail || 'No se pudo configurar la jornada de la sucursal.');
      },
    });
  }

  closeCurrentServiceSession(): void {
    const session = this.currentSession();
    if (!session) return;
    this.isSaving.set(true);
    this.catalogService.closeServiceSession(session.id).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.currentSession.set(null);
        this.successMessage.set('Jornada operativa cerrada exitosamente.');
      },
      error: (err) => {
        this.isSaving.set(false);
        this.errorMessage.set(err.error?.detail || 'No se pudo cerrar la jornada operativa.');
      },
    });
  }

  getCourseLabel(course: CourseType | string): string {
    return this.courseTypesList.find((c) => c.value === course)?.label || course;
  }

  getCourseBadgeColor(course: CourseType | string): UiBadgeColor {
    switch (course) {
      case 'STARTER':
        return 'primary';
      case 'MAIN':
        return 'warning';
      case 'DESSERT':
        return 'secondary';
      case 'BEVERAGE':
        return 'success';
      default:
        return 'neutral';
    }
  }
}
