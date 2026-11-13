/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: [
          '"DM Sans"',
          'system-ui',
          '-apple-system',
          'BlinkMacSystemFont',
          '"Segoe UI"',
          'Roboto',
          'sans-serif',
        ],
      },
      colors: {
        canvas: '#f1f5f9',
        surface: '#ffffff',
        sidebar: {
          DEFAULT: '#141c2b',
          hover: '#1e293b',
          border: '#283548',
          text: '#cbd5e1',
          muted: '#94a3b8',
        },
        primary: {
          DEFAULT: '#5d87ff',
          hover: '#4570ea',
          light: '#ecf2ff',
        },
        secondary: {
          DEFAULT: '#49beff',
          light: '#e8f7ff',
        },
        success: {
          DEFAULT: '#0f766e',
          bright: '#13deb9',
          light: '#dcfce7',
        },
        warning: {
          DEFAULT: '#b45309',
          bright: '#ffae1f',
          light: '#fef3c7',
        },
        danger: {
          DEFAULT: '#be123c',
          bright: '#fa896b',
          light: '#ffe4e6',
        },
        info: {
          DEFAULT: '#0369a1',
          light: '#e0f2fe',
        },
        border: '#dce3ec',
      },
      boxShadow: {
        card: 'rgba(145, 158, 171, 0.18) 0px 0px 2px 0px, rgba(145, 158, 171, 0.10) 0px 10px 20px -4px',
      },
    },
  },
  plugins: [],
};
