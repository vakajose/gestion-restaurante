import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CheckAvailabilityResultDto,
  OnboardingRequest,
  OnboardingResultDto,
} from '../models/company.models';

@Injectable({
  providedIn: 'root',
})
export class OnboardingService {
  private readonly http = inject(HttpClient);

  /**
   * Verifica la disponibilidad de un slug empresarial en tiempo real.
   */
  checkSlugAvailability(slug: string): Observable<CheckAvailabilityResultDto> {
    const params = new HttpParams().set('slug', slug.trim());
    return this.http.get<CheckAvailabilityResultDto>('/api/v1/auth/check-slug', { params });
  }

  /**
   * Verifica disponibilidad general de slug y/o NIT en onboarding.
   */
  checkAvailability(params: { slug?: string; nit?: string }): Observable<CheckAvailabilityResultDto> {
    let httpParams = new HttpParams();
    if (params.slug && params.slug.trim().length > 0) {
      httpParams = httpParams.set('slug', params.slug.trim());
    }
    if (params.nit && params.nit.trim().length > 0) {
      httpParams = httpParams.set('nit', params.nit.trim());
    }
    return this.http.get<CheckAvailabilityResultDto>('/api/v1/auth/onboarding/check-availability', {
      params: httpParams,
    });
  }

  /**
   * Ejecuta el aprovisionamiento atómico de empresa, sucursal, cuenta admin y plantilla operativa.
   */
  registerOnboarding(request: OnboardingRequest): Observable<OnboardingResultDto> {
    return this.http.post<OnboardingResultDto>('/api/v1/auth/onboarding', request);
  }
}
