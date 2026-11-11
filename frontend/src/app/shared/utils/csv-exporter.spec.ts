import { describe, it, expect, beforeEach, vi } from 'vitest';
import { exportToCsv } from './csv-exporter';

describe('exportToCsv', () => {
  let createdBlob: Blob | null = null;
  let clickedLink: { href: string; download: string; click: () => void } | null = null;

  beforeEach(() => {
    createdBlob = null;
    clickedLink = null;

    // Spy on URL methods
    URL.createObjectURL = vi.fn().mockImplementation((blob: Blob) => {
      createdBlob = blob;
      return 'blob:mock-csv-url';
    });
    URL.revokeObjectURL = vi.fn();

    // Spy on document.createElement('a')
    const origCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
      if (tagName === 'a') {
        const link = {
          href: '',
          download: '',
          click: vi.fn(),
        };
        clickedLink = link;
        return link as unknown as HTMLElement;
      }
      return origCreateElement(tagName);
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('debe generar un blob CSV con BOM UTF-8 y disparar la descarga', async () => {
    const headers = ['ID', 'Cajero', 'Monto'];
    const rows = [
      ['101', 'Juan Pérez', 150.5],
      ['102', 'María "Mafe"', null],
    ];

    exportToCsv('reporte_turnos', headers, rows);

    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(clickedLink).toBeTruthy();
    expect(clickedLink?.download).toBe('reporte_turnos.csv');
    expect(clickedLink?.click).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-csv-url');

    expect(createdBlob).toBeTruthy();
    const buffer = await createdBlob!.arrayBuffer();
    const bytes = new Uint8Array(buffer);
    // Debe incluir bytes BOM UTF-8 (0xEF, 0xBB, 0xBF)
    expect(bytes[0]).toBe(0xef);
    expect(bytes[1]).toBe(0xbb);
    expect(bytes[2]).toBe(0xbf);

    const text = await createdBlob!.text();
    // Debe incluir cabeceras separadas por ;
    expect(text).toContain('"ID";"Cajero";"Monto"');
    // Debe escapar comillas internas
    expect(text).toContain('"María ""Mafe"""');
    // Debe serializar null como ""
    expect(text).toContain(';""');
  });

  it('debe mantener la extensión si el nombre ya incluye .csv', () => {
    exportToCsv('detalle.csv', ['Col1'], [['Val1']]);
    expect(clickedLink?.download).toBe('detalle.csv');
  });
});
