/**
 * Utilidad nativa TypeScript para exportación de datos a formato CSV compatible con Microsoft Excel y LibreOffice.
 * Cumple con la Regla 9/10 de Gobernanza (Cero dependencias npm externas / < 3 KB).
 * Incluye prefijo BOM UTF-8 (\uFEFF) para visualización correcta de caracteres especiales, tildes y eñes.
 */
export function exportToCsv(
  filename: string,
  headers: string[],
  rows: (string | number | undefined | null)[][],
): void {
  const BOM = '\uFEFF';
  const headerLine = headers
    .map((h) => {
      const str = String(h ?? '').replace(/"/g, '""');
      return `"${str}"`;
    })
    .join(';');

  const csvContent = rows
    .map((row) =>
      row
        .map((val) => {
          if (val === null || val === undefined) return '""';
          const str = String(val).replace(/"/g, '""');
          return `"${str}"`;
        })
        .join(';'),
    )
    .join('\r\n');

  const fullContent = BOM + headerLine + (rows.length > 0 ? '\r\n' + csvContent : '');
  const blob = new Blob([fullContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename.endsWith('.csv') ? filename : `${filename}.csv`;
  link.click();
  URL.revokeObjectURL(link.href);
}
