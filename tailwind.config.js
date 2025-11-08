/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/main/resources/templates/**/*.html",
    "./src/main/resources/static/**/*.{html,js}"
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Primary brand color
        primary: {
          DEFAULT: '#7543ea',
          50: '#f5f2ff',
          100: '#ede8ff',
          200: '#ddd5ff',
          300: '#c3b3ff',
          400: '#a688ff',
          500: '#8a5cff',
          600: '#7543ea',
          700: '#6530d0',
          800: '#5428a8',
          900: '#452589',
        },
        // Accent colors
        accent: {
          green: '#32ffa8',
        },
        // Background colors
        background: {
          light: '#f6f6f8',
          dark: '#18161b',
        },
      },
      fontFamily: {
        display: ['Syne', 'Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'Source Code Pro', 'ui-monospace', 'monospace'],
      },
      borderRadius: {
        DEFAULT: '0.125rem',
        lg: '0.25rem',
        xl: '0.5rem',
        '2xl': '0.75rem',
        full: '9999px',
      },
      boxShadow: {
        'card': '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)',
        'card-hover': '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
      },
      animation: {
        'spin-slow': 'spin 3s linear infinite',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms')({
      strategy: 'class',
    }),
  ],
}
