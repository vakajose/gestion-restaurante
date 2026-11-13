import { describe, expect, it } from 'vitest';
import { getApiBaseUrl } from '../config/runtimeEnv';
import { formatBytes, formatCalendarDate, formatCurrency } from './formatters';

describe('formatters & runtimeEnv', () => {
  it('formatea fechas calendario puras YYYY-MM-DD a dd/MM/yyyy sin desplazamientos horarios (ADR-0006)', () => {
    expect(formatCalendarDate('2026-11-12')).toBe('12/11/2026');
    expect(formatCalendarDate('2026-01-05')).toBe('05/01/2026');
    expect(formatCalendarDate(null)).toBe('—');
  });

  it('formatea bytes y montos monetarios con valores nulos seguros', () => {
    expect(formatBytes(524288)).toBe('512.0 KB');
    expect(formatBytes(1048576)).toBe('1.00 MB');
    expect(formatBytes(null)).toBe('—');

    const formatted = formatCurrency(150.5, 'BOB');
    expect(formatted).toContain('BOB');
    expect(formatted).toContain('150');
  });

  it('prioriza window.__ENV__.API_BASE_URL inyectado en runtime y remueve slashes finales', () => {
    window.__ENV__ = { API_BASE_URL: 'https://api.staging.restaurante.bo///' };
    expect(getApiBaseUrl()).toBe('https://api.staging.restaurante.bo');

    window.__ENV__ = { API_BASE_URL: '' };
    expect(getApiBaseUrl()).toBe('');
  });
});
