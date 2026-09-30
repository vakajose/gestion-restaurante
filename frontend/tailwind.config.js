/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        background: {
          light: '#f4f7fa',
          dark: '#1c2128',
        },
        card: {
          light: '#ffffff',
          dark: '#2a2e36',
          border: '#373d49',
        },
        sidebar: {
          DEFAULT: '#3f4d67',
          text: '#a9b7d0',
          active: '#1de9b6',
          dark: '#1b1e24',
        },
        primary: {
          DEFAULT: '#04a9f5',
          hover: '#0398dc',
        },
        success: {
          DEFAULT: '#1de9b6',
          hover: '#15c599',
        },
        danger: {
          DEFAULT: '#899fd4',
          hover: '#7287be',
        },
        auth: {
          purple: '#9084d7',
          turquoise: '#1de9b6',
        },
      },
    },
  },
  plugins: [],
};
