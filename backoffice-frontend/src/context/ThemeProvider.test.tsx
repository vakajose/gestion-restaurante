import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';
import { ThemeProvider } from './ThemeProvider';
import { useTheme } from './themeContext';

function ThemeProbe() {
  const { theme, toggleTheme } = useTheme();
  return (
    <button type="button" onClick={toggleTheme}>
      Tema actual: {theme}
    </button>
  );
}

describe('ThemeProvider (TASK-622 Dark Mode)', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
  });

  it('alterna entre claro y oscuro y persiste en localStorage + clase html.dark', async () => {
    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    const btn = screen.getByRole('button');
    expect(btn.textContent).toContain('light');
    expect(document.documentElement.classList.contains('dark')).toBe(false);

    const user = userEvent.setup();
    await user.click(btn);

    expect(btn.textContent).toContain('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem('bo_theme')).toBe('dark');

    await user.click(btn);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem('bo_theme')).toBe('light');
  });
});
