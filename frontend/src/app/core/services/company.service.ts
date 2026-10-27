import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TenantDto,
  BranchDto,
  UpdateCompanyRequest,
  CreateBranchRequest,
  UpdateBranchRequest,
} from '../models/company.models';

@Injectable({
  providedIn: 'root',
})
export class CompanyService {
  private readonly http = inject(HttpClient);

  getCompany(): Observable<TenantDto> {
    return this.http.get<TenantDto>('/api/v1/company');
  }

  updateCompany(request: UpdateCompanyRequest): Observable<TenantDto> {
    return this.http.put<TenantDto>('/api/v1/company', request);
  }

  getBranches(): Observable<BranchDto[]> {
    return this.http.get<BranchDto[]>('/api/v1/branches');
  }

  getBranchById(id: string): Observable<BranchDto> {
    return this.http.get<BranchDto>(`/api/v1/branches/${id}`);
  }

  createBranch(request: CreateBranchRequest): Observable<BranchDto> {
    return this.http.post<BranchDto>('/api/v1/branches', request);
  }

  updateBranch(id: string, request: UpdateBranchRequest): Observable<BranchDto> {
    return this.http.put<BranchDto>(`/api/v1/branches/${id}`, request);
  }

  toggleBranchStatus(id: string, isActive: boolean): Observable<BranchDto> {
    return this.http.patch<BranchDto>(`/api/v1/branches/${id}/status`, { isActive });
  }
}
