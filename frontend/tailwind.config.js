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
        sm: '0 0.125rem 0.25rem rgba(0, 0, 0, 0.075)',
        md: 'rgba(145, 158, 171, 0.2) 0px 0px 2px 0px, rgba(145, 158, 171, 0.12) 0px 12px 24px -4px',
        'dark-md': 'rgba(0, 0, 0, 0.3) 0px 0px 2px 0px, rgba(0, 0, 0, 0.25) 0px 12px 24px -4px',
      },
      colors: {
        // TailwindAdmin Primary & Accents
        primary: {
          DEFAULT: '#5d87ff',
          hover: '#4570ea',
          emphasis: '#4570ea',
        },
        lightprimary: '#ecf2ff',
        secondary: {
          DEFAULT: '#49beff',
          hover: '#2ba8fb',
          emphasis: '#2ba8fb',
        },
        lightsecondary: '#e8f7ff',
        success: {
          DEFAULT: '#13deb9',
          hover: '#0eb597',
          emphasis: '#0eb597',
        },
        lightsuccess: '#e6fffa',
        warning: {
          DEFAULT: '#ffae1f',
          hover: '#e6980e',
          emphasis: '#e6980e',
        },
        lightwarning: '#fef5e5',
        error: {
          DEFAULT: '#fa896b',
          hover: '#e56f50',
          emphasis: '#e56f50',
        },
        lighterror: '#fdede8',
        danger: {
          DEFAULT: '#fa896b',
          hover: '#e56f50',
        },
        info: {
          DEFAULT: '#539bff',
          hover: '#3984ea',
        },
        lightinfo: '#ebf3fe',
        
        // Superficies y modo oscuro TailwindAdmin
        dark: '#202938',
        darkcard: '#2a3547',
        border: '#eaeff4',
        bordergray: '#dfe5ef',
        darkborder: '#333f55',
        link: '#2a3547',
        bodytext: '#5a6a85',
        darklink: '#7c8fac',
        lightgray: '#f6f9fc',
        darkgray: '#465670',

        // Compatibilidad semántica para shells
        background: {
          light: '#f6f9fc',
          dark: '#202938',
        },
        card: {
          light: '#ffffff',
          dark: '#2a3547',
          border: '#333f55',
        },
        sidebar: {
          DEFAULT: '#ffffff',
          text: '#5a6a85',
          active: '#5d87ff',
          dark: '#202938',
        },
      },
    },
  },
  plugins: [],
};
