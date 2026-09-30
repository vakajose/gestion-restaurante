import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

/**
 * En Angular 22, la detección de cambios sin Zone.js es estable mediante provideZonelessChangeDetection().
 * Se exporta provideExperimentalZonelessChangeDetection como alias para compatibilidad y completitud de spec.
 */
export const provideExperimentalZonelessChangeDetection = provideZonelessChangeDetection;

export const appConfig: ApplicationConfig = {
  providers: [
    provideExperimentalZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
