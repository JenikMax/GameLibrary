import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { gamesApi } from '../api/games'

export const useLibraryStore = defineStore('library', () => {
  const games = ref([])
  const totalItems = ref(0)
  const totalPages = ref(1)
  const currentPage = ref(1)
  const loading = ref(false)
  const searchText = ref('')
  const selectedPlatforms = ref([])
  const selectedYears = ref([])
  const selectedGenres = ref([])
  const sortField = ref('')
  const sortType = ref('')
  const favoritesOnly = ref(false)
  const filterOptions = ref({ years: [], platforms: [], genres: [] })
  const pageSize = 12

  async function fetchGames(page = 1) {
    loading.value = true
    currentPage.value = page
    try {
      const response = await gamesApi.getGames({
        page,
        search: searchText.value || undefined,
        platforms: selectedPlatforms.value.length ? selectedPlatforms.value : undefined,
        years: selectedYears.value.length ? selectedYears.value : undefined,
        genres: selectedGenres.value.length ? selectedGenres.value : undefined,
        sortField: sortField.value || undefined,
        sortType: sortType.value || undefined,
        favoritesOnly: favoritesOnly.value || undefined
      })
      const data = response.data
      if (data.success) {
        games.value = data.data.items
        totalItems.value = data.data.totalItems
        totalPages.value = data.data.totalPages
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchFilterOptions() {
    try {
      const response = await gamesApi.getFilterOptions()
      const data = response.data
      if (data.success) {
        filterOptions.value = data.data
      }
    } catch {
      // ignore
    }
  }

  function setSearch(text) {
    searchText.value = text
  }

  function setFilters({ platforms, years, genres }) {
    selectedPlatforms.value = platforms || []
    selectedYears.value = years || []
    selectedGenres.value = genres || []
  }

  function setSort(field, type) {
    sortField.value = field
    sortType.value = type
  }

  function resetFilters() {
    searchText.value = ''
    selectedPlatforms.value = []
    selectedYears.value = []
    selectedGenres.value = []
    sortField.value = ''
    sortType.value = ''
    favoritesOnly.value = false
  }

  const genreMap = computed(() => {
    const map = {}
    for (const g of filterOptions.value.genres || []) {
      map[g.code] = g.name
    }
    return map
  })

  return {
    games, totalItems, totalPages, currentPage, loading,
    searchText, selectedPlatforms, selectedYears, selectedGenres,
    sortField, sortType, favoritesOnly, filterOptions, pageSize, genreMap,
    fetchGames, fetchFilterOptions, setSearch, setFilters, setSort, resetFilters
  }
})
