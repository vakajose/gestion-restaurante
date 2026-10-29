import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { UserService } from './user.service';
import {
  RoleDto,
  PermissionCatalogItemDto,
  CreateRoleRequest,
  UpdateRoleRequest,
} from '../models/user.models';

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  private readonly userService = inject(UserService);

  getRoles(): Observable<RoleDto[]> {
    return this.userService.getRoles();
  }

  getPermissionsCatalog(): Observable<PermissionCatalogItemDto[]> {
    return this.userService.getPermissionsCatalog();
  }

  createRole(request: CreateRoleRequest): Observable<RoleDto> {
    return this.userService.createRole(request);
  }

  updateRole(id: string, request: UpdateRoleRequest): Observable<RoleDto> {
    return this.userService.updateRole(id, request);
  }

  deleteRole(id: string): Observable<void> {
    return this.userService.deleteRole(id);
  }
}
