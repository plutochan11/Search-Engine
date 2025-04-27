<script setup>
import SearchEngine from './components/SearchEngine.vue';
</script>

<template>
  <SearchEngine />
</template>

<style>
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen,
    Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  line-height: 1.6;
  color: #333;
  background-color: #f9f9f9;
  margin: 0;
  padding: 0;
}

#app {
  width: 100%;
  min-height: 100vh;
}

* {
  box-sizing: border-box;
}
</style>
