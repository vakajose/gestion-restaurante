import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HasPrivilegeDirective } from './has-privilege.directive';
import { AuthService } from '../services/auth.service';
import { UserDto } from '../models/auth.models';

@Component({
  standalone: true,
  imports: [HasPrivilegeDirective],
  template: `
    <div *hasPrivilege="'pos:discount_apply'" data-testid="single-priv-box">
      Descuento Autorizado
    </div>
    <div *appHasPrivilege="['users:write', 'roles:manage']" data-testid="multi-priv-box">
      Gestión Seguridad
    </div>
  `,
})
class TestHostComponent {}

describe('HasPrivilegeDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let authService: AuthService;

  const baseCashier: UserDto = {
    id: 'u-1',
    username: 'cajero1',
    email: null,
    role: 'CASHIER',
    tenantId: 't-1',
    branchId: 'b-1',
    permissions: ['pos:access'],
  };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    authService = TestBed.inject(AuthService);
    fixture = TestBed.createComponent(TestHostComponent);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('debe ocultar los elementos si el usuario no posee el privilegio requerido', () => {
    authService.currentUser.set(baseCashier);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="single-priv-box"]')).toBeFalsy();
    expect(fixture.nativeElement.querySelector('[data-testid="multi-priv-box"]')).toBeFalsy();
  });

  it('debe mostrar dinámicamente el elemento cuando el usuario adquiere el privilegio específico', () => {
    authService.currentUser.set({
      ...baseCashier,
      permissions: ['pos:access', 'pos:discount_apply'],
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="single-priv-box"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="multi-priv-box"]')).toBeFalsy();
  });

  it('debe mostrar todos los elementos si el usuario tiene el comodín "*" o rol ADMIN_TENANT', () => {
    authService.currentUser.set({
      ...baseCashier,
      permissions: ['*'],
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="single-priv-box"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="multi-priv-box"]')).toBeTruthy();
  });
});
