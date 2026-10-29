/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        sans: ['"DM Sans"', 'sans-serif', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', 'Arial'],
      },
      borderRadius: {
        DEFAULT: '7px',
        sm: '4px',
        md: '7px',
        lg: '10px',
        xl: '14px',
      },
      boxShadow: {
        xs: 'var(--theme-shadow-sm, 0 1px 2px 0 rgba(16, 24, 40, 0.05))',
        sm: 'var(--theme-shadow-sm, 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075))',
        md: 'var(--theme-shadow-md, rgba(145, 158, 171, 0.2) 0px 0px 2px 0px, rgba(145, 158, 171, 0.12) 0px 12px 24px -4px)',
        'dark-md': 'rgba(0, 0, 0, 0.35) 0px 0px 2px 0px, rgba(0, 0, 0, 0.3) 0px 12px 24px -4px',
      },
      colors: {
        // Tokens Semánticos Dinámicos en Caliente con soporte de alpha (/10../90)
        canvas: 'rgb(var(--bg-canvas-rgb, 241 245 249) / <alpha-value>)',
        surface: 'rgb(var(--bg-surface-rgb, 255 255 255) / <alpha-value>)',
        topbar: 'var(--bg-topbar, rgba(255, 255, 255, 0.92))',

        // TailwindAdmin Primary & Accents (conectados a variables en caliente)
        primary: {
          DEFAULT: 'rgb(var(--color-primary-rgb, 93 135 255) / <alpha-value>)',
          hover: 'rgb(var(--color-primary-emphasis-rgb, 69 112 234) / <alpha-value>)',
          emphasis: 'rgb(var(--color-primary-emphasis-rgb, 69 112 234) / <alpha-value>)',
        },
        lightprimary: 'rgb(var(--color-lightprimary-rgb, 236 242 255) / <alpha-value>)',
        secondary: {
          DEFAULT: 'rgb(var(--color-secondary-rgb, 73 190 255) / <alpha-value>)',
          hover: 'rgb(var(--color-secondary-emphasis-rgb, 43 168 251) / <alpha-value>)',
          emphasis: 'rgb(var(--color-secondary-emphasis-rgb, 43 168 251) / <alpha-value>)',
        },
        lightsecondary: 'rgb(var(--color-lightsecondary-rgb, 232 247 255) / <alpha-value>)',
        success: {
          DEFAULT: '#13deb9',
          hover: '#0eb597',
          emphasis: '#0f766e',
        },
        lightsuccess: '#dcfce7',
        warning: {
          DEFAULT: '#ffae1f',
          hover: '#e6980e',
          emphasis: '#b45309',
        },
        lightwarning: '#fef3c7',
        error: {
          DEFAULT: '#fa896b',
          hover: '#e56f50',
          emphasis: '#be123c',
        },
        lighterror: '#ffe4e6',
        danger: {
          DEFAULT: '#fa896b',
          hover: '#e56f50',
          emphasis: '#be123c',
        },
        info: {
          DEFAULT: '#539bff',
          hover: '#3984ea',
          emphasis: '#0369a1',
        },
        lightinfo: '#e0f2fe',

        // Superficies y modo oscuro TailwindAdmin v3.0
        dark: '#141c2b',
        darkcard: '#1e293b',
        border: 'rgb(var(--border-default-rgb, 220 227 236) / <alpha-value>)',
        bordergray: 'rgb(var(--border-input-rgb, 203 213 225) / <alpha-value>)',
        darkborder: '#334155',
        link: 'rgb(var(--text-heading-rgb, 30 41 59) / <alpha-value>)',
        bodytext: 'rgb(var(--text-body-rgb, 71 85 105) / <alpha-value>)',
        darklink: '#94a3b8',
        lightgray: 'rgb(var(--bg-canvas-rgb, 241 245 249) / <alpha-value>)',
        darkgray: '#475569',

        // Compatibilidad semántica para shells
        background: {
          light: '#f1f5f9',
          dark: '#141c2b',
        },
        card: {
          light: '#ffffff',
          dark: '#1e293b',
          border: '#334155',
        },
        sidebar: {
          DEFAULT: 'rgb(var(--bg-sidebar-rgb, 255 255 255) / <alpha-value>)',
          text: 'var(--sidebar-text, #475569)',
          heading: 'var(--sidebar-heading, #1e293b)',
          border: 'var(--sidebar-border, #dce3ec)',
          active: 'rgb(var(--color-primary-rgb, 93 135 255) / <alpha-value>)',
          dark: '#1e293b',
        },
      },
    },
  },
  plugins: [],
};
