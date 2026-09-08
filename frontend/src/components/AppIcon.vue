<script setup>
defineProps({ name: { type: String, default: 'grid' } })
const paths = {
  grid: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
  search: 'M21 21l-5-5 M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
  arrow: 'M4 12h16 M14 6l6 6-6 6',
  book: 'M12 5v16 M12 5C9 2 5 3 2 4v15c3-1 7-2 10 1 3-3 7-2 10-1V4c-3-1-7-2-10 1',
  play: 'M8 4l13 8-13 8z',
  code: 'M8 7l-5 5 5 5 M16 7l5 5-5 5 M14 3l-4 18',
  chevron: 'M6 9l6 6 6-6',
  menu: 'M4 6h16 M4 12h16 M4 18h16',
  close: 'M6 6l12 12 M6 18L18 6',
  check: 'M5 12l4 4L19 6',
  copy: 'M9 9h12v12H9z M15 9V3H3v12h6',
  lock: 'M6 10h12v11H6z M8 10V6a4 4 0 0 1 8 0v4',
  bolt: 'M13 2L3 14h8l-1 8 11-13h-8z',
  logout: 'M9 4H3v16h6 M9 12h12 M17 8l4 4-4 4',
  eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12 M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  refresh: 'M20 7a9 9 0 1 0 1 8 M20 2v6h-6',
}
</script>

<template>
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" class="h-4 w-4 shrink-0">
    <path :d="paths[name] || paths.grid" />
  </svg>
</template>
