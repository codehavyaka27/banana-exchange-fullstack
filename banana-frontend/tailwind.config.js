/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        banana: {
          400: '#facc15',
          500: '#eab308',
        },
        terminal: {
          900: '#0f172a',
          800: '#1e293b',
        }
      }
    },
  },
  plugins: [],
}