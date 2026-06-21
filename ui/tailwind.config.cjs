/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{vue,ts,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        neon: {
          cyan: '#06b6d4',
          coral: '#f43f5e',
          purple: '#a855f7',
          emerald: '#10b981',
          amber: '#f59e0b',
        },
        glass: {
          base: 'rgba(255, 255, 255, 0.05)',
          hover: 'rgba(255, 255, 255, 0.1)',
          border: 'rgba(255, 255, 255, 0.1)',
          'border-hover': 'rgba(255, 255, 255, 0.2)',
        },
        dark: {
          bg: '#0f0c29',
          'bg-alt': '#1a103c',
          card: 'rgba(255, 255, 255, 0.03)',
        },
      },
      animation: {
        'fade-in-up': 'fadeInUp 0.6s ease-out forwards',
        'breathe': 'breathe 2s ease-in-out infinite',
        'glow': 'glow 2s ease-in-out infinite alternate',
      },
      keyframes: {
        fadeInUp: {
          '0%': { opacity: '0', transform: 'translateY(20px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        breathe: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.6' },
        },
        glow: {
          '0%': { boxShadow: '0 0 5px rgba(168, 85, 247, 0.3), 0 0 10px rgba(168, 85, 247, 0.1)' },
          '100%': { boxShadow: '0 0 10px rgba(168, 85, 247, 0.5), 0 0 20px rgba(168, 85, 247, 0.2)' },
        },
      },
      backdropBlur: {
        glass: '16px',
      },
    },
  },
  plugins: [],
}
