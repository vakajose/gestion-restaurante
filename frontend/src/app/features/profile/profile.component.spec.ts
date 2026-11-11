import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { of } from 'rxjs';
import { ProfileComponent } from './profile.component';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { BranchContextService } from '../../core/services/branch-context.service';
import { UserDto } from '../../core/models/auth.models';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let userServiceSpy: {
    getProfile: ReturnType<typeof vi.fn>;
    updateProfile: ReturnType<typeof vi.fn>;
    changePassword: ReturnType<typeof vi.fn>;
    updatePin: ReturnType<typeof vi.fn>;
  };
  let currentUserSignal: WritableSignal<UserDto | null>;
  let authServiceSpy: { currentUser: WritableSignal<UserDto | null> };
  let branchContextServiceSpy: { activeBranch: ReturnType<typeof vi.fn> };

  const mockUser: UserDto = {
    id: 'u-1',
    username: 'admin',
    fullName: 'Carlos Administrador',
    phoneNumber: '+591 70012345',
    email: 'admin@restaurant.com',
    role: 'ADMIN_TENANT',
    tenantId: 't-1',
    branchId: 'b-1',
    tenantSlug: 'demo-restaurant',
    permissions: ['users:read', 'users:write'],
  };

  beforeEach(async () => {
    currentUserSignal = signal<UserDto | null>(mockUser);
    userServiceSpy = {
      getProfile: vi.fn().mockReturnValue(of(mockUser)),
      updateProfile: vi.fn(),
      changePassword: vi.fn(),
      updatePin: vi.fn(),
    };
    authServiceSpy = {
      currentUser: currentUserSignal,
    };
    branchContextServiceSpy = {
      activeBranch: vi.fn().mockReturnValue({ id: 'b-1', name: 'Sucursal Central' }),
    };

    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: BranchContextService, useValue: branchContextServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe inicializarse y cargar los datos del perfil', () => {
    expect(component).toBeTruthy();
    expect(component.user()?.username).toBe('admin');
    expect(component.profileForm.get('fullName')?.value).toBe('Carlos Administrador');
    expect(component.profileForm.get('phoneNumber')?.value).toBe('+591 70012345');
    expect(component.profileForm.get('email')?.value).toBe('admin@restaurant.com');
  });

  it('debe actualizar los datos de contacto del perfil', () => {
    const updated: UserDto = {
      ...mockUser,
      fullName: 'Carlos Gomez',
      phoneNumber: '71122334',
      email: 'nuevo_correo@restaurant.com',
    };
    userServiceSpy.updateProfile.mockReturnValue(of(updated));

    component.profileForm.patchValue({
      fullName: 'Carlos Gomez',
      phoneNumber: '71122334',
      email: 'nuevo_correo@restaurant.com',
    });
    component.saveProfile();

    expect(userServiceSpy.updateProfile).toHaveBeenCalledWith({
      fullName: 'Carlos Gomez',
      phoneNumber: '71122334',
      email: 'nuevo_correo@restaurant.com',
    });
    expect(component.user()?.email).toBe('nuevo_correo@restaurant.com');
    expect(component.user()?.fullName).toBe('Carlos Gomez');
    expect(component.user()?.phoneNumber).toBe('71122334');
    expect(authServiceSpy.currentUser()).toEqual(updated);
  });

  it('debe validar coincidencia de contraseñas antes de invocar changePassword', () => {
    component.passwordForm.patchValue({
      currentPassword: 'actualPassword123',
      newPassword: 'newPassword123',
      confirmPassword: 'differentPassword123',
    });

    component.changePassword();

    expect(userServiceSpy.changePassword).not.toHaveBeenCalled();
    expect(component.passwordMessage()?.type).toBe('error');
    expect(component.passwordMessage()?.text).toContain('no coinciden');
  });

  it('debe invocar changePassword si las contraseñas coinciden', () => {
    userServiceSpy.changePassword.mockReturnValue(of({ message: 'Contraseña actualizada con éxito.' }));

    component.passwordForm.patchValue({
      currentPassword: 'actualPassword123',
      newPassword: 'newPassword123',
      confirmPassword: 'newPassword123',
    });

    component.changePassword();

    expect(userServiceSpy.changePassword).toHaveBeenCalledWith({
      currentPassword: 'actualPassword123',
      newPassword: 'newPassword123',
    });
    expect(component.passwordMessage()?.type).toBe('success');
  });

  it('debe validar coincidencia de PIN antes de invocar updateSupervisorPin', () => {
    component.pinForm.patchValue({
      pin: '1234',
      confirmPin: '9999',
    });

    component.updateSupervisorPin();

    expect(userServiceSpy.updatePin).not.toHaveBeenCalled();
    expect(component.pinMessage()?.type).toBe('error');
    expect(component.pinMessage()?.text).toContain('no coinciden');
  });

  it('debe invocar userService.updatePin al confirmar PIN válido', () => {
    userServiceSpy.updatePin.mockReturnValue(of({ message: 'PIN actualizado con éxito.' }));

    component.pinForm.patchValue({
      pin: '4321',
      confirmPin: '4321',
    });

    component.updateSupervisorPin();

    expect(userServiceSpy.updatePin).toHaveBeenCalledWith({ pin: '4321' });
    expect(component.pinMessage()?.type).toBe('success');
  });
});
