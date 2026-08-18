/** Quiet Luxury tokens, matched to the Android app. */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        ivory: '#FAF7F1',
        espresso: '#1C1917',
        'espresso-mid': '#4A4038',
        warm: '#6B6259',
        gold: '#B8863B',
        'gold-bright': '#D4A85C',
        'gold-container': '#F3E6CC',
        forest: '#2F4A3C',
        'forest-light': '#5C8770',
        brick: '#A23B2E',
        outline: '#E4DDD1',
      },
      fontFamily: {
        display: ['Fraunces', 'Georgia', 'serif'],
        sans: ['Manrope', 'system-ui', 'sans-serif'],
      },
      boxShadow: { warm: '0 4px 16px rgba(28,25,23,0.10)' },
    },
  },
  plugins: [],
}
