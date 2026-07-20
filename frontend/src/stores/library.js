import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { gamesApi } from '../api/games'
import { useLocaleStore } from './locale'

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
  const selectedTags = ref([])
  const sortField = ref('')
  const sortType = ref('')
  const favoritesOnly = ref(false)
  const semanticSearch = ref(false)
  const filterOptions = ref({ years: [], platforms: [], genres: [], tags: [] })
  const pageSize = ref(12)
  const viewMode = ref('grid')

  function setViewMode(mode) {
    viewMode.value = mode
  }

  async function fetchGames(page = 1) {
    loading.value = true
    currentPage.value = page
    try {
      const response = await gamesApi.getGames({
        page,
        pageSize: pageSize.value,
        search: searchText.value || undefined,
        platforms: selectedPlatforms.value.length ? selectedPlatforms.value : undefined,
        years: selectedYears.value.length ? selectedYears.value : undefined,
        genres: selectedGenres.value.length ? selectedGenres.value : undefined,
        tags: selectedTags.value.length ? selectedTags.value : undefined,
        sortField: sortField.value || undefined,
        sortType: sortType.value || undefined,
        favoritesOnly: favoritesOnly.value || undefined,
        semantic: semanticSearch.value || undefined
      })
      const data = response.data
      if (data.success) {
        games.value = data.data.items
        totalItems.value = data.data.totalItems
        totalPages.value = data.data.totalPages
      }
    } catch {
      // error handled by global axios interceptor
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

  function setFilters({ platforms, years, genres, tags }) {
    selectedPlatforms.value = platforms || []
    selectedYears.value = years || []
    selectedGenres.value = genres || []
    selectedTags.value = tags || []
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
    selectedTags.value = []
    sortField.value = ''
    sortType.value = ''
    favoritesOnly.value = false
    semanticSearch.value = false
  }

  const genreMap = computed(() => {
    const map = {}
    for (const g of filterOptions.value.genres || []) {
      map[g.code] = g.name
    }
    return map
  })

  const localeStore = useLocaleStore()
  watch(() => localeStore.locale, () => {
    fetchFilterOptions()
  })

  return {
    games, totalItems, totalPages, currentPage, loading,
    searchText, selectedPlatforms, selectedYears, selectedGenres, selectedTags,
    sortField, sortType, favoritesOnly, semanticSearch, filterOptions, pageSize, viewMode, genreMap,
    fetchGames, fetchFilterOptions, setSearch, setFilters, setSort, resetFilters, setViewMode
  }
})
