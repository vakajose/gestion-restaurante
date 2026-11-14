import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Badge } from './Badge';
import { Card } from './Card';
import { EmptyState } from './EmptyState';
import { Modal } from './Modal';

describe('Backoffice shared UI primitives (TASK-620)', () => {
  it('Modal renderiza título y dispara onClose al hacer clic en cerrar o pulsar Escape', async () => {
    const onClose = vi.fn();
    render(
      <Modal open onClose={onClose} title="Título de Prueba" closeAriaLabel="Cerrar diálogo">
        <p>Contenido modal</p>
      </Modal>,
    );

    expect(screen.getByText('Título de Prueba')).toBeTruthy();
    expect(screen.getByText('Contenido modal')).toBeTruthy();

    const user = userEvent.setup();
    await user.keyboard('{Escape}');
    await user.click(screen.getByRole('button', { name: 'Cerrar diálogo' }));

    expect(onClose).toHaveBeenCalledTimes(2);
  });

  it('Card, EmptyState y Badge renderizan variantes esperadas', () => {
    render(
      <Card padding="sm">
        <Badge tone="warning">Temporal</Badge>
        <EmptyState message="Sin resultados" />
      </Card>,
    );

    expect(screen.getByText('Temporal')).toBeTruthy();
    expect(screen.getByText('Sin resultados')).toBeTruthy();
  });
});
