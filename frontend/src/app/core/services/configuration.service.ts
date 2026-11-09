import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, tap, catchError, of, map } from 'rxjs';
import {
  ParameterCategory,
  ParameterScope,
  SystemParameterDto,
  UpdateParameterRequest,
} from '../models/configuration.model';

@Injectable({
  providedIn: 'root',
})
export class ConfigurationService {
  private readonly http = inject(HttpClient);

  // Estados Reactivos Principales
  readonly parameters = signal<SystemParameterDto[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly selectedScope = signal<ParameterScope>('BRANCH');
  readonly selectedCategory = signal<ParameterCategory | 'ALL'>('ALL');
  readonly searchQuery = signal<string>('');

  // Caché de ETags para HTTP 304 Not Modified
  private readonly etagCache = new Map<string, { etag: string; data: SystemParameterDto[] }>();

  // Parámetros filtrados reactivamente por categoría y búsqueda
  readonly filteredParameters = computed(() => {
    const list = this.parameters();
    const cat = this.selectedCategory();
    const query = this.searchQuery().trim().toLowerCase();

    return list.filter((param) => {
      // Filtro por categoría
      if (cat !== 'ALL' && param.category !== cat) {
        return false;
      }

      // Filtro por texto de búsqueda predictiva multicriterio
      if (query.length > 0) {
        const matchKey = param.key.toLowerCase().includes(query);
        const matchName = param.name.toLowerCase().includes(query);
        const matchDesc = param.description.toLowerCase().includes(query);
        const matchOptions = param.options?.some(
          (opt) =>
            opt.label.toLowerCase().includes(query) ||
            (opt.description && opt.description.toLowerCase().includes(query)) ||
            opt.value.toLowerCase().includes(query)
        ) ?? false;
        const matchHelp =
          (param.help?.impact && param.help.impact.toLowerCase().includes(query)) ||
          (param.help?.example && param.help.example.toLowerCase().includes(query)) ||
          false;

        if (!matchKey && !matchName && !matchDesc && !matchOptions && !matchHelp) {
          return false;
        }
      }

      return true;
    });
  });

  /**
   * Carga los parámetros del backend con soporte de cabeceras ETag y carga diferida por categoría.
   */
  loadParameters(
    scope?: ParameterScope,
    category?: ParameterCategory | null,
    branchId?: string | null
  ): Observable<SystemParameterDto[]> {
    const effectiveScope = scope || this.selectedScope();
    let params = new HttpParams().set('scope', effectiveScope);

    if (category) {
      params = params.set('category', category);
    }
    if (branchId) {
      params = params.set('branchId', branchId);
    }

    const cacheKey = `${effectiveScope}:${category || 'ALL'}:${branchId || 'NONE'}`;
    const cachedEntry = this.etagCache.get(cacheKey);

    let headers = new HttpHeaders();
    if (cachedEntry?.etag) {
      headers = headers.set('If-None-Match', cachedEntry.etag);
    }

    this.isLoading.set(true);

    return this.http
      .get<SystemParameterDto[]>('/api/v1/parameters', {
        params,
        headers,
        observe: 'response',
      })
      .pipe(
        map((response: HttpResponse<SystemParameterDto[]>) => {
          this.isLoading.set(false);
          if (response.status === 200 && response.body) {
            const etag = response.headers.get('ETag');
            if (etag) {
              this.etagCache.set(cacheKey, { etag, data: response.body });
            }
            this.parameters.set(response.body);
            return response.body;
          }
          return this.parameters();
        }),
        catchError((err) => {
          this.isLoading.set(false);
          // Si el servidor retorna 304 Not Modified, reutilizar datos en caché
          if (err.status === 304 && cachedEntry) {
            this.parameters.set(cachedEntry.data);
            return of(cachedEntry.data);
          }
          throw err;
        })
      );
  }

  /**
   * Mutación atómica PATCH sobre un parámetro individual con actualización optimista.
   */
  updateParameter(
    key: string,
    request: UpdateParameterRequest
  ): Observable<SystemParameterDto> {
    return this.http
      .patch<SystemParameterDto>(`/api/v1/parameters/${encodeURIComponent(key)}`, request)
      .pipe(
        tap((updated) => {
          // Actualización reactiva instantánea en la lista en memoria (0 ms)
          this.parameters.update((list) =>
            list.map((item) => (item.key === updated.key ? updated : item))
          );
          this.etagCache.clear();
        })
      );
  }

  /**
   * Restablece el parámetro al valor corporativo heredado (DELETE /api/v1/parameters/{key}).
   */
  resetParameter(
    key: string,
    scope?: ParameterScope,
    branchId?: string | null
  ): Observable<void> {
    let params = new HttpParams();
    if (scope) {
      params = params.set('scope', scope);
    }
    if (branchId) {
      params = params.set('branchId', branchId);
    }

    return this.http
      .delete<void>(`/api/v1/parameters/${encodeURIComponent(key)}`, { params })
      .pipe(
        tap(() => {
          this.etagCache.clear();
          // Cargar nuevamente para reflejar el valor heredado
          this.loadParameters(scope, null, branchId).subscribe();
        })
      );
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
  }

  setSelectedCategory(category: ParameterCategory | 'ALL'): void {
    this.selectedCategory.set(category);
  }

  setSelectedScope(scope: ParameterScope): void {
    this.selectedScope.set(scope);
  }
}
